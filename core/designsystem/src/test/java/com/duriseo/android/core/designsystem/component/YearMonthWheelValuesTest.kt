package com.duriseo.android.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

/** SCR-CAL-01 연도와 월 휠 값 (D-167) */
class YearMonthWheelValuesTest {
    @Test
    fun `D-167 연도 휠은 1950년부터 2050년까지 101개다`() {
        assertEquals(101, YearMonthWheelValues.YEARS.size)
        assertEquals(1950, YearMonthWheelValues.YEARS.first())
        assertEquals(2050, YearMonthWheelValues.YEARS.last())
    }

    @Test
    fun `D-167 월 휠은 1월부터 12월까지 12개다`() {
        assertEquals((1..12).toList(), YearMonthWheelValues.MONTHS)
    }

    @Test
    fun `D-167 처음 가운데에 놓을 위치는 보고 있는 달의 연도와 월이다`() {
        assertEquals(76, YearMonthWheelValues.yearIndex(2026))
        assertEquals(10, YearMonthWheelValues.monthIndex(11))
    }

    @Test
    fun `D-167 범위 밖 값은 가까운 끝으로 맞춘다`() {
        assertEquals(0, YearMonthWheelValues.yearIndex(1900))
        assertEquals(100, YearMonthWheelValues.yearIndex(2100))
        assertEquals(YearMonth.of(2050, 12), YearMonthWheelValues.yearMonth(2100, 13))
    }

    @Test
    fun `D-167 연도와 월로 달을 만든다`() {
        assertEquals(YearMonth.of(1999, 2), YearMonthWheelValues.yearMonth(1999, 2))
    }
}
