package com.duriseo.android.data.remote.anniversary

import com.duriseo.android.data.remote.FakeTokenStorage
import com.duriseo.android.data.remote.RecordingSessionEnd
import com.duriseo.android.data.remote.authedClient
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.errorJson
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.retrofitFor
import com.duriseo.android.data.remote.testTokens
import com.duriseo.android.domain.anniversary.AnniversaryKind
import com.duriseo.android.domain.anniversary.AnniversaryPatch
import com.duriseo.android.domain.anniversary.NewAnniversary
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.error.FieldError
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** 기념일 API 6개 경로 (API.md 3.3 "기념일") */
class AnniversaryRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: AnniversaryRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val client = authedClient(server, FakeTokenStorage(testTokens(access = "my-access")), RecordingSessionEnd())
        remote = AnniversaryRemoteDataSource(
            retrofitFor(server, client).create(AnniversaryApi::class.java),
            ApiCaller(createApiJson()),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun <T> DataResult<T>.value(): T = (this as DataResult.Success).value

    private fun <T> DataResult<T>.apiError(): DataError.ApiError = (this as DataResult.Failure).error as DataError.ApiError

    private fun bodyOf(request: RecordedRequest): JsonObject = Json.parseToJsonElement(request.body.readUtf8()).jsonObject

    private val autoJson = """{"anniversaryId":null,"kind":"AUTO","name":"200일","date":"2026-10-23","daysLeft":11,
        "repeatsYearly":false,"authorId":null}"""

    private val customJson = """{"anniversaryId":"a1","kind":"CUSTOM","name":"처음 여행 간 날","date":"2026-12-05","daysLeft":54,
        "repeatsYearly":true,"authorId":"u1"}"""

    private val anniversaryJson = """{"id":"a1","name":"처음 여행 간 날","date":"2025-12-05","repeatsYearly":true,"authorId":"u1",
        "createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-02T00:00:00Z"}"""

    @Test
    fun `API GET anniversaries는 from to를 보내고 응답 순서 그대로 items를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[$autoJson,$customJson]}"""))

        val items = remote.list(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 11, 1)).value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/anniversaries?from=2026-09-30&to=2026-11-01", request.path)
        assertEquals("Bearer my-access", request.getHeader("Authorization"))
        assertEquals(listOf("200일", "처음 여행 간 날"), items.map { it.name })
        assertEquals(AnniversaryKind.AUTO, items[0].kind)
        assertNull(items[0].anniversaryId)
        assertNull(items[0].authorId)
        assertEquals(AnniversaryKind.CUSTOM, items[1].kind)
        assertEquals("a1", items[1].anniversaryId)
        assertEquals(LocalDate.of(2026, 12, 5), items[1].date)
        assertEquals(54, items[1].daysLeft)
        assertTrue(items[1].repeatsYearly)
    }

    @Test
    fun `API GET anniversaries는 모르는 kind를 건너뛴다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[${autoJson.replace("AUTO", "SOMETHING_NEW")},$customJson]}"""))

        assertEquals(listOf("처음 여행 간 날"), remote.list(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).value().map { it.name })
    }

    @Test
    fun `API GET anniversaries의 400은 필드 이유를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"m","details":{"fields":[{"field":"to","reason":"INVALID"}]},"traceId":"t"}""",
            ),
        )

        val error = remote.list(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).apiError()

        assertEquals(ErrorCode.VALIDATION_FAILED, error.knownCode)
        assertEquals(ErrorDetails.Fields(listOf(FieldError("to", "INVALID"))), error.details)
    }

    @Test
    fun `API GET anniversaries upcoming은 cursor를 그대로 보내고 nextCursor를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[$autoJson],"nextCursor":"cursor-1"}"""))
        server.enqueue(jsonResponse(200, """{"items":[$customJson],"nextCursor":null}"""))

        val first = remote.upcoming().value()
        val second = remote.upcoming(cursor = first.nextCursor).value()

        assertEquals("/api/v1/anniversaries/upcoming", server.takeRequest().path)
        assertEquals("/api/v1/anniversaries/upcoming?cursor=cursor-1", server.takeRequest().path)
        assertEquals("cursor-1", first.nextCursor)
        assertNull(second.nextCursor)
        assertNull(first.totalCount)
    }

    @Test
    fun `API GET anniversaries past는 totalCount와 항목을 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[{"anniversaryId":"p1","name":"제주 여행 출발","date":"2026-08-01","authorId":"u2"}],
                    "nextCursor":"c1","totalCount":2}""",
            ),
        )

        val page = remote.past(size = 1).value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/anniversaries/past?size=1", request.path)
        assertEquals(2, page.totalCount)
        assertEquals("c1", page.nextCursor)
        assertEquals("p1", page.items.single().anniversaryId)
        assertEquals(LocalDate.of(2026, 8, 1), page.items.single().date)
        assertEquals("u2", page.items.single().authorId)
    }

    @Test
    fun `API POST anniversaries는 name date repeatsYearly를 보내고 Anniversary를 읽는다`() = runTest {
        server.enqueue(jsonResponse(201, anniversaryJson))

        val made = remote.create(NewAnniversary("처음 여행 간 날", LocalDate.of(2025, 12, 5), repeatsYearly = true)).value()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/anniversaries", request.path)
        val body = bodyOf(request)
        assertEquals("처음 여행 간 날", body["name"]?.jsonPrimitive?.content)
        assertEquals("2025-12-05", body["date"]?.jsonPrimitive?.content)
        assertTrue(body["repeatsYearly"]!!.jsonPrimitive.boolean)
        assertEquals("a1", made.id)
        assertEquals("u1", made.authorId)
    }

    @Test
    fun `API POST anniversaries는 정하지 않은 repeatsYearly를 보내지 않는다`() = runTest {
        server.enqueue(jsonResponse(201, anniversaryJson))

        remote.create(NewAnniversary("이름", LocalDate.of(2025, 12, 5))).value()

        assertFalse(bodyOf(server.takeRequest()).containsKey("repeatsYearly"))
    }

    @Test
    fun `API POST anniversaries의 DATE_OUT_OF_RANGE를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"DATE_OUT_OF_RANGE","message":"m","details":{"fields":[{"field":"date","reason":"OUT_OF_RANGE"}]},"traceId":"t"}""",
            ),
        )

        val error = remote.create(NewAnniversary("이름", LocalDate.of(1949, 1, 1))).apiError()

        assertEquals(ErrorCode.DATE_OUT_OF_RANGE, error.knownCode)
    }

    @Test
    fun `API PATCH anniversaries는 바뀐 필드만 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, anniversaryJson))

        remote.update("a1", AnniversaryPatch(name = "새 이름")).value()

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/anniversaries/a1", request.path)
        assertEquals(setOf("name"), bodyOf(request).keys)
    }

    @Test
    fun `API PATCH anniversaries는 날짜와 반복을 바꿀 때 그 필드를 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, anniversaryJson))

        remote.update("a1", AnniversaryPatch(date = LocalDate.of(2026, 1, 2), repeatsYearly = false)).value()

        val body = bodyOf(server.takeRequest())
        assertEquals(setOf("date", "repeatsYearly"), body.keys)
        assertEquals("2026-01-02", body["date"]?.jsonPrimitive?.content)
        assertFalse(body["repeatsYearly"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `API PATCH anniversaries의 403은 NOT_AUTHOR고 404는 NOT_FOUND다`() = runTest {
        server.enqueue(jsonResponse(403, errorJson("NOT_AUTHOR")))
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        assertEquals(ErrorCode.NOT_AUTHOR, remote.update("a1", AnniversaryPatch(name = "x")).apiError().knownCode)
        assertEquals(ErrorCode.NOT_FOUND, remote.update("a1", AnniversaryPatch(name = "x")).apiError().knownCode)
    }

    @Test
    fun `API DELETE anniversaries는 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        remote.delete("a1").value()

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/anniversaries/a1", request.path)
    }

    @Test
    fun `API GET anniversaries의 날짜 형식이 틀리면 공통 오류로 본다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[${customJson.replace("2026-12-05", "어제")}]}"""))

        assertEquals(ErrorCode.INTERNAL_ERROR, remote.list(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).apiError().knownCode)
    }
}
