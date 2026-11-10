package com.duriseo.android.feature.main.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.anniversary.navigation.anniversaryNavGraph
import com.duriseo.android.feature.auth.navigation.authNavGraph
import com.duriseo.android.feature.auth.start.AfterLogin
import com.duriseo.android.feature.calendar.navigation.calendarNavGraph
import com.duriseo.android.feature.couple.navigation.coupleNavGraph
import com.duriseo.android.feature.main.component.MemoTabPlaceholder
import com.duriseo.android.feature.settings.navigation.settingsNavGraph

/**
 * @param startDestination 앱을 켰을 때 첫 화면. 토큰이 있으면 캘린더, 없으면 시작 화면 (FR-AUTH-01)
 * @param snackbarVisible 스낵바가 떠 있는지. 캘린더 FAB가 스낵바 위로 올라가는 데 쓴다 (SCR-CAL-01)
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
    snackbarVisible: Boolean = false,
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
            onLoggedIn = { destination ->
                when (destination) {
                    AfterLogin.Home -> navigator.navigateToHome()
                    // 신규 회원은 SCR-COUPLE-01 (FR-AUTH-02 AC1, D-31). 앞 화면이 없어 뒤로 가기 버튼이 없다
                    AfterLogin.CoupleConnect -> navigator.navigateToCoupleConnect()
                    // 가져올 기록이 남은 기존 회원은 캘린더로 간다. 그 위의 연결 완료 시트(SCR-COUPLE-04)는
                    // `MainScreen`이 `importPending`을 보고 띄운다 (FR-COUPLE-03 AC4, FR-AUTH-02 AC1)
                    AfterLogin.ImportPending -> navigator.navigateToHome()
                }
            },
        )
        coupleNavGraph(
            padding = padding,
            canGoBackFromConnect = navigator::hasPreviousScreen,
            onBack = navigator::navigateUp,
            onInviteCreate = navigator::navigateToInviteCreate,
            onInviteEnter = navigator::navigateToInviteEnter,
            // 연결됨, 혼자 시작하기: 캘린더로 가고 백스택을 비운다
            onHome = { navigator.navigateToHome() },
            onLoginAndConnect = { navigator.navigateToAccount(fromCouple = true) },
            onLoggedOut = { navigator.navigateToStart() },
        )
        calendarNavGraph(
            padding = padding,
            onOpenMenu = navigator::navigateToSettings,
            onOpenEvent = navigator::navigateToEventDetail,
            onOpenCreate = navigator::navigateToEventCreate,
            onOpenEdit = navigator::navigateToEventEdit,
            onOpenAnniversaries = navigator::navigateToAnniversaries,
            onOpenColors = navigator::navigateToColors,
            onBack = navigator::navigateUp,
            onShowSnackBar = onShowSnackBar,
            snackbarVisible = snackbarVisible,
        )
        // 기념일 (SCR-ANNIV-01·02). 캘린더(앱바·기념일 카드)에서 `navigateToAnniversaries`로 들어간다
        anniversaryNavGraph(
            padding = padding,
            onBack = navigator::navigateUp,
            onOpenCreate = navigator::navigateToAnniversaryCreate,
            onOpenEdit = navigator::navigateToAnniversaryEdit,
            onShowSnackBar = onShowSnackBar,
        )
        // feature:memo(단위 6)가 생기면 그 모듈의 그래프로 바꾼다
        composable<MainRouteModel.Memo> {
            MemoTabPlaceholder(padding = padding)
        }
        // 하단 탭이 아니다. 진입은 캘린더 앱바
        settingsNavGraph(
            padding = padding,
            onClose = navigator::navigateUp,
            onOpenAccount = { navigator.navigateToAccount() },
            onBackFromAccount = navigator::navigateUp,
            onOpenColors = navigator::navigateToColors,
            onBackFromColors = navigator::navigateUp,
            onOpenConnect = navigator::navigateToConnect,
            onOpenConnectionInfo = navigator::navigateToConnectionInfo,
            onBackFromConnectionInfo = navigator::navigateUp,
            onOpenInviteCreate = navigator::navigateToInviteCreate,
            onOpenInviteEnter = navigator::navigateToInviteEnter,
            onLoggedOut = { navigator.navigateToStart() },
            onSwitched = { navigator.navigateToHome() },
            // 커플 연결에서 시작한 연동이 끝남: 아래에 있는 SCR-COUPLE-01(이제 회원 모양)로 돌아간다
            onLinkedFromCouple = navigator::navigateUp,
            onShowSnackBar = onShowSnackBar,
        )
    }
}
