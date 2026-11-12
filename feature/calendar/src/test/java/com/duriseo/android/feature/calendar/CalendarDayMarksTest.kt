package com.duriseo.android.feature.calendar

import com.duriseo.android.domain.event.EventTarget
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarDayMarksTest {

    private fun allDay(id: String, target: EventTarget) = CalendarEvent(id, id, null, true, null, target)

    private fun timed(id: String, target: EventTarget, hour: Int = 10) =
        CalendarEvent(id, id, null, false, LocalTime.of(hour, 0), target)

    @Test
    fun `기념일이 있으면 고리 하나와 점 둘까지다`() {
        val events = listOf(
            allDay("a", EventTarget.ME),
            timed("b", EventTarget.PARTNER),
            timed("c", EventTarget.TOGETHER),
        )
        val marks = calendarDayMarks(hasAnniversary = true, events = events)
        assertTrue(marks.hasAnniversary)
        assertEquals(listOf(EventTarget.ME, EventTarget.PARTNER), marks.dots)
        // 고리 1 + 점 2 = 3개 (MAX_DAY_MARKS)
        assertEquals(MAX_DAY_MARKS, 1 + marks.dots.size)
    }

    @Test
    fun `기념일이 없으면 점 셋까지다`() {
        val events = listOf(
            allDay("a", EventTarget.ME),
            timed("b", EventTarget.PARTNER),
            timed("c", EventTarget.TOGETHER),
        )
        val marks = calendarDayMarks(hasAnniversary = false, events = events)
        assertFalse(marks.hasAnniversary)
        assertEquals(listOf(EventTarget.ME, EventTarget.PARTNER, EventTarget.TOGETHER), marks.dots)
    }

    @Test
    fun `기념일만 있으면 고리만 있고 여럿이어도 고리는 하나다`() {
        val marks = calendarDayMarks(hasAnniversary = true, events = emptyList())
        assertTrue(marks.hasAnniversary)
        assertTrue(marks.dots.isEmpty())
    }

    @Test
    fun `FR-CAL-01 AC8 같은 색 점은 한 번만 종일 시간 순이다`() {
        val events = listOf(
            // 받은 순서는 시간 일정이 먼저여도 종일이 앞선다
            timed("t1", EventTarget.PARTNER, hour = 8),
            timed("t2", EventTarget.ME, hour = 9),
            allDay("d1", EventTarget.ME),
            allDay("d2", EventTarget.ME),
            timed("t3", EventTarget.TOGETHER, hour = 20),
        )
        val marks = calendarDayMarks(hasAnniversary = false, events = events)
        // 종일(나) -> 시간(상대, 나는 이미 있음, 함께)
        assertEquals(listOf(EventTarget.ME, EventTarget.PARTNER, EventTarget.TOGETHER), marks.dots)
    }

    @Test
    fun `FR-CAL-01 AC8 일정이 여럿이어도 사람 색이 하나뿐이면 점도 하나다`() {
        val events = listOf(timed("a", EventTarget.ME), timed("b", EventTarget.ME), allDay("c", EventTarget.ME))
        assertEquals(listOf(EventTarget.ME), calendarDayMarks(false, events).dots)
    }

    @Test
    fun `일정과 기념일이 모두 없으면 표시가 없다`() {
        val marks = calendarDayMarks(hasAnniversary = false, events = emptyList())
        assertFalse(marks.hasAnniversary)
        assertTrue(marks.dots.isEmpty())
    }

    private fun memo(id: String, author: EventTarget) = CalendarMemo(id, id, author)

    @Test
    fun `FR-CAL-01 AC8 메모 점은 작성자 색이고 시간 일정 뒤다`() {
        val events = listOf(timed("t", EventTarget.TOGETHER), allDay("a", EventTarget.ME))
        val memos = listOf(memo("m1", EventTarget.PARTNER), memo("m2", EventTarget.ME))
        // 종일(나) -> 시간(함께) -> 메모(상대, 나는 이미 있어 한 번만)
        assertEquals(
            listOf(EventTarget.ME, EventTarget.TOGETHER, EventTarget.PARTNER),
            calendarDayMarks(hasAnniversary = false, events = events, memos = memos).dots,
        )
        // 고리가 있으면 점은 2개: 메모는 밀려난다
        assertEquals(
            listOf(EventTarget.ME, EventTarget.TOGETHER),
            calendarDayMarks(hasAnniversary = true, events = events, memos = memos).dots,
        )
    }

    @Test
    fun `FR-CAL-01 AC8 메모만 있는 날은 작성자 색 점만 서로 다른 사람 색으로 보인다`() {
        val memos = listOf(memo("m1", EventTarget.PARTNER), memo("m2", EventTarget.PARTNER), memo("m3", EventTarget.ME))
        val marks = calendarDayMarks(hasAnniversary = false, events = emptyList(), memos = memos)
        assertEquals(listOf(EventTarget.PARTNER, EventTarget.ME), marks.dots)
        assertFalse(marks.hasAnniversary)
    }
}
