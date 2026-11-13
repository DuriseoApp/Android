package com.duriseo.android.feature.main

import com.duriseo.android.core.common.navigation.AnniversaryRouteModel
import com.duriseo.android.core.common.navigation.CalendarRouteModel
import com.duriseo.android.core.common.navigation.CoupleRouteModel
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.core.common.navigation.MemoRouteModel
import com.duriseo.android.core.common.navigation.SettingsRouteModel
import com.duriseo.android.feature.main.navigation.isSnackbarAtTop
import kotlin.reflect.KClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnackbarPositionTest {

    private fun atTop(current: KClass<*>, linkedSheetVisible: Boolean = false): Boolean =
        isSnackbarAtTop(linkedSheetVisible) { it == current }

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
    fun `메모 탭의 스낵바는 위에서 40이 아니라 하단 탭 위다`() {
        assertFalse(atTop(MainRouteModel.Memo::class))
    }

    @Test
    fun `D-126 메모 탭은 바닥 96이고 메모 하위 화면은 위 40이다`() {
        // SCR-MEMO-01은 하단 탭이 있는 탭 루트라 바닥에서 96 (FAB은 바닥에서 161)
        assertFalse(atTop(MainRouteModel.Memo::class))
        // SCR-MEMO-02 고정 메모 전체, SCR-MEMO-03 상세, SCR-MEMO-04 작성·수정은 하단 탭이 없어 위에서 40
        assertTrue(atTop(MemoRouteModel.Pinned::class))
        assertTrue(atTop(MemoRouteModel.Detail::class))
        assertTrue(atTop(MemoRouteModel.Edit::class))
    }

    @Test
    fun `D-126 탭이 없는 화면과 시트 위는 위에서 40이다`() {
        // SCR-COUPLE-01·02·03 (바닥에 고정 버튼이 있는 SCR-COUPLE-02도 예외 없다)
        assertTrue(atTop(CoupleRouteModel.Connect::class))
        assertTrue(atTop(CoupleRouteModel.InviteCreate::class))
        assertTrue(atTop(CoupleRouteModel.InviteEnter::class))
        // 전체 메뉴와 그 하위 화면
        assertTrue(atTop(MainRouteModel.Settings::class))
        assertTrue(atTop(SettingsRouteModel.Account::class))
        assertTrue(atTop(SettingsRouteModel.Colors::class))
        assertTrue(atTop(SettingsRouteModel.ConnectionInfo::class))
        // 연결 완료 시트(SCR-COUPLE-04)가 캘린더 위에 떠 있는 동안
        assertTrue(atTop(MainRouteModel.Calendar::class, linkedSheetVisible = true))
        // 시트가 없으면 탭 화면은 그대로 하단 탭 위
        assertFalse(atTop(MainRouteModel.Calendar::class, linkedSheetVisible = false))
    }
}
