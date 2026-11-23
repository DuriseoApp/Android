package com.duriseo.android.data.remote.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpLoggingTest {
    private lateinit var server: MockWebServer
    private val lines = mutableListOf<String>()
    private val secret = "secret-access-token-value"

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        server.enqueue(MockResponse().setResponseCode(204))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun send(enabled: Boolean) {
        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor(enabled) { lines.add(it) })
            .build()
        val request = Request.Builder()
            .url(server.url("/api/v1/me"))
            .header("Authorization", "Bearer $secret")
            .build()
        client.newCall(request).execute().close()
    }

    @Test
    fun `debug 로그는 Authorization 헤더와 본문을 가리지 않고 남긴다`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("""{"accessToken":"$secret-a","refreshToken":"$secret-r"}"""),
        )
        send(enabled = true)
        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor(enabled = true) { lines.add(it) })
            .build()
        client.newCall(Request.Builder().url(server.url("/api/v1/auth/refresh")).build()).execute().close()

        assertTrue(lines.any { it == "Authorization: Bearer $secret" })
        assertTrue(lines.any { it.contains("\"accessToken\":\"$secret-a\"") })
    }

    @Test
    fun `NFR-SEC-01 release처럼 로깅을 끄면 아무것도 남기지 않는다`() {
        send(enabled = false)

        assertEquals(emptyList<String>(), lines)
    }
}
