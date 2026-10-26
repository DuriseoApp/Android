package com.duriseo.android.data.remote.call

import com.duriseo.android.data.remote.SampleApi
import com.duriseo.android.data.remote.fastFailClient
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.sampleApi
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.error.FieldError
import com.duriseo.android.domain.error.FieldErrorReason
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** 오류 변환 (API.md 1.1·5장, ARCHITECTURE.md 3장) */
class ApiCallerErrorTest {
    private lateinit var server: MockWebServer
    private lateinit var api: SampleApi
    private val caller = ApiCaller(createApiJson())

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = sampleApi(server, fastFailClient())
    }

    @After
    fun tearDown() {
        // 일부 테스트가 서버를 먼저 내리므로 두 번째 shutdown의 예외는 무시한다
        runCatching { server.shutdown() }
    }

    private suspend fun failure(): DataError = (caller.call { api.getSample() } as DataResult.Failure).error

    private fun apiError(error: DataError): DataError.ApiError = error as DataError.ApiError

    @Test
    fun `API 1_1 오류 응답은 code와 details와 traceId를 가진 ApiError로 바뀐다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"nickname: max 10",
                    "details":{"fields":[{"field":"nickname","reason":"TOO_LONG"}]},"traceId":"8f2c1d"}""",
            ),
        )

        val error = apiError(failure())

        assertEquals("VALIDATION_FAILED", error.code)
        assertEquals(ErrorCode.VALIDATION_FAILED, error.knownCode)
        assertEquals("8f2c1d", error.traceId)
        assertEquals(ErrorDetails.Fields(listOf(FieldError("nickname", "TOO_LONG"))), error.details)
    }

    @Test
    fun `API 1_1 오류의 message는 ApiError에 담지 않는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"nickname: max 10","details":null,"traceId":"t"}""",
            ),
        )

        val error = apiError(failure())

        assertFalse(error.toString().contains("max 10"))
        assertFalse(error.toString().contains("nickname"))
    }

    @Test
    fun `API 1_1 모르는 code는 알려지지 않은 코드로 두고 원래 문자열을 남긴다`() = runTest {
        server.enqueue(jsonResponse(409, """{"code":"SOMETHING_NEW","message":"m","details":null,"traceId":"t"}"""))

        val error = apiError(failure())

        assertEquals("SOMETHING_NEW", error.code)
        assertNull(error.knownCode)
        assertNull(error.details)
    }

    @Test
    fun `API 1_1 VALIDATION_FAILED의 details fields를 필드와 이유로 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"m","traceId":"t","details":{"fields":[
                    {"field":"title","reason":"REQUIRED"},{"field":"end","reason":"INVALID"}]}}""",
            ),
        )

        val details = apiError(failure()).details as ErrorDetails.Fields

        assertEquals(listOf("title", "end"), details.fields.map { it.field })
        assertEquals(
            listOf(FieldErrorReason.REQUIRED, FieldErrorReason.INVALID),
            details.fields.map { it.reason },
        )
    }

    @Test
    fun `API 1_1 DATE_OUT_OF_RANGE의 details fields도 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"DATE_OUT_OF_RANGE","message":"m","traceId":"t",
                    "details":{"fields":[{"field":"start","reason":"OUT_OF_RANGE"}]}}""",
            ),
        )

        val details = apiError(failure()).details as ErrorDetails.Fields

        assertEquals(FieldErrorReason.OUT_OF_RANGE, details.fields.single().reason)
    }

    @Test
    fun `API 1_1 INVITE_RATE_LIMITED의 details retryAfterSeconds를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                429,
                """{"code":"INVITE_RATE_LIMITED","message":"m","details":{"retryAfterSeconds":18},"traceId":"t"}""",
            ),
        )
        server.enqueue(
            jsonResponse(
                429,
                """{"code":"INVITE_REISSUE_TOO_SOON","message":"m","details":{"retryAfterSeconds":42},"traceId":"t"}""",
            ),
        )

        assertEquals(ErrorDetails.RetryAfter(18), apiError(failure()).details)
        assertEquals(ErrorDetails.RetryAfter(42), apiError(failure()).details)
    }

    @Test
    fun `API 1_1 그 밖의 code는 details가 null이다`() = runTest {
        server.enqueue(
            jsonResponse(
                404,
                """{"code":"NOT_FOUND","message":"m","details":{"fields":[{"field":"x","reason":"INVALID"}]},"traceId":"t"}""",
            ),
        )

        val error = apiError(failure())

        assertEquals(ErrorCode.NOT_FOUND, error.knownCode)
        assertNull(error.details)
    }

    @Test
    fun `API 1_1 JSON이 아닌 오류 응답은 INTERNAL_ERROR로 본다`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(502)
                .setHeader("Content-Type", "text/html")
                .setHeader("X-Trace-Id", "from-header")
                .setBody("<html>Bad Gateway</html>"),
        )
        server.enqueue(MockResponse().setResponseCode(500))

        val html = apiError(failure())
        val empty = apiError(failure())

        assertEquals("INTERNAL_ERROR", html.code)
        assertEquals(ErrorCode.INTERNAL_ERROR, html.knownCode)
        assertNull(html.details)
        assertEquals("from-header", html.traceId)
        assertEquals("INTERNAL_ERROR", empty.code)
        assertNull(empty.traceId)
    }

    @Test
    fun `API 1_1 오류 객체 모양이 아닌 JSON도 INTERNAL_ERROR로 본다`() = runTest {
        server.enqueue(jsonResponse(500, """{"status":"down"}"""))

        assertEquals("INTERNAL_ERROR", apiError(failure()).code)
    }

    @Test
    fun `API 1_1 204 요청이 오류면 callUnit도 ApiError를 돌려준다`() = runTest {
        server.enqueue(jsonResponse(403, """{"code":"NOT_AUTHOR","message":"m","details":null,"traceId":"t"}"""))

        val result = caller.callUnit { api.delete() }

        assertEquals(ErrorCode.NOT_AUTHOR, apiError((result as DataResult.Failure).error).knownCode)
    }

    @Test
    fun `NFR-OFF-02 연결이 끊기면 Offline으로 바뀐다`() = runTest {
        // 서버를 먼저 내려서 연결 자체를 못 하게 한다
        server.shutdown()

        assertEquals(DataError.Offline, failure())
    }

    @Test
    fun `NFR-OFF-02 응답 도중 연결이 끊겨도 Offline으로 바뀐다`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))

        assertEquals(DataError.Offline, failure())
    }

    @Test
    fun `NFR-OFF-02 시간 초과는 Offline으로 바뀐다`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        assertEquals(DataError.Offline, failure())
    }
}
