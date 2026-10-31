package com.duriseo.android.feature.couple.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.CoupleRouteModel
import com.duriseo.android.feature.couple.connect.ConnectRoute
import com.duriseo.android.feature.couple.invitecreate.InviteCreateRoute
import com.duriseo.android.feature.couple.inviteenter.InviteEnterRoute

fun NavController.navigateToConnect(navOptions: NavOptions? = null) {
    navigate(CoupleRouteModel.Connect, navOptions)
}

fun NavController.navigateToInviteCreate(navOptions: NavOptions? = null) {
    navigate(CoupleRouteModel.InviteCreate, navOptions)
}

fun NavController.navigateToInviteEnter(navOptions: NavOptions? = null) {
    navigate(CoupleRouteModel.InviteEnter, navOptions)
}

/**
 * 커플 연결 화면 셋 (SCR-COUPLE-01~03). 하단 탭이 없다. 어디로 갈지는 `feature:main`이 정한다.
 *
 * @param canGoBackFromConnect SCR-COUPLE-01에 앞 화면이 있는지. 로그인 직후·연결 해제 뒤처럼 없으면
 *   뒤로 가기 버튼을 그리지 않는다 (unit-3.md 확인 필요 4)
 * @param onBack 뒤로 가기
 * @param onInviteCreate [초대코드 만들기]
 * @param onInviteEnter [초대코드 입력하기]
 * @param onHome [혼자 시작하기]·[혼자 계속 쓰기], 연결됨 → SCR-CAL-01 (백스택을 비운다).
 *   연결 완료 시트(SCR-COUPLE-04)는 v0.9 반영 대기
 * @param onLoginAndConnect 게스트의 [로그인하고 연결하기] → 계정 화면
 * @param onLoggedOut "다른 계정으로 로그인"으로 로그아웃을 마쳤다 → SCR-START-01 (백스택을 비운다)
 */
fun NavGraphBuilder.coupleNavGraph(
    padding: PaddingValues,
    canGoBackFromConnect: () -> Boolean,
    onBack: () -> Unit,
    onInviteCreate: () -> Unit,
    onInviteEnter: () -> Unit,
    onHome: () -> Unit,
    onLoginAndConnect: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    composable<CoupleRouteModel.Connect> {
        ConnectRoute(
            canGoBack = canGoBackFromConnect(),
            padding = padding,
            onBack = onBack,
            onInviteCreate = onInviteCreate,
            onInviteEnter = onInviteEnter,
            onHome = onHome,
            onAccount = onLoginAndConnect,
            onLoggedOut = onLoggedOut,
        )
    }
    composable<CoupleRouteModel.InviteCreate> { entry ->
        InviteCreateRoute(
            lifecycleOwner = entry,
            padding = padding,
            onBack = onBack,
            onConnected = onHome,
        )
    }
    composable<CoupleRouteModel.InviteEnter> {
        InviteEnterRoute(
            padding = padding,
            onBack = onBack,
            onConnected = onHome,
        )
    }
}
