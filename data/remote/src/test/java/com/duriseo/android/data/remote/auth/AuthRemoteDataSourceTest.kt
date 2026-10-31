package com.duriseo.android.data.remote.auth

import com.duriseo.android.data.remote.RecordingSessionEnd
import com.duriseo.android.data.remote.FakeTokenStorage
import com.duriseo.android.data.remote.authTokensJson
import com.duriseo.android.data.remote.authedClient
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.errorJson
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.noAuthClient
import com.duriseo.android.data.remote.retrofitFor
import com.duriseo.android.data.remote.testTokens
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** 인증 API (API.md 3.1, 4.1~4.3, 4.7): 경로·본문·헤더·응답 읽기·오류 변환 */
class AuthRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: AuthRemoteDataSource
    private lateinit var tokens: FakeTokenStorage
    private val caller = ApiCaller(createApiJson())

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokens = FakeTokenStorage(testTokens(access = "guest-access", refresh = "guest-refresh"))
        val authApi = retrofitFor(server, noAuthClient(server)).create(AuthApi::class.java)
        val sessionApi = retrofitFor(server, authedClient(server, tokens, RecordingSessionEnd()))
            .create(AuthSessionApi::class.java)
        remote = AuthRemoteDataSource(authApi, sessionApi, caller)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun bodyJson(request: RecordedRequest) = Json.parseToJsonElement(request.body.peek().readUtf8()).jsonObject

    private fun <T> DataResult<T>.value(): T = (this as DataResult.Success).value

    private fun <T> DataResult<T>.apiError(): DataError.ApiError =
        (this as DataResult.Failure).error as DataError.ApiError

    @Test
    fun `API POST auth guest는 본문 없이 보내고 AuthTokens 다섯 필드를 읽는다`() = runTest {
        server.enqueue(jsonResponse(201, authTokensJson("a1", "r1", isNewUser = true)))

        val issued = remote.guest().value()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/guest", request.path)
        assertEquals(0L, request.bodySize)
        assertEquals("a1", issued.tokens.accessToken)
        assertEquals(Instant.parse("2026-10-30T02:00:00Z"), issued.tokens.accessTokenExpiresAt)
        assertEquals("r1", issued.tokens.refreshToken)
        assertEquals(Instant.parse("2026-11-29T00:00:00Z"), issued.tokens.refreshTokenExpiresAt)
        assertTrue(issued.isNewUser)
    }

    @Test
    fun `API POST auth social은 provider와 token을 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, authTokensJson("a2", "r2", isNewUser = false)))

        val issued = remote.social(SocialProvider.GOOGLE, "social-token-1").value()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/social", request.path)
        assertEquals("GOOGLE", bodyJson(request)["provider"]?.jsonPrimitive?.content)
        assertEquals("social-token-1", bodyJson(request)["token"]?.jsonPrimitive?.content)
        assertEquals("a2", issued.tokens.accessToken)
        assertEquals(false, issued.isNewUser)
    }

    @Test
    fun `FR-AUTH-02 Google 로그인은 ID 토큰을 provider GOOGLE로 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, authTokensJson("a", "r")))

        remote.social(SocialProvider.GOOGLE, "google-id-token")

        val body = bodyJson(server.takeRequest())
        assertEquals("GOOGLE", body["provider"]?.jsonPrimitive?.content)
        assertEquals("google-id-token", body["token"]?.jsonPrimitive?.content)
    }

    @Test
    fun `FR-AUTH-02 카카오 로그인은 액세스 토큰을 provider KAKAO로 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, authTokensJson("a", "r")))

        remote.social(SocialProvider.KAKAO, "kakao-access-token")

        val body = bodyJson(server.takeRequest())
        assertEquals("KAKAO", body["provider"]?.jsonPrimitive?.content)
        assertEquals("kakao-access-token", body["token"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API auth social의 401 SOCIAL_TOKEN_INVALID는 refresh 하지 않는다`() = runTest {
        server.enqueue(jsonResponse(401, errorJson("SOCIAL_TOKEN_INVALID")))
        server.enqueue(jsonResponse(401, errorJson("SOCIAL_TOKEN_INVALID")))

        val viaSocial = remote.social(SocialProvider.KAKAO, "bad").apiError()
        // 인증 클라이언트(link)도 SOCIAL_TOKEN_INVALID로는 refresh 하지 않는다
        val viaLink = remote.link(SocialProvider.KAKAO, "bad").apiError()

        assertEquals(ErrorCode.SOCIAL_TOKEN_INVALID, viaSocial.knownCode)
        assertEquals(ErrorCode.SOCIAL_TOKEN_INVALID, viaLink.knownCode)
        assertEquals(listOf("/api/v1/auth/social", "/api/v1/auth/link"), listOf(server.takeRequest().path, server.takeRequest().path))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `API POST auth link는 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = remote.link(SocialProvider.GOOGLE, "google-id-token")

        val request = server.takeRequest()
        assertEquals(DataResult.Success(Unit), result)
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/link", request.path)
        assertEquals("Bearer guest-access", request.getHeader("Authorization"))
        assertEquals("GOOGLE", bodyJson(request)["provider"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API auth link의 409는 SOCIAL_ALREADY_USED ApiError다`() = runTest {
        server.enqueue(jsonResponse(409, errorJson("SOCIAL_ALREADY_USED")))

        val error = remote.link(SocialProvider.KAKAO, "kakao-token").apiError()

        assertEquals("SOCIAL_ALREADY_USED", error.code)
        assertEquals(ErrorCode.SOCIAL_ALREADY_USED, error.knownCode)
    }

    @Test
    fun `API POST auth switch는 게스트 access 토큰과 함께 보내고 AuthTokens를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, authTokensJson("member-access", "member-refresh", isNewUser = false)))

        val issued = remote.switch(SocialProvider.KAKAO, "kakao-token").value()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/switch", request.path)
        assertEquals("Bearer guest-access", request.getHeader("Authorization"))
        assertEquals("KAKAO", bodyJson(request)["provider"]?.jsonPrimitive?.content)
        assertEquals("kakao-token", bodyJson(request)["token"]?.jsonPrimitive?.content)
        assertEquals("member-access", issued.tokens.accessToken)
        assertEquals("member-refresh", issued.tokens.refreshToken)
        assertEquals(false, issued.isNewUser)
    }

    @Test
    fun `API auth switch의 404는 SOCIAL_NOT_REGISTERED ApiError다`() = runTest {
        server.enqueue(jsonResponse(404, errorJson("SOCIAL_NOT_REGISTERED")))

        val error = remote.switch(SocialProvider.GOOGLE, "t").apiError()

        assertEquals(ErrorCode.SOCIAL_NOT_REGISTERED, error.knownCode)
    }

    @Test
    fun `API POST auth logout은 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = remote.logout()

        val request = server.takeRequest()
        assertEquals(DataResult.Success(Unit), result)
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/logout", request.path)
        assertEquals(0L, request.bodySize)
        assertEquals("Bearer guest-access", request.getHeader("Authorization"))
    }

    @Test
    fun `API 1장 인증 경로는 Bearer access 토큰을 붙인다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        remote.link(SocialProvider.GOOGLE, "t")
        remote.logout()

        assertEquals("Bearer guest-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer guest-access", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `API 1장 auth guest와 social과 refresh에는 Authorization을 붙이지 않는다`() = runTest {
        // 저장된 토큰이 있어도 "인증 없음" 클라이언트는 헤더를 붙이지 않는다
        server.enqueue(jsonResponse(201, authTokensJson("a", "r", isNewUser = true)))
        server.enqueue(jsonResponse(200, authTokensJson("a", "r")))
        server.enqueue(jsonResponse(200, authTokensJson("a", "r")))
        val authApi = retrofitFor(server, noAuthClient(server)).create(AuthApi::class.java)

        remote.guest()
        remote.social(SocialProvider.GOOGLE, "t")
        authApi.refresh(RefreshRequest("old-refresh")).execute()

        val requests = List(3) { server.takeRequest() }
        assertEquals(
            listOf("/api/v1/auth/guest", "/api/v1/auth/social", "/api/v1/auth/refresh"),
            requests.map { it.path },
        )
        requests.forEach { assertNull(it.getHeader("Authorization")) }
        val refreshBody = bodyJson(requests[2])
        assertEquals("old-refresh", refreshBody["refreshToken"]?.jsonPrimitive?.content)
    }

    @Test
    fun `NFR-SEC-01 토큰이 든 DTO toString은 토큰 값을 찍지 않는다`() {
        val tokensDto = AuthTokensDto(
            accessToken = "secret-access",
            accessTokenExpiresAt = Instant.parse("2026-10-30T01:00:00Z"),
            refreshToken = "secret-refresh",
            refreshTokenExpiresAt = Instant.parse("2026-11-29T00:00:00Z"),
            isNewUser = true,
        )
        val texts = listOf(
            tokensDto.toString(),
            SocialTokenRequest(SocialProviderDto.GOOGLE, "secret-social").toString(),
            RefreshRequest("secret-refresh").toString(),
        )

        texts.forEach { text ->
            assertTrue(text, !text.contains("secret"))
        }
    }
}
