package com.duriseo.android.feature.calendar

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

/** SCR-CAL-01 달력 쪽 계산 (D-166) */
class CalendarMonthPagerTest {
    @Test
    fun `D-166 쪽은 1950년 1월부터 2050년 12월까지 1212개다`() {
        assertEquals(1212, MONTH_PAGE_COUNT)
        assertEquals(0, monthPage(YearMonth.of(1950, 1)))
        assertEquals(1211, monthPage(YearMonth.of(2050, 12)))
    }

    @Test
    fun `D-166 달과 쪽은 서로 되돌려진다`() {
        listOf(YearMonth.of(1950, 1), YearMonth.of(1999, 2), YearMonth.of(2026, 11), YearMonth.of(2050, 12)).forEach {
            assertEquals(it, pageMonth(monthPage(it)))
        }
        assertEquals(YearMonth.of(2026, 11), pageMonth(monthPage(YearMonth.of(2026, 10)) + 1))
    }

    @Test
    fun `D-166 범위 밖은 가까운 끝으로 맞춘다`() {
        assertEquals(0, monthPage(YearMonth.of(1949, 12)))
        assertEquals(1211, monthPage(YearMonth.of(2051, 1)))
        assertEquals(YearMonth.of(1950, 1), pageMonth(-3))
        assertEquals(YearMonth.of(2050, 12), pageMonth(5000))
    }
}
