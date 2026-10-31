package com.duriseo.android.data.local.token

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import com.duriseo.android.domain.auth.AuthTokens
import com.duriseo.android.domain.auth.TokenState
import com.google.crypto.tink.Aead
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.GeneralSecurityException
import java.time.Instant

/** 암호화 토큰 저장소 (NFR-SEC-01, FR-AUTH-01). Keystore는 JVM에서 못 쓰므로 가짜 [Aead]를 쓴다. */
class EncryptedTokenStorageTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val tokens = AuthTokens(
        accessToken = "access-token-plain-value",
        accessTokenExpiresAt = Instant.parse("2026-10-30T01:00:00Z"),
        refreshToken = "refresh-token-plain-value-0123456789abcdef",
        refreshTokenExpiresAt = Instant.parse("2026-11-29T00:00:00Z"),
    )

    /** 키 바이트로 섞고 associated data를 확인하는 가짜 AEAD. 키가 다르거나 associated data가 다르면 복호화에 실패한다. */
    private class FakeAead(private val key: Int) : Aead {
        override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
            tag(associatedData) + plaintext.map { (it.toInt() xor key).toByte() }

        override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray {
            val tag = tag(associatedData)
            if (ciphertext.size < tag.size || !ciphertext.copyOfRange(0, tag.size).contentEquals(tag)) {
                throw GeneralSecurityException("복호화할 수 없어요")
            }
            return ciphertext.copyOfRange(tag.size, ciphertext.size).map { (it.toInt() xor key).toByte() }.toByteArray()
        }

        private fun tag(associatedData: ByteArray): ByteArray =
            byteArrayOf(key.toByte()) + associatedData.fold(0) { acc, b -> acc * 31 + b }.toString().toByteArray()
    }

    private fun TestScope.dataStore(): Pair<DataStore<Preferences>, File> {
        val file = File(tempFolder.root, "$TOKEN_DATASTORE_NAME.preferences_pb")
        return createTokenDataStore(scope = backgroundScope) { file } to file
    }

    private fun storage(dataStore: DataStore<Preferences>, key: Int = 0x5A) =
        EncryptedTokenStorage(dataStore, aeadProvider = { FakeAead(key) })

    @Test
    fun `저장소를 읽기 전에는 로그인 여부를 모르는 상태다`() = runTest {
        val (dataStore, _) = dataStore()
        val storage = storage(dataStore)

        assertEquals(TokenState.Loading, storage.state.value)
        assertNull(storage.current())
    }

    @Test
    fun `FR-AUTH-01 AC3 저장된 토큰이 없으면 시작 화면을 보여 준다`() = runTest {
        val (dataStore, _) = dataStore()
        val storage = storage(dataStore)

        storage.restore()

        assertEquals(TokenState.Empty, storage.state.value)
        assertNull(storage.current())
    }

    @Test
    fun `저장한 토큰을 다시 읽으면 같은 값이다`() = runTest {
        val (dataStore, _) = dataStore()
        storage(dataStore).apply {
            restore()
            save(tokens)
        }

        // 앱을 다시 시작한 것처럼 같은 파일에서 새 저장소가 읽는다
        val restarted = storage(dataStore)
        restarted.restore()

        assertEquals(TokenState.Present(tokens), restarted.state.value)
        assertEquals(tokens, restarted.current())
    }

    @Test
    fun `저장 직후 current는 새 토큰을 돌려준다`() = runTest {
        val (dataStore, _) = dataStore()
        val storage = storage(dataStore)
        storage.restore()
        val renewed = tokens.copy(accessToken = "renewed-access", refreshToken = "renewed-refresh")

        storage.save(tokens)
        storage.save(renewed)

        assertEquals(renewed, storage.current())
    }

    @Test
    fun `clear 뒤에는 토큰이 없다`() = runTest {
        val (dataStore, _) = dataStore()
        val storage = storage(dataStore)
        storage.restore()
        storage.save(tokens)

        storage.clear()

        assertEquals(TokenState.Empty, storage.state.value)
        assertNull(storage.current())
        assertNull(dataStore.data.first()[EncryptedTokenStorage.KEY_TOKENS])
        // 다시 시작해도 없다
        val restarted = storage(dataStore)
        restarted.restore()
        assertEquals(TokenState.Empty, restarted.state.value)
    }

    @Test
    fun `NFR-SEC-01 저장소에는 토큰 평문이 남지 않는다`() = runTest {
        val (dataStore, file) = dataStore()
        val storage = storage(dataStore)
        storage.restore()

        storage.save(tokens)

        val stored = dataStore.data.first()[EncryptedTokenStorage.KEY_TOKENS]
        assertTrue(stored != null && stored.isNotEmpty())
        val fileText = file.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(fileText.contains(tokens.accessToken))
        assertFalse(fileText.contains(tokens.refreshToken))
        assertFalse(stored!!.contains(tokens.accessToken))
    }

    @Test
    fun `FR-AUTH-01 AC3 토큰을 복호화할 수 없으면 저장소를 비우고 시작 화면을 보여 준다`() = runTest {
        val (dataStore, _) = dataStore()
        storage(dataStore, key = 0x11).apply {
            restore()
            save(tokens)
        }

        // Keystore 키가 사라진 기기(재설치·기기 변경·백업 복원)처럼 다른 키로 읽는다
        val restored = storage(dataStore, key = 0x22)
        restored.restore()

        assertEquals(TokenState.Empty, restored.state.value)
        assertNull(restored.current())
        assertNull(dataStore.data.first()[EncryptedTokenStorage.KEY_TOKENS])
    }

    @Test
    fun `FR-AUTH-01 AC3 Aead를 만들 수 없어도 저장소를 비우고 시작 화면을 보여 준다`() = runTest {
        val (dataStore, _) = dataStore()
        storage(dataStore).apply {
            restore()
            save(tokens)
        }

        val broken = EncryptedTokenStorage(dataStore, aeadProvider = { throw GeneralSecurityException("Keystore 키가 없어요") })
        broken.restore()

        assertEquals(TokenState.Empty, broken.state.value)
        assertNull(dataStore.data.first()[EncryptedTokenStorage.KEY_TOKENS])
    }

    @Test
    fun `restore는 한 번만 읽는다`() = runTest {
        val (dataStore, _) = dataStore()
        val storage = storage(dataStore)
        storage.restore()
        storage.save(tokens)

        storage.restore()

        assertEquals(tokens, storage.current())
    }

    @Test
    fun `FR-AUTH-01 AC3 복호화할 수 없으면 깨진 키셋을 버리고 이전 사용자 데이터도 지운다`() = runTest {
        val (dataStore, _) = dataStore()
        storage(dataStore, key = 0x11).apply {
            restore()
            save(tokens)
        }
        var keyResets = 0
        var userDataClears = 0

        val restored = EncryptedTokenStorage(
            dataStore,
            aeadProvider = { FakeAead(0x22) },
            resetKeys = { keyResets++ },
            clearUserData = { userDataClears++ },
        )
        restored.restore()

        assertEquals(TokenState.Empty, restored.state.value)
        assertEquals(1, keyResets)
        assertEquals(1, userDataClears)
    }

    @Test
    fun `FR-AUTH-01 AC3 저장된 토큰이 없으면 키셋도 사용자 데이터도 건드리지 않는다`() = runTest {
        val (dataStore, _) = dataStore()
        var keyResets = 0
        var userDataClears = 0

        val empty = EncryptedTokenStorage(
            dataStore,
            aeadProvider = { FakeAead(0x5A) },
            resetKeys = { keyResets++ },
            clearUserData = { userDataClears++ },
        )
        empty.restore()

        assertEquals(TokenState.Empty, empty.state.value)
        assertEquals(0, keyResets)
        assertEquals(0, userDataClears)
    }

    @Test
    fun `FR-AUTH-01 AC3 이전 사용자 데이터를 지우지 못해도 로그아웃 상태가 된다`() = runTest {
        val (dataStore, _) = dataStore()
        storage(dataStore, key = 0x11).apply {
            restore()
            save(tokens)
        }

        val restored = EncryptedTokenStorage(
            dataStore,
            aeadProvider = { FakeAead(0x22) },
            clearUserData = { throw IllegalStateException("지우기 실패") },
        )
        restored.restore()

        // 시작 때 `SessionStartup`이 한 번 더 지운다
        assertEquals(TokenState.Empty, restored.state.value)
    }

    @Test
    fun `NFR-SEC-01 키셋을 만들지 못해 저장이 실패하면 키셋을 다시 만들어 저장한다`() = runTest {
        val (dataStore, _) = dataStore()
        var providerCalls = 0
        var keyResets = 0
        val storage = EncryptedTokenStorage(
            dataStore,
            aeadProvider = {
                if (providerCalls++ == 0) throw GeneralSecurityException("키셋이 깨졌어요")
                FakeAead(0x5A)
            },
            resetKeys = { keyResets++ },
        )
        storage.restore()

        storage.save(tokens)

        assertEquals(1, keyResets)
        assertEquals(2, providerCalls)
        // 다시 시작해도 읽힌다: 평문이 아니라 새 키셋으로 암호화해 저장했다
        val restarted = storage(dataStore)
        restarted.restore()
        assertEquals(tokens, restarted.current())
    }

    @Test
    fun `NFR-SEC-01 키셋을 다시 만들어도 실패하면 메모리 값으로 계속 쓰고 평문은 남기지 않는다`() = runTest {
        val (dataStore, file) = dataStore()
        val storage = EncryptedTokenStorage(dataStore, aeadProvider = { throw GeneralSecurityException("Keystore 오류") })
        storage.restore()

        storage.save(tokens)

        assertEquals(tokens, storage.current())
        assertNull(dataStore.data.first()[EncryptedTokenStorage.KEY_TOKENS])
        assertFalse(file.exists() && file.readBytes().toString(Charsets.ISO_8859_1).contains(tokens.accessToken))
    }
}
