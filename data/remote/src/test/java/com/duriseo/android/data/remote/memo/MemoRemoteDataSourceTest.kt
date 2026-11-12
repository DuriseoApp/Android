package com.duriseo.android.data.remote.memo

import com.duriseo.android.data.remote.FakeTokenStorage
import com.duriseo.android.data.remote.RecordingSessionEnd
import com.duriseo.android.data.remote.authedClient
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.errorJson
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.retrofitFor
import com.duriseo.android.data.remote.testTokens
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.error.FieldError
import com.duriseo.android.domain.memo.FieldChange
import com.duriseo.android.domain.memo.MemoPatch
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.domain.memo.NewMemo
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
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
import java.time.LocalDate

/** 메모 API 11개 경로 (API.md 3.4 "메모") */
class MemoRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: MemoRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val client = authedClient(server, FakeTokenStorage(testTokens(access = "my-access")), RecordingSessionEnd())
        remote = MemoRemoteDataSource(
            retrofitFor(server, client).create(MemoApi::class.java),
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

    /** 날짜·제목이 있는 내 메모, 고정됨 */
    private val fullJson = """{"id":"m1","title":"장보기","body":"우유\n계란","memoDate":"2026-10-17","authorId":"u1",
        "isPinned":true,"pinnedAt":"2026-10-18T01:00:00Z","isFavorite":true,
        "createdAt":"2026-10-08T00:12:00Z","updatedAt":"2026-10-09T00:00:00Z"}"""

    /** 값이 없는 필드는 null로 온다 (1.1) */
    private val emptyJson = """{"id":"m2","title":null,"body":"본문만","memoDate":null,"authorId":"u2",
        "isPinned":false,"pinnedAt":null,"isFavorite":false,
        "createdAt":"2026-10-08T00:12:00Z","updatedAt":"2026-10-08T00:12:00Z"}"""

    @Test
    fun `API GET memos는 view cursor size를 보내고 totalCount favoriteCount nextCursor를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[$fullJson,$emptyJson],"nextCursor":"c1","totalCount":42,"favoriteCount":3}"""))

        val page = remote.list(MemoView.OLDEST, cursor = "abc", size = 20).value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/memos?view=oldest&cursor=abc&size=20", request.path)
        assertEquals("Bearer my-access", request.getHeader("Authorization"))
        assertEquals(listOf("m1", "m2"), page.items.map { it.id })
        assertEquals("c1", page.nextCursor)
        assertEquals(42, page.totalCount)
        assertEquals(3, page.favoriteCount)
        val m = page.items[0]
        assertEquals("장보기", m.title)
        assertEquals("우유\n계란", m.body)
        assertEquals(LocalDate.of(2026, 10, 17), m.memoDate)
        assertTrue(m.isPinned)
        assertEquals(Instant.parse("2026-10-18T01:00:00Z"), m.pinnedAt)
        assertTrue(m.isFavorite)
        assertEquals(Instant.parse("2026-10-08T00:12:00Z"), m.createdAt)
        val empty = page.items[1]
        assertNull(empty.title)
        assertNull(empty.memoDate)
        assertNull(empty.pinnedAt)
    }

    @Test
    fun `API GET memos는 view의 세 값과 마지막 페이지의 null nextCursor를 다룬다`() = runTest {
        repeat(3) { server.enqueue(jsonResponse(200, """{"items":[],"nextCursor":null,"totalCount":0,"favoriteCount":0}""")) }

        val last = MemoView.entries.map { remote.list(it).value() }.last()

        assertEquals("/api/v1/memos?view=newest", server.takeRequest().path)
        assertEquals("/api/v1/memos?view=oldest", server.takeRequest().path)
        assertEquals("/api/v1/memos?view=favorites", server.takeRequest().path)
        assertNull(last.nextCursor)
    }

    @Test
    fun `API GET memos의 cursor 오류는 필드 이유를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"m","details":{"fields":[{"field":"cursor","reason":"INVALID"}]},"traceId":"t"}""",
            ),
        )

        val error = remote.list(MemoView.NEWEST, cursor = "깨짐").apiError()

        assertEquals(ErrorCode.VALIDATION_FAILED, error.knownCode)
        assertEquals(ErrorDetails.Fields(listOf(FieldError("cursor", "INVALID"))), error.details)
    }

    @Test
    fun `API GET memos pinned는 응답 순서 그대로 items를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[$emptyJson,$fullJson]}"""))

        val items = remote.pinned().value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/memos/pinned", request.path)
        assertEquals(listOf("m2", "m1"), items.map { it.id })
    }

    @Test
    fun `API GET memos dated는 from to를 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[$fullJson]}"""))

        val items = remote.dated(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 11, 1)).value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/memos/dated?from=2026-09-30&to=2026-11-01", request.path)
        assertEquals(LocalDate.of(2026, 10, 17), items.single().memoDate)
    }

    @Test
    fun `API POST memos는 정리한 title body memoDate notifyPartner를 보내고 Memo를 읽는다`() = runTest {
        server.enqueue(jsonResponse(201, fullJson))

        val made = remote.create(NewMemo("  장보기  ", "우유\n계란", LocalDate.of(2026, 10, 17), notifyPartner = true)).value()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/memos", request.path)
        val body = bodyOf(request)
        assertEquals("장보기", body["title"]?.jsonPrimitive?.content)
        assertEquals("우유\n계란", body["body"]?.jsonPrimitive?.content)
        assertEquals("2026-10-17", body["memoDate"]?.jsonPrimitive?.content)
        assertTrue(body["notifyPartner"]!!.jsonPrimitive.boolean)
        assertEquals("m1", made.id)
    }

    @Test
    fun `API POST memos는 비어 있는 값과 정하지 않은 notifyPartner를 보내지 않는다`() = runTest {
        server.enqueue(jsonResponse(201, emptyJson))

        // 제목은 공백뿐이라 없는 것, 본문 공백은 보존
        remote.create(NewMemo("   ", "  본문  ", null)).value()

        val body = bodyOf(server.takeRequest())
        assertEquals(setOf("body"), body.keys)
        assertEquals("  본문  ", body["body"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API POST memos의 DATE_OUT_OF_RANGE와 둘 다 빈 VALIDATION_FAILED를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"DATE_OUT_OF_RANGE","message":"m","details":{"fields":[{"field":"memoDate","reason":"OUT_OF_RANGE"}]},"traceId":"t"}""",
            ),
        )
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"m","details":{"fields":[{"field":"title","reason":"REQUIRED"},{"field":"body","reason":"REQUIRED"}]},"traceId":"t"}""",
            ),
        )

        val range = remote.create(NewMemo("제목", null, LocalDate.of(1949, 1, 1))).apiError()
        val required = remote.create(NewMemo(null, null, null)).apiError()

        assertEquals(ErrorCode.DATE_OUT_OF_RANGE, range.knownCode)
        assertEquals(
            ErrorDetails.Fields(listOf(FieldError("title", "REQUIRED"), FieldError("body", "REQUIRED"))),
            required.details,
        )
    }

    @Test
    fun `API GET memos id는 Memo를 읽고 404를 NOT_FOUND로 바꾼다`() = runTest {
        server.enqueue(jsonResponse(200, fullJson))
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        val memo = remote.get("m1").value()
        val error = remote.get("gone").apiError()

        assertEquals("/api/v1/memos/m1", server.takeRequest().path)
        assertEquals("/api/v1/memos/gone", server.takeRequest().path)
        assertTrue(memo.isFavorite)
        assertEquals(ErrorCode.NOT_FOUND, error.knownCode)
    }

    @Test
    fun `API PATCH memos는 바뀐 필드만 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, fullJson))

        remote.update("m1", MemoPatch(title = FieldChange.Set("새 제목"))).value()

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/memos/m1", request.path)
        val body = bodyOf(request)
        assertEquals(setOf("title"), body.keys)
        assertEquals("새 제목", body["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API PATCH memos는 지우는 필드를 null로 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, emptyJson))

        remote.update(
            "m1",
            MemoPatch(title = FieldChange.Set(null), memoDate = FieldChange.Set(null), body = FieldChange.Set("본문만")),
        ).value()

        val body = bodyOf(server.takeRequest())
        assertEquals(setOf("title", "body", "memoDate"), body.keys)
        assertEquals(JsonNull, body["title"])
        assertEquals(JsonNull, body["memoDate"])
        assertEquals("본문만", body["body"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API PATCH memos는 날짜를 YYYY-MM-DD로 보내고 403 NOT_AUTHOR를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, fullJson))
        server.enqueue(jsonResponse(403, errorJson("NOT_AUTHOR")))

        remote.update("m1", MemoPatch(memoDate = FieldChange.Set(LocalDate.of(2027, 1, 5)))).value()
        val error = remote.update("m1", MemoPatch(title = FieldChange.Set("x"))).apiError()

        assertEquals("2027-01-05", bodyOf(server.takeRequest())["memoDate"]?.jsonPrimitive?.content)
        assertEquals(ErrorCode.NOT_AUTHOR, error.knownCode)
    }

    @Test
    fun `API DELETE memos id는 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = remote.delete("m1")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/memos/m1", request.path)
        assertTrue(result is DataResult.Success)
    }

    @Test
    fun `API PUT DELETE memos id pin은 본문 없이 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        assertTrue(remote.setPinned("m1", true) is DataResult.Success)
        assertTrue(remote.setPinned("m1", false) is DataResult.Success)

        val put = server.takeRequest()
        val delete = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/api/v1/memos/m1/pin", put.path)
        assertEquals(0L, put.bodySize)
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/memos/m1/pin", delete.path)
    }

    @Test
    fun `API PUT DELETE memos id favorite는 본문 없이 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        assertTrue(remote.setFavorite("m1", true) is DataResult.Success)
        assertTrue(remote.setFavorite("m1", false) is DataResult.Success)

        val put = server.takeRequest()
        val delete = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/api/v1/memos/m1/favorite", put.path)
        assertEquals(0L, put.bodySize)
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/memos/m1/favorite", delete.path)
    }

    @Test
    fun `API 메모 응답에 필수 필드가 없으면 INTERNAL_ERROR다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[],"nextCursor":null}"""))

        val error = remote.list(MemoView.NEWEST).apiError()

        assertEquals("INTERNAL_ERROR", error.code)
    }
}
