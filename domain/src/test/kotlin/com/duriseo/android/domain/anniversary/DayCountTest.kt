package com.duriseo.android.domain.anniversary

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayCountTest {
    private val start = LocalDate.of(2026, 1, 1)

    @Test
    fun `FR-ANNIV-01 AC1 처음 만난 날 당일은 D+1이다`() {
        assertEquals(1L, DayCount.of(start, LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `FR-ANNIV-01 AC1 다음 날은 D+2다`() {
        assertEquals(2L, DayCount.of(start, LocalDate.of(2026, 1, 2)))
    }

    @Test
    fun `FR-ANNIV-01 AC3 계산값이 1보다 작으면 D+1이다`() {
        assertEquals(1L, DayCount.of(start, LocalDate.of(2025, 12, 31)))
        assertEquals(1L, DayCount.of(start, LocalDate.of(2025, 6, 1)))
    }

    @Test
    fun `FR-ANNIV-02 AC6 자동 기념일 카드의 D+N은 기념일 날짜에서 처음 만난 날을 뺀 값에 1을 더한다`() {
        // 100일 = 처음 만난 날 + 99일 = D+100 (D-117)
        assertEquals(100L, DayCount.onDate(start, LocalDate.of(2026, 4, 10)))
        assertEquals(1L, DayCount.onDate(start, start))
    }
}
