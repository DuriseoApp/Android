package com.duriseo.android.data.remote

import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.data.remote.auth.AuthApi
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.network.AuthInterceptor
import com.duriseo.android.data.remote.network.TokenAuthenticator
import com.duriseo.android.data.remote.network.createAuthedClient
import com.duriseo.android.data.remote.network.createNoAuthClient
import com.duriseo.android.domain.auth.AuthTokens
import com.duriseo.android.domain.auth.SessionEndNotifier
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.auth.TokenState
import com.duriseo.android.domain.auth.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/** 메모리 토큰 저장소 (암호화는 data:local 테스트가 확인한다) */
class FakeTokenStorage(initial: AuthTokens? = null) : TokenStorage {
    private val mutableState = MutableStateFlow<TokenState>(
        if (initial != null) TokenState.Present(initial) else TokenState.Empty,
    )
    override val state: StateFlow<TokenState> = mutableState

    override suspend fun restore() = Unit

    override suspend fun save(tokens: AuthTokens) {
        mutableState.value = TokenState.Present(tokens)
    }

    override suspend fun clear() {
        mutableState.value = TokenState.Empty
    }
}

/** 세션 종료 알림을 기록한다 */
class RecordingSessionEnd : SessionEndNotifier {
    val reasons = CopyOnWriteArrayList<SessionEndReason>()

    override fun end(reason: SessionEndReason) {
        reasons.add(reason)
    }
}

fun testTokens(access: String = "old-access", refresh: String = "old-refresh") = AuthTokens(
    accessToken = access,
    accessTokenExpiresAt = Instant.parse("2026-10-30T01:00:00Z"),
    refreshToken = refresh,
    refreshTokenExpiresAt = Instant.parse("2026-11-29T00:00:00Z"),
)

/** API.md 3.1 `AuthTokens` 응답 본문 (다섯 필드) */
fun authTokensJson(access: String, refresh: String, isNewUser: Boolean = false): String =
    """{"accessToken":"$access","accessTokenExpiresAt":"2026-10-30T02:00:00Z",
        "refreshToken":"$refresh","refreshTokenExpiresAt":"2026-11-29T00:00:00Z","isNewUser":$isNewUser}"""

fun errorJson(code: String): String =
    """{"code":"$code","message":"개발자용 설명","details":null,"traceId":"trace-1"}"""

/** API.md 3.1 `Me` 응답 본문. 개인 공간 */
fun meJson(accountType: String = "GUEST", timeZone: String = "Asia/Seoul"): String =
    """{"id":"11111111-1111-1111-1111-111111111111","accountType":"$accountType","nickname":"게스트",
        "timeZone":"$timeZone","providers":[],"hasProfileImage":false,"createdAt":"2026-10-12T03:00:00Z",
        "space":{"id":"22222222-2222-2222-2222-222222222222","type":"PERSONAL","myColor":"pick-orange",
        "partner":null,"togetherColor":null,"startDate":null,"importPending":false}}"""

fun retrofitFor(server: MockWebServer, client: OkHttpClient): Retrofit =
    Retrofit.Builder()
        .baseUrl(server.url("/api/v1/"))
        .addConverterFactory(createApiJson().asConverterFactory("application/json".toMediaType()))
        .client(client)
        .build()

private fun configFor(server: MockWebServer) = AppConfig(server.url("/").toString(), "", "")

private val silentLogging = okhttp3.Interceptor { chain -> chain.proceed(chain.request()) }

/** 운영과 같은 "인증 없음" 클라이언트 (연결 실패를 다시 시도하지 않게 줄임) */
fun noAuthClient(server: MockWebServer): OkHttpClient =
    createNoAuthClient(configFor(server), silentLogging).newBuilder()
        .retryOnConnectionFailure(false)
        .build()

/** 운영과 같은 "인증" 클라이언트: AuthInterceptor + TokenAuthenticator (refresh는 같은 서버의 NoAuth 클라이언트) */
fun authedClient(
    server: MockWebServer,
    tokens: TokenStorage,
    sessionEnd: SessionEndNotifier,
): OkHttpClient {
    val authApi = retrofitFor(server, noAuthClient(server)).create(AuthApi::class.java)
    val authenticator = TokenAuthenticator(tokens, authApi, sessionEnd, createApiJson())
    return createAuthedClient(configFor(server), silentLogging, AuthInterceptor(tokens), authenticator)
        .newBuilder()
        .retryOnConnectionFailure(false)
        .build()
}

