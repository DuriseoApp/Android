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
import com.duriseo.android.feature.calendar.navigation.navigateToEventCreate
import com.duriseo.android.feature.calendar.navigation.navigateToEventDetail
import com.duriseo.android.feature.calendar.navigation.navigateToEventEdit
import com.duriseo.android.feature.couple.navigation.navigateToConnect
import com.duriseo.android.feature.couple.navigation.navigateToInviteCreate
import com.duriseo.android.feature.couple.navigation.navigateToInviteEnter
import com.duriseo.android.feature.main.MainStart
import com.duriseo.android.feature.main.component.MainTab
import com.duriseo.android.feature.settings.navigation.navigateToAccount
import com.duriseo.android.feature.settings.navigation.navigateToColors
import com.duriseo.android.feature.settings.navigation.navigateToConnectionInfo
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

    /** 커플 연결(SCR-COUPLE-01)로 간다. 백스택을 비운다 (신규 회원 로그인 직후, 연결 해제 뒤 앱 초기화) */
    fun navigateToCoupleConnect() {
        navController.navigateToConnect(navOptions { clearBackStack() })
    }

    /**
     * 커플 연결(SCR-COUPLE-01)로 간다. 백스택을 그대로 둬서 뒤로 가기가 전체 메뉴로 돌아온다
     * (전체 메뉴의 [상대와 연결하기], 게스트의 연결 정보 행. D-96)
     */
    fun navigateToConnect() = navController.navigateToConnect()

    fun navigateToInviteCreate() = navController.navigateToInviteCreate()

    fun navigateToInviteEnter() = navController.navigateToInviteEnter()

    /** 지금 화면 아래에 돌아갈 화면이 있는지 (없으면 뒤로 가기 버튼을 그리지 않는 화면이 쓴다) */
    fun hasPreviousScreen(): Boolean = navController.previousBackStackEntry != null

    /** 일정 상세 (SCR-CAL-02) */
    fun navigateToEventDetail(eventId: String) = navController.navigateToEventDetail(eventId)

    /** 새 일정 작성 (SCR-CAL-03). [date]는 캘린더에서 고른 날 */
    fun navigateToEventCreate(date: java.time.LocalDate) = navController.navigateToEventCreate(date)

    /** 일정 수정 (SCR-CAL-03) */
    fun navigateToEventEdit(eventId: String) = navController.navigateToEventEdit(eventId)

    /** 전체 메뉴(SCR-SET-01). 진입은 캘린더 앱바 */
    fun navigateToSettings() = navController.navigateToSettings()

    /** [fromCouple]이면 연동을 마친 뒤 SCR-COUPLE-01로 돌아온다 (게스트의 [로그인하고 연결하기], FR-AUTH-03 AC4) */
    fun navigateToAccount(fromCouple: Boolean = false) = navController.navigateToAccount(fromCouple)

    fun navigateToColors() = navController.navigateToColors()

    fun navigateToConnectionInfo() = navController.navigateToConnectionInfo()

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
