package com.duriseo.android.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

/** 점선 원(아직 없음 자리) 간격 계산: 점 6 · 간격 4 비율, 둘레를 정수 조각으로 나눈다 (SCR-SET-01) */
class RingDashTest {
    @Test
    fun `SCR-SET-01 점선 원은 점 6 간격 4 비율을 지킨다`() {
        val intervals = ringDashIntervals(circumference = 170f, dash = 6f, gap = 4f)
        assertEquals(0.6f, intervals[0] / (intervals[0] + intervals[1]), 0.0001f)
    }

    @Test
    fun `SCR-SET-01 점선 원은 둘레를 정수 조각으로 나눠 이음매가 없다`() {
        val circumference = 169.6f
        val intervals = ringDashIntervals(circumference, 6f, 4f)
        val piece = intervals[0] + intervals[1]
        assertEquals(17f, circumference / piece, 0.0001f)
    }

    @Test
    fun `SCR-SET-01 아주 작은 둘레도 조각이 하나는 있다`() {
        val intervals = ringDashIntervals(3f, 6f, 4f)
        assertEquals(3f, intervals[0] + intervals[1], 0.0001f)
    }
}
