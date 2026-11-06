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
import com.duriseo.android.feature.calendar.edit.EventEditRoute
import java.time.LocalDate

fun NavController.navigateToCalendar(navOptions: NavOptions) {
    navigate(MainRouteModel.Calendar, navOptions)
}

/** 일정 상세 (SCR-CAL-02). 하단 탭이 없다 */
fun NavController.navigateToEventDetail(eventId: String) {
    navigate(CalendarRouteModel.EventDetail(eventId))
}

/** 새 일정 작성 (SCR-CAL-03). [date]는 캘린더에서 고른 날 */
fun NavController.navigateToEventCreate(date: LocalDate) {
    navigate(CalendarRouteModel.EventEdit(date = date.toString()))
}

/** 일정 수정 (SCR-CAL-03) */
fun NavController.navigateToEventEdit(eventId: String) {
    navigate(CalendarRouteModel.EventEdit(eventId = eventId))
}

/**
 * @param onOpenMenu 캘린더 앱바의 [전체 메뉴]
 * @param onOpenColors 작성 화면의 "색상 변경" 링크. 화면 모듈끼리는 서로 모르므로 앱 쪽 이동에 맡긴다
 */
fun NavGraphBuilder.calendarNavGraph(
    padding: PaddingValues,
    onOpenMenu: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenCreate: (LocalDate) -> Unit,
    onOpenEdit: (String) -> Unit,
    onOpenColors: () -> Unit,
    onBack: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    snackbarVisible: Boolean = false,
) {
    composable<MainRouteModel.Calendar> { entry ->
        CalendarRoute(
            lifecycleOwner = entry,
            padding = padding,
            onOpenMenu = onOpenMenu,
            onOpenEvent = onOpenEvent,
            onOpenCreate = onOpenCreate,
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
        )
    }
}
