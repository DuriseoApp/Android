package com.duriseo.android.data.remote.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `NFR-SEC-01 로그에 Authorization 헤더 값을 남기지 않는다`() {
        send(enabled = true)

        assertTrue(lines.any { it.startsWith("Authorization:") })
        assertFalse(lines.any { it.contains(secret) })
        assertFalse(lines.any { it.contains("Bearer") })
    }

    @Test
    fun `NFR-SEC-01 본문은 남기되 토큰 필드 값은 가린다`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("""{"data":{"accessToken":"$secret-a","refreshToken":"$secret-r","nickname":"나"}}"""),
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor(enabled = true) { lines.add(it) })
            .build()
        val body = """{"provider":"KAKAO","token":"$secret-k"}""".toRequestBody("application/json".toMediaType())
        // setUp에서 넣은 204를 먼저 쓴 뒤 200을 받는다
        client.newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute().close()
        client.newCall(Request.Builder().url(server.url("/api/v1/auth/social")).post(body).build()).execute().close()

        assertFalse(lines.any { it.contains(secret) })
        assertTrue(lines.any { it.contains("\"token\":\"***\"") })
        assertTrue(lines.any { it.contains("\"accessToken\":\"***\"") && it.contains("\"refreshToken\":\"***\"") })
        assertTrue(lines.any { it.contains("\"nickname\":\"나\"") })
    }

    @Test
    fun `NFR-SEC-01 토큰 필드만 가리고 다른 필드는 그대로 둔다`() {
        assertEquals(
            """{"fcmToken":"***","timezone":"Asia/Seoul","code":"INVITE_CODE_INVALID"}""",
            redactTokens("""{"fcmToken":"abc\"def","timezone":"Asia/Seoul","code":"INVITE_CODE_INVALID"}"""),
        )
    }

    @Test
    fun `토큰 로그 스위치를 켜면 Authorization과 토큰 필드를 가리지 않는다`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("""{"accessToken":"$secret-a"}"""),
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor(enabled = true, showTokens = true) { lines.add(it) })
            .build()
        client.newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute().close()
        client.newCall(
            Request.Builder().url(server.url("/api/v1/auth/refresh")).header("Authorization", "Bearer $secret").build(),
        ).execute().close()

        assertTrue(lines.any { it == "Authorization: Bearer $secret" })
        assertTrue(lines.any { it.contains("\"accessToken\":\"$secret-a\"") })
    }

    @Test
    fun `NFR-SEC-01 로깅이 꺼져 있으면 토큰 로그 스위치가 켜져 있어도 아무것도 남기지 않는다`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor(enabled = false, showTokens = true) { lines.add(it) })
            .build()
        client.newCall(
            Request.Builder().url(server.url("/api/v1/me")).header("Authorization", "Bearer $secret").build(),
        ).execute().close()

        assertEquals(emptyList<String>(), lines)
    }

    @Test
    fun `NFR-SEC-01 로깅을 끄면 아무것도 남기지 않는다`() {
        send(enabled = false)

        assertEquals(emptyList<String>(), lines)
    }
}
