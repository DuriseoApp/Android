package com.duriseo.android.data.remote.event

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
import com.duriseo.android.domain.event.EventPatch
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.event.NewEvent
import com.duriseo.android.domain.event.NoteChange
import com.duriseo.android.domain.event.ReminderOption
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** 일정 API (API.md 3.3) */
class EventRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: EventRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val client = authedClient(server, FakeTokenStorage(testTokens(access = "my-access")), RecordingSessionEnd())
        remote = EventRemoteDataSource(retrofitFor(server, client).create(EventApi::class.java), ApiCaller(createApiJson()))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun <T> DataResult<T>.value(): T = (this as DataResult.Success).value

    private fun <T> DataResult<T>.apiError(): DataError.ApiError = (this as DataResult.Failure).error as DataError.ApiError

    private fun bodyOf(request: okhttp3.mockwebserver.RecordedRequest): JsonObject =
        Json.parseToJsonElement(request.body.readUtf8()).jsonObject

    private val timedJson = """{"id":"e1","title":"저녁 데이트","note":"식당 예약","isAllDay":false,
        "start":"2026-10-12T10:30:00Z","end":"2026-10-12T12:30:00Z","isTogether":true,"authorId":"u1","updatedBy":null,
        "reminder":{"option":"HOUR_1","fireAt":"2026-10-12T09:30:00Z"},
        "createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-02T00:00:00Z"}"""

    private val allDayJson = """{"id":"e2","title":"1박 2일 여행","note":null,"isAllDay":true,
        "start":"2026-10-17","end":"2026-10-18","isTogether":false,"authorId":"u2","updatedBy":"u1","reminder":null,
        "createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-02T00:00:00Z"}"""

    @Test
    fun `API GET events는 from to를 보내고 items를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[$timedJson,$allDayJson]}"""))

        val events = remote.list(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/events?from=2026-10-01&to=2026-10-31", request.path)
        assertEquals("Bearer my-access", request.getHeader("Authorization"))
        assertEquals(2, events.size)
        val timed = events[0]
        assertEquals(EventTime.Timed(Instant.parse("2026-10-12T10:30:00Z"), Instant.parse("2026-10-12T12:30:00Z")), timed.time)
        assertEquals(ReminderOption.HOUR_1, timed.reminder?.option)
        assertEquals(Instant.parse("2026-10-12T09:30:00Z"), timed.reminder?.fireAt)
        assertTrue(timed.isTogether)
        assertEquals(EventTime.AllDay(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18)), events[1].time)
        assertNull(events[1].reminder)
        assertEquals("u1", events[1].updatedBy)
    }

    @Test
    fun `API GET events의 400은 필드 이유를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"m","details":{"fields":[{"field":"to","reason":"INVALID"}]},"traceId":"t"}""",
            ),
        )

        val error = remote.list(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 10, 31)).apiError()

        assertEquals(ErrorCode.VALIDATION_FAILED, error.knownCode)
        assertEquals(ErrorDetails.Fields(listOf(FieldError("to", "INVALID"))), error.details)
    }

    @Test
    fun `API POST events는 title isAllDay start end를 보내고 Event를 읽는다`() = runTest {
        server.enqueue(jsonResponse(201, allDayJson))

        val event = remote.create(
            NewEvent("1박 2일 여행", note = null, time = EventTime.AllDay(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18))),
        ).value()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/events", request.path)
        val body = bodyOf(request)
        assertEquals("1박 2일 여행", body["title"]?.jsonPrimitive?.content)
        assertTrue(body["isAllDay"]!!.jsonPrimitive.boolean)
        assertEquals("2026-10-17", body["start"]?.jsonPrimitive?.content)
        assertEquals("2026-10-18", body["end"]?.jsonPrimitive?.content)
        // 정하지 않은 선택 필드는 보내지 않는다 (서버 기본값: 나, 알림 켬)
        assertFalse(body.containsKey("note"))
        assertFalse(body.containsKey("isTogether"))
        assertFalse(body.containsKey("notifyPartner"))
        assertEquals("e2", event.id)
        assertNull(event.reminder)
    }

    @Test
    fun `NFR-TIME-01 시간 일정의 시작과 종료는 초 단위 UTC Z 문자열로 보낸다`() = runTest {
        server.enqueue(jsonResponse(201, timedJson))

        remote.create(
            NewEvent(
                title = "저녁 데이트",
                note = "식당 예약",
                time = EventTime.Timed(Instant.parse("2026-10-12T10:30:00.987654Z"), Instant.parse("2026-10-12T12:30:45.5Z")),
                isTogether = true,
                notifyPartner = false,
            ),
        ).value()

        val body = bodyOf(server.takeRequest())
        assertEquals("2026-10-12T10:30:00Z", body["start"]?.jsonPrimitive?.content)
        assertEquals("2026-10-12T12:30:45Z", body["end"]?.jsonPrimitive?.content)
        assertFalse(body["isAllDay"]!!.jsonPrimitive.boolean)
        assertTrue(body["isTogether"]!!.jsonPrimitive.boolean)
        assertFalse(body["notifyPartner"]!!.jsonPrimitive.boolean)
        assertEquals("식당 예약", body["note"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API POST events의 DATE_OUT_OF_RANGE는 start end 필드를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"DATE_OUT_OF_RANGE","message":"m","details":{"fields":[
                  {"field":"start","reason":"OUT_OF_RANGE"},{"field":"end","reason":"OUT_OF_RANGE"}]},"traceId":"t"}""",
            ),
        )

        val error = remote.create(
            NewEvent("x", null, EventTime.AllDay(LocalDate.of(1949, 12, 31), LocalDate.of(2051, 1, 1))),
        ).apiError()

        assertEquals(ErrorCode.DATE_OUT_OF_RANGE, error.knownCode)
        assertEquals(
            ErrorDetails.Fields(listOf(FieldError("start", "OUT_OF_RANGE"), FieldError("end", "OUT_OF_RANGE"))),
            error.details,
        )
    }

    @Test
    fun `API POST events 400 VALIDATION_FAILED는 필드 이름을 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"m","details":{"fields":[{"field":"title","reason":"TOO_LONG"}]},"traceId":"t"}""",
            ),
        )

        val error = remote.create(NewEvent("x", null, EventTime.AllDay(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1)))).apiError()

        assertEquals(ErrorDetails.Fields(listOf(FieldError("title", "TOO_LONG"))), error.details)
    }

    @Test
    fun `API GET events id는 Event를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, timedJson))

        val event = remote.get("e1").value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/events/e1", request.path)
        assertEquals("저녁 데이트", event.title)
        assertEquals("u1", event.authorId)
    }

    @Test
    fun `API GET events id의 404는 NOT_FOUND다`() = runTest {
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        assertEquals(ErrorCode.NOT_FOUND, remote.get("none").apiError().knownCode)
    }

    @Test
    fun `API PATCH events는 바뀐 필드만 보내고 Event를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, timedJson))

        remote.update("e1", EventPatch(title = "새 제목", isTogether = true)).value()

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/events/e1", request.path)
        val body = bodyOf(request)
        assertEquals(setOf("title", "isTogether"), body.keys)
        assertEquals("새 제목", body["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API PATCH events는 메모를 지울 때 note null을 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, timedJson))

        remote.update("e1", EventPatch(note = NoteChange.Clear)).value()

        val body = bodyOf(server.takeRequest())
        assertEquals(setOf("note"), body.keys)
        assertEquals(JsonNull, body["note"])
    }

    @Test
    fun `API PATCH events는 종일을 바꿀 때 start와 end를 함께 보낸다`() = runTest {
        server.enqueue(jsonResponse(200, allDayJson))

        remote.update("e2", EventPatch(time = EventTime.AllDay(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18)))).value()

        val body = bodyOf(server.takeRequest())
        assertEquals(setOf("isAllDay", "start", "end"), body.keys)
        assertTrue(body["isAllDay"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `API PATCH events의 403은 NOT_AUTHOR다`() = runTest {
        server.enqueue(jsonResponse(403, errorJson("NOT_AUTHOR")))

        assertEquals(ErrorCode.NOT_AUTHOR, remote.update("e1", EventPatch(title = "x")).apiError().knownCode)
    }

    @Test
    fun `API DELETE events는 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        remote.delete("e1").value()

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/events/e1", request.path)
    }

    @Test
    fun `API PUT events reminder는 option을 보내고 Event를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, timedJson))

        val event = remote.putReminder("e1", ReminderOption.MIN_10).value()

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/events/e1/reminder", request.path)
        assertEquals("MIN_10", bodyOf(request)["option"]?.jsonPrimitive?.content)
        assertEquals(ReminderOption.HOUR_1, event.reminder?.option)
    }

    @Test
    fun `API DELETE events reminder는 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        remote.deleteReminder("e1").value()

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/events/e1/reminder", request.path)
    }

    @Test
    fun `API GET events의 모르는 알림 option은 알림 없음으로 읽는다`() = runTest {
        val json = timedJson.replace("HOUR_1", "SOMETHING_NEW")
        server.enqueue(jsonResponse(200, json))

        assertNull(remote.get("e1").value().reminder)
    }

    @Test
    fun `API GET events의 일시 형식이 틀리면 공통 오류로 본다`() = runTest {
        server.enqueue(jsonResponse(200, timedJson.replace("2026-10-12T10:30:00Z", "어제")))

        assertEquals(ErrorCode.INTERNAL_ERROR, remote.get("e1").apiError().knownCode)
    }
}
