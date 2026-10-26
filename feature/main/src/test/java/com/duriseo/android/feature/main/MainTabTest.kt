package com.duriseo.android.feature.main

import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.feature.main.component.MainTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MainTabTest {

    @Test
    fun `D-30 하단 탭은 캘린더와 메모 두 개다`() {
        assertEquals(listOf(MainTab.CALENDAR, MainTab.MEMO), MainTab.entries)
        assertEquals(
            listOf<MainRouteModel>(MainRouteModel.Calendar, MainRouteModel.Memo),
            MainTab.entries.map { it.route },
        )
        // 전체 메뉴(설정)는 탭이 아니다
        assertFalse(MainTab.entries.any { it.route == MainRouteModel.Settings })
    }

    @Test
    fun `D-30 앱의 첫 화면은 캘린더 탭이다`() {
        assertEquals(MainTab.CALENDAR, MainTab.START)
        assertEquals(MainRouteModel.Calendar, MainTab.START.route)
    }

    @Test
    fun `D-30 탭 라벨은 strings의 캘린더와 메모다`() {
        assertEquals(DesignR.string.tab_calendar, MainTab.CALENDAR.labelRes)
        assertEquals(DesignR.string.tab_memo, MainTab.MEMO.labelRes)
    }

    @Test
    fun `D-30 탭 아이콘은 달력과 메모 선 아이콘이다`() {
        assertEquals(DesignR.drawable.ic_tab_calendar, MainTab.CALENDAR.iconRes)
        assertEquals(DesignR.drawable.ic_tab_memo, MainTab.MEMO.iconRes)
    }
}
