package com.duriseo.android.feature.calendar.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.calendar.CalendarRoute

fun NavController.navigateToCalendar(navOptions: NavOptions) {
    navigate(MainRouteModel.Calendar, navOptions)
}

fun NavGraphBuilder.calendarNavGraph(
    padding: PaddingValues,
    onOpenMenu: () -> Unit,
) {
    composable<MainRouteModel.Calendar> {
        CalendarRoute(
            padding = padding,
            onOpenMenu = onOpenMenu,
        )
    }
}
