package com.duriseo.android.data.repository.event

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.errorJson
import com.duriseo.android.data.repository.fixtureCoupleMe
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.coupleMeJson
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.event.EventPatch
import com.duriseo.android.domain.event.EventRules
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.event.NewEvent
import com.duriseo.android.domain.event.ReminderOption
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 일정 리포지토리 (API.md 3.3): Room 구독, 범위 refresh, 온라인 쓰기 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EventRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private var created: RepoHarness? = null
    private val seoul = ZoneId.of("Asia/Seoul")

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        created?.close()
        runCatching { server.shutdown() }
    }

    private fun harness(online: Boolean = true): RepoHarness =
        RepoHarness(server, tempFolder.root, online = online, initialTokens = testTokens()).also { created = it }

    private fun eventJson(
        id: String,
        title: String = "일정 $id",
        isAllDay: Boolean = false,
        start: String = "2026-10-12T10:30:00Z",
        end: String = "2026-10-12T12:30:00Z",
        reminder: String = "null",
        together: Boolean = false,
    ) = """{"id":"$id","title":"$title","note":null,"isAllDay":$isAllDay,"start":"$start","end":"$end",
        "isTogether":$together,"authorId":"11111111-1111-1111-1111-111111111111","updatedBy":null,"reminder":$reminder,
        "createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-02T00:00:00Z"}"""

    private fun listJson(vararg events: String) = """{"items":[${events.joinToString(",")}]}"""

    private val october = LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 31)

    private suspend fun RepoHarness.titles(from: LocalDate = october.first, to: LocalDate = october.second): List<String> =
        eventRepository.observeEvents(from, to, seoul).first().map { it.title }

    @Test
    fun `NFR-SYNC-02 범위를 다시 불러오면 서버에서 지워진 일정이 Room에서 사라진다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson(eventJson("a"), eventJson("b"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        assertEquals(listOf("일정 a", "일정 b"), h.titles())

        server.enqueue(jsonResponse(200, listJson(eventJson("a"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)

        assertEquals(listOf("일정 a"), h.titles())
    }

    @Test
    fun `NFR-SYNC-02 범위를 다시 불러와도 범위 밖 일정은 그대로다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson(eventJson("oct"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        // 12월 일정
        server.enqueue(
            jsonResponse(
                200,
                listJson(eventJson("dec", start = "2026-12-05T01:00:00Z", end = "2026-12-05T02:00:00Z")),
            ),
        )
        val december = LocalDate.of(2026, 12, 1) to LocalDate.of(2026, 12, 31)
        h.eventRepository.refreshRange(december.first, december.second, seoul)

        // 10월을 다시 받아 비워도 12월은 남는다
        server.enqueue(jsonResponse(200, listJson()))
        h.eventRepository.refreshRange(october.first, october.second, seoul)

        assertEquals(emptyList<String>(), h.titles())
        assertEquals(listOf("일정 dec"), h.titles(december.first, december.second))
    }

    @Test
    fun `FR-CAL-02 AC5 여러 날 일정은 걸친 모든 날에 나온다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(
            jsonResponse(
                200,
                listJson(eventJson("trip", isAllDay = true, start = "2026-10-17", end = "2026-10-18")),
            ),
        )
        h.eventRepository.refreshRange(october.first, october.second, seoul)

        assertEquals(listOf("일정 trip"), h.titles(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 17)))
        assertEquals(listOf("일정 trip"), h.titles(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18)))
        assertEquals(emptyList<String>(), h.titles(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 19)))
        assertEquals(emptyList<String>(), h.titles(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 16)))
    }

    @Test
    fun `FR-CAL-02 AC5 시간 일정은 기기 시간대 날짜로 걸친 날을 센다`() = runTestWithTimeout {
        val h = harness()
        // 서울 10월 12일 23:30 ~ 10월 13일 00:30 (UTC 14:30 ~ 15:30)
        server.enqueue(
            jsonResponse(200, listJson(eventJson("night", start = "2026-10-12T14:30:00Z", end = "2026-10-12T15:30:00Z"))),
        )
        h.eventRepository.refreshRange(october.first, october.second, seoul)

        assertEquals(listOf("일정 night"), h.titles(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12)))
        assertEquals(listOf("일정 night"), h.titles(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 13)))
        assertEquals(emptyList<String>(), h.titles(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 14)))
    }

    @Test
    fun `NFR-OFF-01 오프라인이면 다시 불러오기는 조용히 실패하고 저장된 일정이 남는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson(eventJson("a"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        h.monitor.online = false

        val result = h.eventRepository.refreshRange(october.first, october.second, seoul)

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(listOf("일정 a"), h.titles())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `FR-CAL-01 AC6 범위 조회는 1950-01-01 ~ 2050-12-31 안으로 잘라서 요청한다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson()))

        h.eventRepository.refreshRange(LocalDate.of(1949, 12, 1), LocalDate.of(1950, 1, 31), seoul)

        assertEquals("/api/v1/events?from=1950-01-01&to=1950-01-31", server.takeRequest().path)
    }

    @Test
    fun `FR-CAL-01 AC6 범위가 모두 캘린더 밖이면 요청하지 않는다`() = runTestWithTimeout {
        val h = harness()

        val result = h.eventRepository.refreshRange(LocalDate.of(2051, 1, 1), LocalDate.of(2051, 1, 31), seoul)

        assertTrue(result is DataResult.Success)
        assertEquals(0, server.requestCount)
        assertEquals(LocalDate.of(2050, 12, 31), EventRules.MAX_DATE)
    }

    @Test
    fun `FR-CAL-02 AC1 저장하면 POST events를 보내고 응답을 Room에 넣는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(201, eventJson("new", title = "저녁 데이트")))

        val result = h.eventRepository.create(
            NewEvent("저녁 데이트", null, EventTime.Timed(Instant.parse("2026-10-12T10:30:00Z"), Instant.parse("2026-10-12T12:30:00Z"))),
        )

        assertEquals("new", (result as DataResult.Success).value.id)
        assertEquals(listOf("저녁 데이트"), h.titles())
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 일정 저장 요청을 보내지 않고 안내한다`() = runTestWithTimeout {
        val h = harness(online = false)

        val result = h.eventRepository.create(NewEvent("x", null, EventTime.AllDay(october.first, october.first)))

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 일정 삭제 요청을 보내지 않고 안내한다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson(eventJson("a"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        h.monitor.online = false

        val result = h.eventRepository.delete("a")

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(1, server.requestCount)
        assertEquals(listOf("일정 a"), h.titles())
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 일정 알림 설정 요청을 보내지 않고 안내한다`() = runTestWithTimeout {
        val h = harness(online = false)

        val result = h.eventRepository.setReminder("a", ReminderOption.MIN_10)

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `FR-CAL-03 PATCH events 응답으로 Room의 일정이 바뀐다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson(eventJson("a", title = "옛 제목"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        server.enqueue(jsonResponse(200, eventJson("a", title = "새 제목")))

        val result = h.eventRepository.update("a", EventPatch(title = "새 제목"))

        assertTrue(result is DataResult.Success)
        assertEquals(listOf("새 제목"), h.titles())
    }

    @Test
    fun `FR-CAL-03 바뀐 것이 없으면 PATCH를 보내지 않는다`() = runTestWithTimeout {
        val h = harness()

        val result = h.eventRepository.update("a", EventPatch())

        assertTrue(result is DataResult.Success)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `FR-CAL-03 DELETE events 성공이면 Room에서 일정이 사라진다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson(eventJson("a"), eventJson("b"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        server.enqueue(MockResponse().setResponseCode(204))

        h.eventRepository.delete("a")

        assertEquals(listOf("일정 b"), h.titles())
    }

    @Test
    fun `FR-CAL-03 404 NOT_FOUND이면 Room에서 그 일정을 지운다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson(eventJson("a"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        val result = h.eventRepository.update("a", EventPatch(title = "x"))

        assertTrue((result as DataResult.Failure).error.hasCode(ErrorCode.NOT_FOUND))
        assertEquals(emptyList<String>(), h.titles())
    }

    @Test
    fun `FR-CAL-03 403 NOT_AUTHOR이면 그 일정을 다시 읽는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson(eventJson("a", title = "낡은 제목"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        server.enqueue(jsonResponse(403, errorJson("NOT_AUTHOR")))
        server.enqueue(jsonResponse(200, eventJson("a", title = "최신 제목")))

        val result = h.eventRepository.delete("a")

        assertTrue((result as DataResult.Failure).error.hasCode(ErrorCode.NOT_AUTHOR))
        assertEquals(listOf("최신 제목"), h.titles())
    }

    @Test
    fun `FR-CAL-05 AC2 응답의 reminder를 그대로 Room에 저장한다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, listJson(eventJson("a"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        server.enqueue(
            jsonResponse(200, eventJson("a", reminder = """{"option":"MIN_10","fireAt":"2026-10-12T10:20:00Z"}""")),
        )

        h.eventRepository.setReminder("a", ReminderOption.MIN_10)

        val reminder = h.eventRepository.observeEvent("a").first()?.reminder
        assertEquals(ReminderOption.MIN_10, reminder?.option)
        assertEquals(Instant.parse("2026-10-12T10:20:00Z"), reminder?.fireAt)
    }

    @Test
    fun `FR-CAL-05 AC5 fireAt이 과거여도 알림 설정을 그대로 보여 준다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(
            jsonResponse(
                200,
                listJson(eventJson("old", start = "2020-01-01T00:00:00Z", end = "2020-01-01T01:00:00Z",
                    reminder = """{"option":"AT_TIME","fireAt":"2020-01-01T00:00:00Z"}""")),
            ),
        )
        h.eventRepository.refreshRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31), ZoneId.of("UTC"))

        val event = h.eventRepository.observeEvent("old").first()
        assertEquals(ReminderOption.AT_TIME, event?.reminder?.option)
    }

    @Test
    fun `FR-CAL-05 AC1 없음을 고르면 DELETE reminder를 보내고 Room의 알림이 사라진다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(
            jsonResponse(200, listJson(eventJson("a", reminder = """{"option":"MIN_10","fireAt":"2026-10-12T10:20:00Z"}"""))),
        )
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        server.enqueue(MockResponse().setResponseCode(204))

        h.eventRepository.setReminder("a", null)

        server.takeRequest()
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/events/a/reminder", delete.path)
        assertNull(h.eventRepository.observeEvent("a").first()?.reminder)
    }

    @Test
    fun `FR-CAL-05 AC4 종일을 바꿔 저장한 응답의 reminder가 null이면 알림이 없음으로 바뀐다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(
            jsonResponse(200, listJson(eventJson("a", reminder = """{"option":"MIN_10","fireAt":"2026-10-12T10:20:00Z"}"""))),
        )
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        server.enqueue(jsonResponse(200, eventJson("a", isAllDay = true, start = "2026-10-12", end = "2026-10-12")))

        h.eventRepository.update("a", EventPatch(time = EventTime.AllDay(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12))))

        assertNull(h.eventRepository.observeEvent("a").first()?.reminder)
    }

    @Test
    fun `FR-COUPLE-05 AC3 해제하면 일정도 Room에서 지운다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureCoupleMe())
        server.enqueue(jsonResponse(200, listJson(eventJson("a"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        server.enqueue(MockResponse().setResponseCode(204)) // DELETE /couple
        server.enqueue(jsonResponse(200, com.duriseo.android.data.repository.meJson(accountType = "MEMBER")))

        h.coupleRepository.unlink()

        assertEquals(emptyList<String>(), h.titles())
    }

    @Test
    fun `FR-COUPLE-03 공간이 바뀌면 이전 공간의 일정 캐시를 비운다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(200, listJson(eventJson("personal"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        // 연결: 개인 공간 -> 커플 공간
        server.enqueue(jsonResponse(200, coupleMeJson()))

        h.meRepository.refreshMe()

        assertEquals(emptyList<String>(), h.titles())
    }

    @Test
    fun `FR-COUPLE-03 가져오기를 정하면 일정 캐시를 비우고 다시 받게 한다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureCoupleMe(importPending = true))
        server.enqueue(jsonResponse(200, listJson(eventJson("a"))))
        h.eventRepository.refreshRange(october.first, october.second, seoul)
        server.enqueue(MockResponse().setResponseCode(204)) // POST /couple/import
        server.enqueue(jsonResponse(200, coupleMeJson()))

        h.coupleRepository.decideImport(true)

        assertEquals(emptyList<String>(), h.titles())
    }
}
