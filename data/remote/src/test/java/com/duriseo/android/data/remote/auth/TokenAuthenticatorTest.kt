package com.duriseo.android.data.remote.auth

import com.duriseo.android.data.remote.FakeTokenStorage
import com.duriseo.android.data.remote.RecordingSessionEnd
import com.duriseo.android.data.remote.authTokensJson
import com.duriseo.android.data.remote.authedClient
import com.duriseo.android.data.remote.errorJson
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.me.MeApi
import com.duriseo.android.data.remote.retrofitFor
import com.duriseo.android.data.remote.testTokens
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** 401 처리 (API.md 1장, ARCHITECTURE.md 4장): TOKEN_EXPIRED만 refresh를 한 번, SESSION_*는 세션 종료 */
class TokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: FakeTokenStorage
    private lateinit var sessionEnd: RecordingSessionEnd

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokens = FakeTokenStorage(testTokens(access = "old-access", refresh = "old-refresh"))
        sessionEnd = RecordingSessionEnd()
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun meRequest() = Request.Builder().url(server.url("/api/v1/me")).build()

    private fun paths(count: Int): List<String?> = List(count) { server.takeRequest().path }

    /** 요청의 Authorization과 경로로 응답을 정하는 가짜 서버 */
    private inner class Script(
        val refreshCalls: AtomicInteger = AtomicInteger(),
        val refreshDelayMillis: Long = 0,
        val onRefresh: () -> MockResponse = { jsonResponse(200, authTokensJson("new-access", "new-refresh")) },
        val onMe: (RecordedRequest) -> MockResponse,
    ) : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            if (request.path == "/api/v1/auth/refresh") {
                refreshCalls.incrementAndGet()
                if (refreshDelayMillis > 0) Thread.sleep(refreshDelayMillis)
                onRefresh()
            } else {
                onMe(request)
            }
    }

    private fun expiredUnlessNew(request: RecordedRequest): MockResponse =
        if (request.getHeader("Authorization") == "Bearer new-access") {
            MockResponse().setResponseCode(204)
        } else {
            jsonResponse(401, errorJson("TOKEN_EXPIRED"))
        }

    @Test
    fun `API 1장 401 TOKEN_EXPIRED면 refresh 후 원래 요청을 다시 보낸다`() {
        val script = Script(onMe = ::expiredUnlessNew)
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().use { response ->
            assertEquals(204, response.code)
        }

        assertEquals(1, script.refreshCalls.get())
        // 원래 요청(옛 access) -> refresh -> 다시 보냄(새 access)
        assertEquals(listOf("/api/v1/me", "/api/v1/auth/refresh", "/api/v1/me"), paths(3))
        // 새 토큰이 저장돼 있다
        assertEquals("new-access", tokens.current()?.accessToken)
        assertEquals("new-refresh", tokens.current()?.refreshToken)
        assertTrue(sessionEnd.reasons.isEmpty())
    }

    @Test
    fun `API 1장 refresh 요청은 저장된 refresh 토큰을 본문에 담고 access 토큰은 붙이지 않는다`() {
        val script = Script(onMe = ::expiredUnlessNew)
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().close()

        server.takeRequest() // 원래 요청
        val refresh = server.takeRequest()
        assertEquals("POST", refresh.method)
        assertNull(refresh.getHeader("Authorization"))
        assertTrue(refresh.body.readUtf8().contains("\"refreshToken\":\"old-refresh\""))
    }

    @Test
    fun `API 1장 401 SESSION_INVALID면 refresh 하지 않고 세션을 끝낸다`() {
        val script = Script(onMe = { jsonResponse(401, errorJson("SESSION_INVALID")) })
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().use { response ->
            assertEquals(401, response.code)
        }

        assertEquals(0, script.refreshCalls.get())
        assertEquals(listOf(SessionEndReason.INVALID), sessionEnd.reasons.toList())
    }

    @Test
    fun `FR-AUTH-02 AC3 요청이 401 SESSION_REPLACED면 세션을 끝내고 REPLACED를 알린다`() {
        val script = Script(onMe = { jsonResponse(401, errorJson("SESSION_REPLACED")) })
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().use { response ->
            assertEquals(401, response.code)
        }

        assertEquals(0, script.refreshCalls.get())
        assertEquals(listOf(SessionEndReason.REPLACED), sessionEnd.reasons.toList())
    }

    @Test
    fun `FR-AUTH-02 AC3 refresh가 401 SESSION_REPLACED여도 REPLACED를 알린다`() {
        val script = Script(
            onRefresh = { jsonResponse(401, errorJson("SESSION_REPLACED")) },
            onMe = { jsonResponse(401, errorJson("TOKEN_EXPIRED")) },
        )
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().close()

        assertEquals(1, script.refreshCalls.get())
        assertEquals(listOf(SessionEndReason.REPLACED), sessionEnd.reasons.toList())
    }

    @Test
    fun `API 1장 refresh가 401 SESSION_INVALID면 INVALID로 세션을 끝낸다`() {
        val script = Script(
            onRefresh = { jsonResponse(401, errorJson("SESSION_INVALID")) },
            onMe = { jsonResponse(401, errorJson("TOKEN_EXPIRED")) },
        )
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().close()

        assertEquals(listOf(SessionEndReason.INVALID), sessionEnd.reasons.toList())
    }

    @Test
    fun `API 1장 refresh가 5xx면 세션을 끝내지 않고 원래 401을 돌려준다`() {
        val script = Script(
            onRefresh = { jsonResponse(500, errorJson("INTERNAL_ERROR")) },
            onMe = { jsonResponse(401, errorJson("TOKEN_EXPIRED")) },
        )
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().use { response ->
            assertEquals(401, response.code)
        }

        assertTrue(sessionEnd.reasons.isEmpty())
        assertEquals("old-access", tokens.current()?.accessToken)
    }

    @Test
    fun `API 1장 동시에 만료된 요청 세 개는 refresh를 한 번만 부른다`() {
        val script = Script(refreshDelayMillis = 300, onMe = ::expiredUnlessNew)
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)
        val pool = Executors.newFixedThreadPool(3)
        val start = CountDownLatch(1)
        val codes = java.util.concurrent.CopyOnWriteArrayList<Int>()
        val done = CountDownLatch(3)

        repeat(3) {
            pool.execute {
                try {
                    start.await()
                    client.newCall(meRequest()).execute().use { codes.add(it.code) }
                } finally {
                    done.countDown()
                }
            }
        }
        start.countDown()

        assertTrue("요청이 끝나지 않았어요", done.await(20, TimeUnit.SECONDS))
        pool.shutdown()
        assertEquals(1, script.refreshCalls.get())
        assertEquals(listOf(204, 204, 204), codes.toList())
        assertEquals("new-access", tokens.current()?.accessToken)
        // 처음 3번 + refresh 1번 + 다시 보낸 3번 = 7번. 늦게 도착한 요청은 이미 새 access를 붙여 401 없이 끝나므로 5~7번
        assertTrue(server.requestCount in 5..7)
    }

    @Test
    fun `API 1장 refresh 뒤에도 401이면 다시 갱신하지 않는다`() {
        val script = Script(onMe = { jsonResponse(401, errorJson("TOKEN_EXPIRED")) })
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().use { response ->
            assertEquals(401, response.code)
        }

        assertEquals(1, script.refreshCalls.get())
        // 원래 요청, refresh, 다시 보낸 요청. 그다음은 없다
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `API 1장 refresh가 네트워크 오류면 토큰을 지우지 않고 Offline이다`() = runTest {
        val script = Script(
            onRefresh = { MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST) },
            onMe = { jsonResponse(401, errorJson("TOKEN_EXPIRED")) },
        )
        server.dispatcher = script
        val api = retrofitFor(server, authedClient(server, tokens, sessionEnd)).create(MeApi::class.java)

        // 원래 호출은 IOException으로 끝나고 ApiCaller가 Offline으로 바꾼다
        val result = ApiCaller(createApiJson()).call { api.getMe() }

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals("old-access", tokens.current()?.accessToken)
        assertEquals("old-refresh", tokens.current()?.refreshToken)
        assertTrue(sessionEnd.reasons.isEmpty())
    }

    @Test
    fun `토큰이 없으면 401에 세션 종료를 알리지 않는다`() {
        tokens = FakeTokenStorage(initial = null)
        val script = Script(onMe = { jsonResponse(401, errorJson("SESSION_INVALID")) })
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().use { response ->
            assertEquals(401, response.code)
        }

        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
        assertEquals(0, script.refreshCalls.get())
        assertTrue(sessionEnd.reasons.isEmpty())
    }

    @Test
    fun `이미 새 로그인으로 바뀐 뒤 도착한 옛 요청의 SESSION_INVALID는 새 세션을 끝내지 않는다`() {
        val script = Script(
            onMe = {
                // 응답이 오기 전에 다른 로그인이 토큰을 바꿨다
                kotlinx.coroutines.runBlocking {
                    tokens.save(testTokens(access = "other-access", refresh = "other-refresh"))
                }
                jsonResponse(401, errorJson("SESSION_INVALID"))
            },
        )
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().close()

        assertTrue(sessionEnd.reasons.isEmpty())
        assertEquals("other-access", tokens.current()?.accessToken)
    }

    @Test
    fun `API 1장 오류 코드가 TOKEN_EXPIRED가 아닌 401은 건드리지 않는다`() {
        val script = Script(onMe = { jsonResponse(401, errorJson("SOCIAL_TOKEN_INVALID")) })
        server.dispatcher = script
        val client = authedClient(server, tokens, sessionEnd)

        client.newCall(meRequest()).execute().use { response ->
            assertEquals(401, response.code)
        }

        assertEquals(0, script.refreshCalls.get())
        assertEquals(1, server.requestCount)
        assertTrue(sessionEnd.reasons.isEmpty())
    }
}
