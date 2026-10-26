package com.duriseo.android.data.remote.network

import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.data.remote.SampleApi
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** 서버 주소 설정 (unit-1.md 결정 1): 비어 있어도 앱이 죽지 않고 그 요청만 실패한다 */
class NetworkConfigTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `API 1장 설정의 서버 주소로 api v1 기본 주소 HttpUrl을 만든다`() {
        val config = AppConfig("https://api.example.test", "", "")

        assertEquals("https://api.example.test/api/v1/", resolveApiBaseUrl(config).toString())
    }

    @Test
    fun `서버 주소가 비었거나 주소 모양이 아니면 기본 주소를 만들지 않는다`() {
        assertNull(resolveApiBaseUrl(AppConfig("", "", "")))
        assertNull(resolveApiBaseUrl(AppConfig("not a url", "", "")))
        assertNotNull(resolveApiBaseUrl(AppConfig("https://api.example.test", "", "")))
    }

    @Test
    fun `서버 주소가 비어 있어도 자리 주소로 Retrofit은 만들어진다`() {
        val retrofit = Retrofit.Builder()
            .baseUrl(UNCONFIGURED_BASE_URL)
            .addConverterFactory(createApiJson().asConverterFactory("application/json".toMediaType()))
            .build()

        assertEquals("http://unconfigured.invalid/api/v1/", retrofit.baseUrl().toString())
    }

    @Test
    fun `서버 주소가 비어 있으면 요청을 보내지 않고 공통 오류로 실패한다`() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor(ServerConfigInterceptor(AppConfig("", "", "")))
            .build()
        // 기본 주소가 실제 서버를 가리켜도 설정이 비었으면 요청이 나가지 않아야 한다
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(createApiJson().asConverterFactory("application/json".toMediaType()))
            .client(client)
            .build()
            .create(SampleApi::class.java)
        server.enqueue(jsonResponse(200, "{}"))

        val result = ApiCaller(createApiJson()).call { api.getSample() }

        val error = (result as DataResult.Failure).error as DataError.ApiError
        assertEquals(ErrorCode.INTERNAL_ERROR, error.knownCode)
        assertEquals(0, server.requestCount)
    }
}
