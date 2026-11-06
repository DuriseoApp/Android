package com.duriseo.android.feature.calendar

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarFabPositionTest {

    @Test
    fun `SCR-CAL-01 스낵바가 없으면 FAB은 탭 위 20이다 (바닥에서 100)`() {
        assertEquals(20.dp, calendarFabBottom(snackbarVisible = false))
    }

    @Test
    fun `SCR-CAL-01 스낵바가 떠 있으면 FAB은 스낵바 위 16이다 (바닥에서 161)`() {
        // 탭 80 + 81 = 161
        assertEquals(81.dp, calendarFabBottom(snackbarVisible = true))
    }
}
