package com.duriseo.android.feature.main.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.core.designsystem.R

/**
 * 하단 탭은 캘린더·메모 두 개뿐이다 (D-30, SCREENS.md 공통 "하단 탭").
 * 전체 메뉴(설정)는 탭이 아니라 캘린더 앱바에서 들어간다.
 * 탭을 늘리지 않는다. 다른 화면은 앱바·메뉴에서 들어간다.
 */
enum class MainTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val route: MainRouteModel,
) {
    CALENDAR(
        labelRes = R.string.tab_calendar,
        iconRes = R.drawable.ic_tab_calendar,
        route = MainRouteModel.Calendar,
    ),
    MEMO(
        labelRes = R.string.tab_memo,
        iconRes = R.drawable.ic_tab_memo,
        route = MainRouteModel.Memo,
    );

    companion object {
        /** 앱의 첫 화면은 캘린더 탭이다 (D-30) */
        val START: MainTab = CALENDAR

        /** 현재 화면이 속한 탭. 탭이 아닌 화면(설정 등)이면 null */
        fun fromDestination(destination: NavDestination?): MainTab? =
            entries.firstOrNull { destination?.hasRoute(it.route::class) == true }
    }
}
