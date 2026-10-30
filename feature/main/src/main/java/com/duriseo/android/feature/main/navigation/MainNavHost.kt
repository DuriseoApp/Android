package com.duriseo.android.feature.main.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.auth.navigation.authNavGraph
import com.duriseo.android.feature.calendar.navigation.calendarNavGraph
import com.duriseo.android.feature.main.component.MemoTabPlaceholder
import com.duriseo.android.feature.settings.navigation.settingsNavGraph

/**
 * @param startDestination 앱을 켰을 때 첫 화면. 토큰이 있으면 캘린더, 없으면 시작 화면 (FR-AUTH-01)
 * @param onShowSnackBar 스낵바를 띄운다. 인자는 문구 리소스 ID(`@StringRes`)다.
 *   기능 화면이 오프라인 안내 같은 문구를 띄울 때 쓴다 (전체 메뉴·계정 화면).
 *   시작 화면은 위에서 40에 자기 스낵바를 따로 그린다 (SCREENS.md SCR-START-01).
 */
@Composable
fun MainNavHost(
    navigator: MainNavigator,
    startDestination: Any,
    padding: PaddingValues,
    onShowSnackBar: (Int) -> Unit,
) {
    NavHost(
        navController = navigator.navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        authNavGraph(
            padding = padding,
            // 신규 회원은 SCR-COUPLE-01, 가져올 기록이 남은 기존 회원은 SCR-COUPLE-04로 가야 하지만
            // 두 화면은 단위 3에서 만든다. 그때까지는 모두 캘린더로 보낸다 (unit-2.md 확인 필요 2)
            onLoggedIn = { navigator.navigateToHome() },
        )
        calendarNavGraph(
            padding = padding,
            // 단위 4의 캘린더 앱바가 생기기 전까지 임시 [전체 메뉴] 버튼 (unit-2.md 확인 필요 3)
            onOpenMenu = navigator::navigateToSettings,
        )
        // feature:memo(단위 6)가 생기면 그 모듈의 그래프로 바꾼다
        composable<MainRouteModel.Memo> {
            MemoTabPlaceholder(padding = padding)
        }
        // 하단 탭이 아니다. 진입은 캘린더 앱바(단위 4)
        settingsNavGraph(
            padding = padding,
            onClose = navigator::navigateUp,
            onOpenAccount = navigator::navigateToAccount,
            onBackFromAccount = navigator::navigateUp,
            onLoggedOut = { navigator.navigateToStart() },
            onSwitched = { navigator.navigateToHome() },
            onShowSnackBar = onShowSnackBar,
        )
    }
}
