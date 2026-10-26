package com.duriseo.android.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.calendar.navigation.navigateToCalendar
import com.duriseo.android.feature.main.component.MainTab

class MainNavigator(
    val navController: NavHostController,
) {
    /** 앱의 첫 화면은 캘린더다 (D-30) */
    val startDestination: MainRouteModel = MainTab.START.route

    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentTab: MainTab?
        @Composable get() = MainTab.fromDestination(currentDestination)

    /** 탭 전환. 시작 화면까지 되돌려 상태를 저장하고, 다시 누르면 복원한다. */
    fun navigate(tab: MainTab) {
        val navOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        when (tab) {
            MainTab.CALENDAR -> navController.navigateToCalendar(navOptions)
            MainTab.MEMO -> navController.navigate(MainRouteModel.Memo, navOptions)
        }
    }

    /** 하단 탭은 탭 화면(캘린더·메모)에서만 보인다 */
    @Composable
    fun showBottomBar(): Boolean = currentTab != null
}

@Composable
fun rememberMainNavigator(
    navController: NavHostController = rememberNavController(),
): MainNavigator = remember(navController) {
    MainNavigator(navController)
}
