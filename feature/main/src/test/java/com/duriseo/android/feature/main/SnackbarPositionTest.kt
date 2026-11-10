package com.duriseo.android.feature.main

import com.duriseo.android.core.common.navigation.AnniversaryRouteModel
import com.duriseo.android.core.common.navigation.CalendarRouteModel
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.main.navigation.isSnackbarAtTop
import kotlin.reflect.KClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnackbarPositionTest {

    private fun atTop(current: KClass<*>): Boolean = isSnackbarAtTop { it == current }

    @Test
    fun `SCR-CAL-01 스낵바는 위에서 40이 아니라 하단 탭 위다`() {
        assertFalse(atTop(MainRouteModel.Calendar::class))
    }

    @Test
    fun `SCR-CAL-02 일정 상세의 스낵바는 위에서 40이다`() {
        assertTrue(atTop(CalendarRouteModel.EventDetail::class))
    }

    @Test
    fun `SCR-CAL-03 일정 작성 수정의 스낵바는 위에서 40이다`() {
        assertTrue(atTop(CalendarRouteModel.EventEdit::class))
    }

    @Test
    fun `D-126 SCR-ANNIV-01 기념일 목록의 스낵바는 위에서 40이다`() {
        assertTrue(atTop(AnniversaryRouteModel.Anniversaries::class))
    }

    @Test
    fun `D-126 SCR-ANNIV-02 기념일 작성 수정의 스낵바는 위에서 40이다`() {
        assertTrue(atTop(AnniversaryRouteModel.Edit::class))
    }

    @Test
    fun `메모 탭 같은 다른 화면은 위에서 40이 아니다`() {
        assertFalse(atTop(MainRouteModel.Memo::class))
    }
}
