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
import com.duriseo.android.feature.settings.menu.SettingsRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) {
    navigate(MainRouteModel.Settings, navOptions)
}

fun NavController.navigateToAccount(navOptions: NavOptions? = null) {
    navigate(SettingsRouteModel.Account, navOptions)
}

fun NavController.navigateToColors(navOptions: NavOptions? = null) {
    navigate(SettingsRouteModel.Colors, navOptions)
}

/**
 * 전체 메뉴(SCR-SET-01), 계정 화면, 우리 색상(SCR-SET-03). 둘 다 하단 탭이 없다 (D-30).
 *
 * @param onClose 전체 메뉴의 닫기
 * @param onOpenAccount 계정 행 또는 [로그인해서 기록 지키기]
 * @param onBackFromAccount 계정 화면의 뒤로 가기
 * @param onOpenColors 꾸미기 › 우리 색상 행
 * @param onBackFromColors 우리 색상의 뒤로 가기
 * @param onLoggedOut 로그아웃·탈퇴가 끝났다. SCR-START-01로 가고 백스택을 비운다
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
    onLoggedOut: () -> Unit,
    onSwitched: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
) {
    composable<MainRouteModel.Settings> {
        SettingsRoute(
            padding = padding,
            onClose = onClose,
            onOpenAccount = onOpenAccount,
            onOpenColors = onOpenColors,
            onLoggedOut = onLoggedOut,
            onShowSnackBar = onShowSnackBar,
        )
    }
    composable<SettingsRouteModel.Colors> {
        ColorsRoute(
            padding = padding,
            onBack = onBackFromColors,
        )
    }
    composable<SettingsRouteModel.Account> {
        AccountRoute(
            padding = padding,
            onBack = onBackFromAccount,
            onLoggedOut = onLoggedOut,
            onSwitched = onSwitched,
            onShowSnackBar = onShowSnackBar,
        )
    }
}
