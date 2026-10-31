package com.duriseo.android.domain.couple

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import java.time.LocalDate
import java.time.YearMonth

class StartDateRulesTest {
    private val today = LocalDate.of(2026, 10, 31)

    @Test
    fun `FR-COUPLE-04 AC3 오늘보다 뒤 날짜는 고를 수 없다`() {
        assertFalse(StartDateRules.isSelectable(today.plusDays(1), today))
    }

    @Test
    fun `FR-COUPLE-04 AC3 오늘은 고를 수 있다`() {
        assertTrue(StartDateRules.isSelectable(today, today))
    }

    @Test
    fun `FR-COUPLE-04 AC3 1950년 1월 1일보다 앞 날짜는 고를 수 없다`() {
        assertFalse(StartDateRules.isSelectable(LocalDate.of(1949, 12, 31), today))
    }

    @Test
    fun `FR-COUPLE-04 AC3 1950년 1월 1일은 고를 수 있다`() {
        assertTrue(StartDateRules.isSelectable(LocalDate.of(1950, 1, 1), today))
    }

    @Test
    fun `FR-COUPLE-04 AC3 달력은 고른 날이 있으면 그 달로 열고 없으면 오늘이 든 달로 연다`() {
        assertEquals(YearMonth.of(2025, 5), StartDateRules.initialMonth(LocalDate.of(2025, 5, 20), today))
        assertEquals(YearMonth.of(2026, 10), StartDateRules.initialMonth(null, today))
    }

    @Test
    fun `FR-COUPLE-04 AC3 이전 달은 1950년 1월까지만 갈 수 있다`() {
        assertEquals(YearMonth.of(1950, 1), StartDateRules.previousMonth(YearMonth.of(1950, 2)))
        assertNull(StartDateRules.previousMonth(YearMonth.of(1950, 1)))
    }

    @Test
    fun `FR-COUPLE-04 AC3 다음 달은 오늘이 든 달까지만 갈 수 있다`() {
        assertEquals(YearMonth.of(2026, 10), StartDateRules.nextMonth(YearMonth.of(2026, 9), today))
        assertNull(StartDateRules.nextMonth(YearMonth.of(2026, 10), today))
    }
}
