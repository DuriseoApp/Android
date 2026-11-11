package com.duriseo.android.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** SCR-CAL-03 시간 휠 값 계산 (FR-CAL-02 AC1, design-system "시간 휠") */
class TimeWheelValuesTest {
    private val row = 132 // 줄 높이 44dp를 3배 밀도로 본 픽셀

    @Test
    fun `FR-CAL-02 AC1 스크롤 위치가 줄 높이의 정수배면 그 줄이 선택된다`() {
        assertEquals(0, TimeWheelValues.selectedIndex(0, 0, row, 12))
        assertEquals(6, TimeWheelValues.selectedIndex(6, 0, row, 12))
        assertEquals(6, TimeWheelValues.selectedIndex(5, row, row, 12))
    }

    @Test
    fun `FR-CAL-02 AC1 줄 높이의 절반을 넘게 밀리면 다음 줄이 선택된다`() {
        assertEquals(3, TimeWheelValues.selectedIndex(3, row / 2 - 1, row, 60))
        assertEquals(4, TimeWheelValues.selectedIndex(3, row / 2, row, 60))
    }

    @Test
    fun `FR-CAL-02 AC1 선택 위치는 값 개수 안으로 막는다`() {
        assertEquals(0, TimeWheelValues.selectedIndex(0, -50, row, 12))
        assertEquals(11, TimeWheelValues.selectedIndex(11, row, row, 12))
        assertEquals(0, TimeWheelValues.selectedIndex(3, 0, 0, 12))
    }

    @Test
    fun `FR-CAL-02 AC1 분 휠은 0부터 59까지 1분 단위다`() {
        assertEquals(60, TimeWheelValues.MINUTES.size)
        assertEquals(0, TimeWheelValues.MINUTES.first())
        assertEquals(59, TimeWheelValues.MINUTES.last())
        assertEquals((0..59).toList(), TimeWheelValues.MINUTES)
    }

    @Test
    fun `FR-CAL-02 AC1 시 휠은 1부터 12까지다`() {
        assertEquals((1..12).toList(), TimeWheelValues.HOURS)
    }

    @Test
    fun `FR-CAL-02 AC1 24시간 값을 오전 오후와 12시간 값으로 나눈다`() {
        assertEquals(12, TimeWheelValues.hour12(0))
        assertFalse(TimeWheelValues.isPm(0))
        assertEquals(11, TimeWheelValues.hour12(11))
        assertFalse(TimeWheelValues.isPm(11))
        assertEquals(12, TimeWheelValues.hour12(12))
        assertTrue(TimeWheelValues.isPm(12))
        assertEquals(7, TimeWheelValues.hour12(19))
        assertTrue(TimeWheelValues.isPm(19))
        assertEquals(11, TimeWheelValues.hour12(23))
    }

    @Test
    fun `FR-CAL-02 AC1 오전 오후와 12시간 값을 24시간 값으로 되돌린다`() {
        assertEquals(0, TimeWheelValues.hour24(12, pm = false))
        assertEquals(1, TimeWheelValues.hour24(1, pm = false))
        assertEquals(11, TimeWheelValues.hour24(11, pm = false))
        assertEquals(12, TimeWheelValues.hour24(12, pm = true))
        assertEquals(19, TimeWheelValues.hour24(7, pm = true))
        assertEquals(23, TimeWheelValues.hour24(11, pm = true))
    }

    @Test
    fun `FR-CAL-02 AC1 모든 시각은 나눴다 합쳐도 같다`() {
        for (hour in 0..23) {
            for (minute in listOf(0, 1, 30, 59)) {
                val time = LocalTime.of(hour, minute)
                val back = TimeWheelValues.time(TimeWheelValues.isPm(hour), TimeWheelValues.hour12(hour), minute)
                assertEquals(time, back)
            }
        }
    }

    @Test
    fun `D-128 가운데에서 떨어진 줄 수는 위아래 모두 절댓값이다`() {
        assertEquals(0, TimeWheelValues.distanceFromSelected(7, 7))
        assertEquals(1, TimeWheelValues.distanceFromSelected(6, 7))
        assertEquals(2, TimeWheelValues.distanceFromSelected(9, 7))
    }
}
