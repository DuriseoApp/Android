package com.duriseo.android.feature.calendar

import com.duriseo.android.domain.event.EventTarget

/** 한 날의 달력 표시는 기념일 고리를 포함해 최대 3개다 (고리 1 + 사람 색 점 2, SCREENS.md SCR-CAL-01 "날짜 표시") */
internal const val MAX_DAY_MARKS = 3

/**
 * 달력 날짜 칸의 표시 (D-65·D-118, FR-CAL-01 AC2·AC8).
 *
 * @property hasAnniversary 기념일이 하나라도 있으면 `brand-500` 고리 하나 (기념일이 여럿이어도 고리는 하나)
 * @property dots 앞에서부터 고른 서로 다른 사람 색 (종일 -> 시간 순). 대상으로 구분하고 색은 그리는 쪽이 정한다
 */
data class CalendarDayMarks(
    val hasAnniversary: Boolean,
    val dots: List<EventTarget>,
)

/**
 * 점 고르는 규칙 (D-118): 종일 일정 -> 시간 일정 순으로 훑어 서로 다른 사람 색만 앞에서부터 고른다(같은 색은 한 번만).
 * 고리가 있으면 점은 2개, 없으면 3개까지다. 같은 종류 안에서는 받은 순서를 지킨다.
 * 메모(작성자 색)는 단위 6에서 시간 일정 뒤에 더한다.
 */
fun calendarDayMarks(hasAnniversary: Boolean, events: List<CalendarEvent>): CalendarDayMarks {
    val maxDots = MAX_DAY_MARKS - if (hasAnniversary) 1 else 0
    val dots = events
        .sortedBy { if (it.isAllDay) 0 else 1 }
        .map { it.target }
        .distinct()
        .take(maxDots)
    return CalendarDayMarks(hasAnniversary = hasAnniversary, dots = dots)
}
