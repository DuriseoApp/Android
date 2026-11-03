package com.duriseo.android.feature.settings.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.core.common.navigation.SettingsRouteModel
import com.duriseo.android.feature.settings.account.AccountRoute
import com.duriseo.android.feature.settings.colors.ColorsRoute
import com.duriseo.android.feature.settings.connection.ConnectionInfoRoute
import com.duriseo.android.feature.settings.menu.SettingsRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) {
    navigate(MainRouteModel.Settings, navOptions)
}

fun NavController.navigateToAccount(fromCouple: Boolean = false, navOptions: NavOptions? = null) {
    navigate(SettingsRouteModel.Account(fromCouple), navOptions)
}

fun NavController.navigateToColors(navOptions: NavOptions? = null) {
    navigate(SettingsRouteModel.Colors, navOptions)
}

fun NavController.navigateToConnectionInfo(navOptions: NavOptions? = null) {
    navigate(SettingsRouteModel.ConnectionInfo, navOptions)
}

/**
 * 전체 메뉴(SCR-SET-01), 계정 화면, 우리 색상(SCR-SET-03), 연결 정보. 모두 하단 탭이 없다 (D-30).
 *
 * @param onClose 전체 메뉴의 닫기
 * @param onOpenAccount 계정 행 또는 [로그인해서 기록 지키기]
 * @param onBackFromAccount 계정 화면의 뒤로 가기
 * @param onOpenColors 꾸미기 › 우리 색상 행
 * @param onBackFromColors 우리 색상의 뒤로 가기. [완료]로 저장했을 때도 같다
 * @param onOpenConnect 프로필 카드의 [상대와 연결하기], 게스트의 연결 정보 행 → SCR-COUPLE-01 (뒤로 가기가 전체 메뉴로 돌아온다)
 * @param onOpenConnectionInfo 연결 정보 행 → 연결 정보 화면
 * @param onBackFromConnectionInfo 연결 정보의 뒤로 가기
 * @param onOpenInviteCreate 연결 정보(혼자)의 [초대코드 만들기] → SCR-COUPLE-02 (D-97)
 * @param onOpenInviteEnter 연결 정보(혼자)의 [받은 초대코드 입력하기] → SCR-COUPLE-03 (D-97)
 * @param onLoggedOut 로그아웃·탈퇴가 끝났다. SCR-START-01로 가고 백스택을 비운다
 * @param onLinkedFromCouple 커플 연결 화면에서 시작한 연동이 끝났다. SCR-COUPLE-01로 돌아간다 (FR-AUTH-03 AC4)
 * @param onSwitched 이미 가입된 계정으로 로그인(switch)이 끝났다. 캘린더로 가고 백스택을 비운다
 * @param onShowSnackBar 스낵바 문구(`@StringRes`)를 띄운다
 */
fun NavGraphBuilder.settingsNavGraph(
    padding: PaddingValues,
    onClose: () -> Unit,
    onOpenAccount: () -> Unit,
    onBackFromAccount: () -> Unit,
    onOpenColors: () -> Unit,
    onBackFromColors: () -> Unit,
    onOpenConnect: () -> Unit,
    onOpenConnectionInfo: () -> Unit,
    onBackFromConnectionInfo: () -> Unit,
    onOpenInviteCreate: () -> Unit,
    onOpenInviteEnter: () -> Unit,
    onLoggedOut: () -> Unit,
    onSwitched: () -> Unit,
    onLinkedFromCouple: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
) {
    composable<MainRouteModel.Settings> {
        SettingsRoute(
            padding = padding,
            onClose = onClose,
            onOpenAccount = onOpenAccount,
            onOpenColors = onOpenColors,
            onOpenConnect = onOpenConnect,
            onOpenConnectionInfo = onOpenConnectionInfo,
            onLoggedOut = onLoggedOut,
            onShowSnackBar = onShowSnackBar,
        )
    }
    composable<SettingsRouteModel.Colors> {
        ColorsRoute(
            padding = padding,
            onBack = onBackFromColors,
            onShowSnackBar = onShowSnackBar,
        )
    }
    composable<SettingsRouteModel.ConnectionInfo> {
        ConnectionInfoRoute(
            padding = padding,
            onBack = onBackFromConnectionInfo,
            onInviteCreate = onOpenInviteCreate,
            onInviteEnter = onOpenInviteEnter,
            onShowSnackBar = onShowSnackBar,
        )
    }
    composable<SettingsRouteModel.Account> {
        AccountRoute(
            padding = padding,
            onBack = onBackFromAccount,
            onLoggedOut = onLoggedOut,
            onSwitched = onSwitched,
            onLinkedFromCouple = onLinkedFromCouple,
            onShowSnackBar = onShowSnackBar,
        )
    }
}
