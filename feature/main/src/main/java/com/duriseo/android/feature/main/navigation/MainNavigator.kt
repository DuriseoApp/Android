package com.duriseo.android.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.NavOptionsBuilder
import com.duriseo.android.core.common.navigation.AuthRouteModel
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.calendar.navigation.navigateToCalendar
import com.duriseo.android.feature.main.MainStart
import com.duriseo.android.feature.main.component.MainTab
import com.duriseo.android.feature.settings.navigation.navigateToAccount
import com.duriseo.android.feature.settings.navigation.navigateToSettings

class MainNavigator(
    val navController: NavHostController,
) {
    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentTab: MainTab?
        @Composable get() = MainTab.fromDestination(currentDestination)

    /**
     * 탭 전환. 첫 탭(캘린더)까지 되돌려 상태를 저장하고, 다시 누르면 복원한다.
     * 그래프의 시작 화면은 로그인 상태에 따라 시작 화면일 수 있어서, 되돌릴 기준을 캘린더 경로로 직접 정한다.
     */
    fun navigate(tab: MainTab) {
        val navOptions = navOptions {
            popUpTo<MainRouteModel.Calendar> {
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

    /** 앱을 켰을 때 갈 곳 (FR-AUTH-01 AC2·AC3): 토큰이 있으면 캘린더, 없으면 시작 화면 */
    fun startDestination(start: MainStart): Any? = when (start) {
        MainStart.Loading -> null
        MainStart.Home -> MainTab.START.route
        MainStart.Start -> AuthRouteModel.Start()
    }

    /** 시작 화면으로 간다. 뒤로 가기로 돌아올 화면이 없도록 백스택을 모두 비운다 (로그아웃·탈퇴·세션 끊김) */
    fun navigateToStart(sessionReplaced: Boolean = false) {
        navController.navigate(AuthRouteModel.Start(sessionReplaced)) { clearBackStack() }
    }

    /** 캘린더로 간다. 백스택을 모두 비운다 (로그인 뒤, 그 계정으로 로그인 뒤) */
    fun navigateToHome() {
        navController.navigate(MainTab.START.route) { clearBackStack() }
    }

    /** 전체 메뉴(SCR-SET-01). 진입은 캘린더 앱바 (단위 4 전까지는 임시 버튼) */
    fun navigateToSettings() = navController.navigateToSettings()

    fun navigateToAccount() = navController.navigateToAccount()

    fun navigateUp() {
        navController.popBackStack()
    }

    private fun NavOptionsBuilder.clearBackStack() {
        popUpTo(navController.graph.id) { inclusive = true }
        launchSingleTop = true
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
