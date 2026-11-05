package com.duriseo.android.domain.event

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 일정·캘린더 규칙 (FR-CAL-01 AC6, FR-CAL-02, FR-CAL-03, FR-CAL-05) */
class EventRulesTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val me = "me"
    private val partner = "partner"

    private fun event(
        id: String = "e",
        time: EventTime = EventTime.AllDay(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12)),
        author: String = me,
        together: Boolean = false,
    ) = Event(
        id = id, title = "t", note = null, time = time, isTogether = together, authorId = author, updatedBy = null,
        reminder = null, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )

    @Test
    fun `FR-CAL-01 AC6 1950년 1월 이전과 2050년 12월 이후로는 넘길 수 없다`() {
        assertNull(EventRules.previousMonth(YearMonth.of(1950, 1)))
        assertEquals(YearMonth.of(1950, 1), EventRules.previousMonth(YearMonth.of(1950, 2)))
        assertNull(EventRules.nextMonth(YearMonth.of(2050, 12)))
        assertEquals(YearMonth.of(2050, 12), EventRules.nextMonth(YearMonth.of(2050, 11)))
        assertEquals(YearMonth.of(1950, 1), EventRules.clampMonth(YearMonth.of(1900, 5)))
        assertEquals(YearMonth.of(2050, 12), EventRules.clampMonth(YearMonth.of(2100, 5)))
    }

    @Test
    fun `FR-CAL-01 AC6 날짜 선택 범위는 1950-01-01부터 2050-12-31까지다`() {
        assertTrue(EventRules.isSelectable(LocalDate.of(1950, 1, 1)))
        assertTrue(EventRules.isSelectable(LocalDate.of(2050, 12, 31)))
        assertFalse(EventRules.isSelectable(LocalDate.of(1949, 12, 31)))
        assertFalse(EventRules.isSelectable(LocalDate.of(2051, 1, 1)))
        assertEquals(101, EventRules.years().size)
    }

    @Test
    fun `FR-CAL-02 AC1 D-89 제목은 앞뒤 공백을 지우고 공백만 있으면 제목이 없다`() {
        assertEquals("저녁 데이트", EventRules.normalizeTitle("  저녁 데이트 \n"))
        assertFalse(EventRules.isTitleValid("   "))
        assertTrue(EventRules.isTitleValid(" a "))
    }

    @Test
    fun `NFR-LIMIT-01 글자 수는 코드 포인트로 세어 한도까지만 남긴다`() {
        assertEquals(EventRules.TITLE_MAX, EventRules.limit("가".repeat(60), EventRules.TITLE_MAX).length)
        // 이모지는 UTF-16으로 둘이지만 한 글자다
        val emoji = "😀".repeat(52)
        val cut = EventRules.limit(emoji, EventRules.TITLE_MAX)
        assertEquals(50, cut.codePointCount(0, cut.length))
        assertEquals("짧은 글", EventRules.limit("짧은 글", EventRules.TITLE_MAX))
    }

    @Test
    fun `FR-CAL-02 AC3 종료가 시작보다 이르면 잘못이고 같으면 괜찮다`() {
        val a = Instant.parse("2026-10-12T10:00:00Z")
        assertFalse(EventRules.isEndValid(EventTime.Timed(a, a.minusSeconds(1))))
        assertTrue(EventRules.isEndValid(EventTime.Timed(a, a)))
        assertFalse(EventRules.isEndValid(EventTime.AllDay(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 11))))
        assertTrue(EventRules.isEndValid(EventTime.AllDay(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12))))
    }

    @Test
    fun `FR-CAL-02 AC5 여러 날 일정은 시작일부터 종료일까지 모든 날에 걸친다`() {
        val trip = event(time = EventTime.AllDay(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 19)))
        assertFalse(EventRules.occursOn(trip, LocalDate.of(2026, 10, 16), seoul))
        assertTrue(EventRules.occursOn(trip, LocalDate.of(2026, 10, 17), seoul))
        assertTrue(EventRules.occursOn(trip, LocalDate.of(2026, 10, 18), seoul))
        assertTrue(EventRules.occursOn(trip, LocalDate.of(2026, 10, 19), seoul))
        assertFalse(EventRules.occursOn(trip, LocalDate.of(2026, 10, 20), seoul))
    }

    @Test
    fun `FR-CAL-02 AC5 시간 일정은 기기 시간대 날짜로 걸친 날을 센다`() {
        // 서울 10월 12일 23:30 ~ 10월 13일 00:30
        val night = event(time = EventTime.Timed(Instant.parse("2026-10-12T14:30:00Z"), Instant.parse("2026-10-12T15:30:00Z")))
        assertEquals(LocalDate.of(2026, 10, 12), EventRules.firstDay(night, seoul))
        assertEquals(LocalDate.of(2026, 10, 13), EventRules.lastDay(night, seoul))
        assertTrue(EventRules.occursOn(night, LocalDate.of(2026, 10, 13), seoul))
        // 같은 순간이 UTC에서는 한 날이다
        assertEquals(LocalDate.of(2026, 10, 12), EventRules.lastDay(night, ZoneId.of("UTC")))
    }

    @Test
    fun `FR-CAL-01 AC2 날짜 목록은 종일 일정이 먼저고 시간 일정은 시작 시간순이다`() {
        val late = event(id = "late", time = EventTime.Timed(Instant.parse("2026-10-12T10:30:00Z"), Instant.parse("2026-10-12T11:00:00Z")))
        val early = event(id = "early", time = EventTime.Timed(Instant.parse("2026-10-12T05:00:00Z"), Instant.parse("2026-10-12T06:00:00Z")))
        val allDay = event(id = "allday")

        val sorted = EventRules.sortForDay(listOf(late, early, allDay)).map { it.id }

        assertEquals(listOf("allday", "early", "late"), sorted)
    }

    @Test
    fun `FR-CAL-02 AC4 일정 색은 대상의 사람 색이다`() {
        assertEquals(EventTarget.ME, EventRules.targetOf(event(author = me), me))
        assertEquals(EventTarget.PARTNER, EventRules.targetOf(event(author = partner), me))
        assertEquals(EventTarget.TOGETHER, EventRules.targetOf(event(author = partner, together = true), me))
        assertEquals(EventTarget.TOGETHER, EventRules.targetOf(event(author = me, together = true), me))
    }

    @Test
    fun `FR-CAL-03 AC5 권한 매트릭스대로 수정 삭제 여부를 정한다`() {
        // 작성자 + 나
        assertTrue(EventRules.canEdit(event(author = me), me))
        // 상대가 만든 나 일정(= 상대 일정)은 못 고친다
        assertFalse(EventRules.canEdit(event(author = partner), me))
        // 함께 일정은 작성자·상대 모두
        assertTrue(EventRules.canEdit(event(author = partner, together = true), me))
        assertTrue(EventRules.canEdit(event(author = me, together = true), me))
    }

    @Test
    fun `FR-CAL-03 AC4 대상은 작성자만 바꿀 수 있다`() {
        assertTrue(EventRules.canChangeTarget(event(author = me, together = true), me))
        assertFalse(EventRules.canChangeTarget(event(author = partner, together = true), me))
    }

    @Test
    fun `FR-CAL-05 AC1 AC3 시간 일정과 종일 일정의 알림 선택지가 다르다`() {
        assertEquals(
            listOf(ReminderOption.AT_TIME, ReminderOption.MIN_10, ReminderOption.HOUR_1, ReminderOption.DAY_1),
            ReminderOption.optionsFor(isAllDay = false),
        )
        assertEquals(
            listOf(ReminderOption.ALLDAY_9AM, ReminderOption.ALLDAY_PREV_9AM),
            ReminderOption.optionsFor(isAllDay = true),
        )
        assertNull(ReminderOption.fromCode("NONE"))
    }

    @Test
    fun `FR-CAL-03 AC4 EventPatch는 바뀐 것이 없으면 비어 있다`() {
        assertTrue(EventPatch().isEmpty)
        assertFalse(EventPatch(note = NoteChange.Clear).isEmpty)
    }
}
