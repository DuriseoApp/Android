package com.duriseo.android.feature.calendar.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.CalendarRouteModel
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.calendar.CalendarRoute
import com.duriseo.android.feature.calendar.detail.EventDetailRoute
import com.duriseo.android.feature.calendar.edit.EventEditMemoTabProvider
import com.duriseo.android.feature.calendar.edit.EventEditRoute
import java.time.LocalDate

fun NavController.navigateToCalendar(navOptions: NavOptions) {
    navigate(MainRouteModel.Calendar, navOptions)
}

/** 일정 상세 (SCR-CAL-02). 하단 탭이 없다 */
fun NavController.navigateToEventDetail(eventId: String) {
    navigate(CalendarRouteModel.EventDetail(eventId))
}

/**
 * 새 작성 화면 (SCR-CAL-03, [일정|메모] 탭). [date]는 캘린더에서 고른 날이고 일정 탭의 날짜만 정한다(없으면 오늘, 메모 날짜는 비운다, D-133).
 * [startOnMemo]이면 [메모] 탭을 고른 채로 연다 (메모 탭의 FAB)
 */
fun NavController.navigateToEventCreate(date: LocalDate? = null, startOnMemo: Boolean = false) {
    navigate(CalendarRouteModel.EventEdit(date = date?.toString(), startOnMemo = startOnMemo))
}

/** 일정 수정 (SCR-CAL-03) */
fun NavController.navigateToEventEdit(eventId: String) {
    navigate(CalendarRouteModel.EventEdit(eventId = eventId))
}

/**
 * @param onOpenMenu 캘린더 앱바의 [전체 메뉴]
 * @param onOpenAnniversaries 앱바 "함께한 지 D+N ›"·"기념일 ›"와 날짜 목록의 기념일 카드 -> SCR-ANNIV-01
 * @param onOpenNotifications 앱바의 알림 버튼 -> SCR-NOTI-01 (`feature:notification`을 모르므로 앱 쪽 이동에 맡긴다)
 * @param onOpenMemo 날짜 목록의 메모 카드 -> SCR-MEMO-03 (메모 화면은 `feature:memo`라 앱 쪽 이동에 맡긴다)
 * @param onOpenColors 작성 화면의 "색상 변경" 링크. 화면 모듈끼리는 서로 모르므로 앱 쪽 이동에 맡긴다
 * @param memoTab 새 작성 화면의 [메모] 탭 내용. `feature:memo`를 모르므로 앱(`feature:main`)이 채운다 (D-133, [EventEditMemoTab]).
 *   null이면 종류 탭 없이 일정만 쓴다. 작성 화면의 내비게이션 항목 안에서 불러 두 ViewModel이 같은 항목에 산다
 */
fun NavGraphBuilder.calendarNavGraph(
    padding: PaddingValues,
    onOpenMenu: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenMemo: (String) -> Unit,
    onOpenCreate: (LocalDate) -> Unit,
    onOpenEdit: (String) -> Unit,
    onOpenAnniversaries: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenColors: () -> Unit,
    onBack: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    snackbarVisible: Boolean = false,
    memoTab: EventEditMemoTabProvider? = null,
) {
    composable<MainRouteModel.Calendar> { entry ->
        CalendarRoute(
            lifecycleOwner = entry,
            padding = padding,
            onOpenMenu = onOpenMenu,
            onOpenEvent = onOpenEvent,
            onOpenMemo = onOpenMemo,
            onOpenCreate = onOpenCreate,
            onOpenAnniversaries = onOpenAnniversaries,
            onOpenNotifications = onOpenNotifications,
            onShowSnackBar = onShowSnackBar,
            snackbarVisible = snackbarVisible,
        )
    }
    composable<CalendarRouteModel.EventDetail> { entry ->
        EventDetailRoute(
            lifecycleOwner = entry,
            padding = padding,
            onClose = onBack,
            onOpenEdit = onOpenEdit,
            onShowSnackBar = onShowSnackBar,
        )
    }
    composable<CalendarRouteModel.EventEdit> {
        EventEditRoute(
            padding = padding,
            onClose = onBack,
            onOpenColors = onOpenColors,
            onShowSnackBar = onShowSnackBar,
            memoTab = memoTab,
        )
    }
}
