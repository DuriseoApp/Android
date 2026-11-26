package com.duriseo.android.data.remote.network

import com.duriseo.android.core.common.config.OsVersion
import com.duriseo.android.core.common.update.AppVersion
import com.duriseo.android.data.remote.FakeTokenStorage
import com.duriseo.android.data.remote.RecordingSessionEnd
import com.duriseo.android.data.remote.authedClient
import com.duriseo.android.data.remote.noAuthClient
import com.duriseo.android.data.remote.testTokens
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** 공통 요청 헤더 (API.md 1장, D-182, FR-APP-03 AC1) */
class CommonHeadersInterceptorTest {
    private lateinit var server: MockWebServer
    private val headers = CommonHeadersInterceptor(AppVersion(name = "1.4.0", code = 140L), OsVersion(apiLevel = 35))

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun get(client: okhttp3.OkHttpClient) {
        client.newCall(Request.Builder().url(server.url("/api/v1/ping")).build()).execute().close()
    }

    @Test
    fun `FR-APP-03 AC1 인증 없는 요청과 인증 요청 모두 네 헤더를 붙인다`() {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        get(noAuthClient(server, commonHeaders = headers))
        get(authedClient(server, FakeTokenStorage(testTokens(access = "my-access")), RecordingSessionEnd(), commonHeaders = headers))

        val noAuth = server.takeRequest()
        val authed = server.takeRequest()
        listOf(noAuth, authed).forEach { request ->
            assertEquals("ANDROID", request.getHeader("X-Platform"))
            assertEquals("1.4.0", request.getHeader("X-App-Version"))
            assertEquals("140", request.getHeader("X-App-Build"))
            assertEquals("35", request.getHeader("X-OS-Version"))
        }
        assertNull(noAuth.getHeader("Authorization"))
        assertEquals("Bearer my-access", authed.getHeader("Authorization"))
    }
}
