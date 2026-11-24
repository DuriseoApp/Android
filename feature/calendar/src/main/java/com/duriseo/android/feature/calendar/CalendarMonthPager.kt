package com.duriseo.android.feature.calendar

import com.duriseo.android.domain.event.EventRules
import java.time.YearMonth

// SCR-CAL-01 옆으로 쓸어 달 바꾸기 (D-166). 달력은 HorizontalPager이고 한 쪽이 한 달이다.
// 1950-01이 0쪽, 2050-12가 마지막 쪽이다. 달 ↔ 쪽 계산은 순수 함수로 두고 화면 없이 테스트한다 (`CalendarMonthPagerTest`).

private const val MONTHS_PER_YEAR = 12

/** 1950-01 ~ 2050-12의 달 수 (1212) */
internal val MONTH_PAGE_COUNT: Int =
    (EventRules.MAX_MONTH.year - EventRules.MIN_MONTH.year) * MONTHS_PER_YEAR +
        (EventRules.MAX_MONTH.monthValue - EventRules.MIN_MONTH.monthValue) + 1

/** [month]가 놓이는 쪽. 범위 밖이면 가까운 끝 쪽 */
internal fun monthPage(month: YearMonth): Int {
    val clamped = EventRules.clampMonth(month)
    return (clamped.year - EventRules.MIN_MONTH.year) * MONTHS_PER_YEAR +
        (clamped.monthValue - EventRules.MIN_MONTH.monthValue)
}

/** [page]쪽의 달. 범위 밖이면 가까운 끝 달 */
internal fun pageMonth(page: Int): YearMonth =
    EventRules.MIN_MONTH.plusMonths(page.coerceIn(0, MONTH_PAGE_COUNT - 1).toLong())
