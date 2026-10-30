package com.duriseo.android.data.local.token

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.duriseo.android.core.common.Logger
import com.duriseo.android.domain.auth.AuthTokens
import com.duriseo.android.domain.auth.TokenState
import com.duriseo.android.domain.auth.TokenStorage
import com.google.crypto.tink.Aead
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Base64
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tink [Aead](AES256_GCM)로 암호화해 DataStore 파일(`auth_tokens`)에 저장하는 토큰 저장소 (NFR-SEC-01).
 * 평문 토큰은 디스크에 남지 않는다. 저장 형식은 `Base64(Aead(JSON(토큰)))`, associated data는 [ASSOCIATED_DATA]다.
 *
 * - 읽기는 메모리 값 [state]다. 앱 시작 때 [restore]로 한 번 읽는다 (OkHttp 스레드가 매 요청 복호화하지 않게).
 * - 복호화할 수 없으면(Keystore 키 없음, 재설치·기기 변경·백업 복원, 키셋 손상) 저장소를 비우고 로그아웃 상태가 된다 (FR-AUTH-01 AC3).
 * - 저장(쓰기)이 실패하면(일부 기기의 Keystore 오류) 이번 실행에서는 메모리 값으로 계속 쓰고 다음 실행에는 시작 화면이 된다.
 *   평문으로 대신 저장하지 않는다 (unit-2.md 위험 4).
 *
 * @param aeadProvider 처음 쓸 때 한 번 부른다. Keystore를 건드리므로 앱 시작을 막지 않으려고 늦게 만든다.
 */
class EncryptedTokenStorage(
    private val dataStore: DataStore<Preferences>,
    aeadProvider: () -> Aead,
) : TokenStorage {

    private val aead: Aead by lazy(aeadProvider)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<TokenState>(TokenState.Loading)

    override val state: StateFlow<TokenState> = mutableState.asStateFlow()

    override suspend fun restore() {
        mutex.withLock {
            if (mutableState.value == TokenState.Loading) {
                mutableState.value = readStored()
            }
        }
    }

    override suspend fun save(tokens: AuthTokens) {
        mutex.withLock {
            // 메모리를 먼저 바꾼다. Authenticator가 저장이 끝나기를 기다렸다가 새 access로 다시 보낸다
            mutableState.value = TokenState.Present(tokens)
            try {
                val encoded = encrypt(tokens)
                dataStore.edit { it[KEY_TOKENS] = encoded }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 토큰·예외 메시지는 남기지 않는다 (NFR-SEC-01)
                Logger.w(TAG, "토큰을 저장하지 못했어요: ${e.javaClass.simpleName}")
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            mutableState.value = TokenState.Empty
            clearStored()
        }
    }

    private suspend fun readStored(): TokenState = try {
        val encoded = dataStore.data.first()[KEY_TOKENS]
        if (encoded == null) {
            TokenState.Empty
        } else {
            TokenState.Present(decrypt(encoded))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // GeneralSecurityException(키 없음·인증 실패), 깨진 파일·JSON 모두 같다: 비우고 로그아웃 상태
        Logger.w(TAG, "저장된 토큰을 읽을 수 없어 비워요: ${e.javaClass.simpleName}")
        clearStored()
        TokenState.Empty
    }

    private suspend fun clearStored() {
        try {
            dataStore.edit { it.remove(KEY_TOKENS) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "저장된 토큰을 지우지 못했어요: ${e.javaClass.simpleName}")
        }
    }

    private fun encrypt(tokens: AuthTokens): String {
        val plain = json.encodeToString(StoredTokens.serializer(), StoredTokens.from(tokens))
        val cipher = aead.encrypt(plain.toByteArray(Charsets.UTF_8), ASSOCIATED_DATA.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(cipher)
    }

    private fun decrypt(encoded: String): AuthTokens {
        val cipher = Base64.getDecoder().decode(encoded)
        val plain = aead.decrypt(cipher, ASSOCIATED_DATA.toByteArray(Charsets.UTF_8))
        return json.decodeFromString(StoredTokens.serializer(), plain.toString(Charsets.UTF_8)).toTokens()
    }

    /** 저장용 JSON 모양. 일시는 ISO 8601 문자열. `toString`은 토큰 값을 찍지 않는다. */
    @Serializable
    private data class StoredTokens(
        val accessToken: String,
        val accessTokenExpiresAt: String,
        val refreshToken: String,
        val refreshTokenExpiresAt: String,
    ) {
        fun toTokens() = AuthTokens(
            accessToken = accessToken,
            accessTokenExpiresAt = Instant.parse(accessTokenExpiresAt),
            refreshToken = refreshToken,
            refreshTokenExpiresAt = Instant.parse(refreshTokenExpiresAt),
        )

        override fun toString(): String = "StoredTokens(***)"

        companion object {
            fun from(tokens: AuthTokens) = StoredTokens(
                accessToken = tokens.accessToken,
                accessTokenExpiresAt = tokens.accessTokenExpiresAt.toString(),
                refreshToken = tokens.refreshToken,
                refreshTokenExpiresAt = tokens.refreshTokenExpiresAt.toString(),
            )
        }
    }

    internal companion object {
        /** 암호문에 묶는 값. 다른 용도의 암호문을 여기에 바꿔 끼우지 못하게 한다 */
        const val ASSOCIATED_DATA = "duriseo.auth.tokens"
        const val TAG = "token"
        val KEY_TOKENS = stringPreferencesKey("tokens")
        private val json = Json { ignoreUnknownKeys = true }
    }
}
