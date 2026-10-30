package com.duriseo.android.data.repository.auth

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.authTokensJson
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.meJson
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.auth.AuthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 앱 시작 때 하는 일 (FR-AUTH-01 AC2·AC3, NFR-TIME-01) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionStartupTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private var created: RepoHarness? = null

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        created?.close()
        runCatching { server.shutdown() }
    }

    private fun startup(h: RepoHarness) =
        SessionStartup(h.authRepository, h.meRepository, h.clock, kotlinx.coroutines.CoroutineScope(Dispatchers.IO))

    @Test
    fun `FR-AUTH-01 AC3 저장된 토큰이 없으면 요청 없이 로그아웃 상태가 된다`() = runTestWithTimeout {
        val h = RepoHarness(server, tempFolder.root, initialTokens = null).also { created = it }

        startup(h).run()

        assertEquals(AuthState.SignedOut, h.authRepository.authState.first())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `FR-AUTH-01 AC2 저장된 토큰이 있으면 뒤에서 내 정보를 새로 받는다`() = runTestWithTimeout {
        val h = RepoHarness(server, tempFolder.root, initialTokens = testTokens()).also { created = it }
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER")))

        startup(h).run()

        assertEquals(AuthState.SignedIn, h.authRepository.authState.first())
        assertEquals("/api/v1/me", server.takeRequest().path)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `NFR-TIME-01 앱 시작 때 기기 시간대가 다르면 PATCH me를 보낸다`() = runTestWithTimeout {
        val h = RepoHarness(server, tempFolder.root, initialTokens = testTokens(), zoneId = "Asia/Tokyo")
            .also { created = it }
        server.enqueue(jsonResponse(200, meJson(timeZone = "Asia/Seoul")))
        server.enqueue(jsonResponse(200, meJson(timeZone = "Asia/Tokyo")))

        startup(h).run()

        assertEquals("GET", server.takeRequest().method)
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertEquals("{\"timeZone\":\"Asia/Tokyo\"}", patch.body.readUtf8())
        assertEquals("Asia/Tokyo", h.roomMe()?.timeZone)
    }

    @Test
    fun `NFR-TIME-01 로그인 직후 기기 시간대가 다르면 PATCH me를 보낸다`() = runTestWithTimeout {
        val h = RepoHarness(server, tempFolder.root, initialTokens = null, zoneId = "Asia/Tokyo")
            .also { created = it }
        server.enqueue(jsonResponse(201, authTokensJson("a", "r", isNewUser = true)))
        server.enqueue(jsonResponse(200, meJson(timeZone = "Asia/Seoul")))
        server.enqueue(jsonResponse(200, meJson(timeZone = "Asia/Tokyo")))

        h.authRepository.startAsGuest()

        assertEquals(3, server.requestCount)
        assertEquals("Asia/Tokyo", h.roomMe()?.timeZone)
    }

}
