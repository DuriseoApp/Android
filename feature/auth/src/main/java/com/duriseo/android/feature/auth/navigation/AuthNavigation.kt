package com.duriseo.android.feature.auth.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.duriseo.android.core.common.navigation.AuthRouteModel
import com.duriseo.android.feature.auth.start.AfterLogin
import com.duriseo.android.feature.auth.start.StartRoute

/**
 * 시작 화면 (SCR-START-01). 하단 탭이 없는 화면이다.
 *
 * @param onLoggedIn 로그인에 성공했다. 어디로 갈지는 `feature:main`이 정한다 (백스택을 비우고 이동)
 */
fun NavGraphBuilder.authNavGraph(
    padding: PaddingValues,
    onLoggedIn: (AfterLogin) -> Unit,
) {
    composable<AuthRouteModel.Start> { entry ->
        val route = entry.toRoute<AuthRouteModel.Start>()
        StartRoute(
            sessionReplaced = route.sessionReplaced,
            padding = padding,
            onLoggedIn = onLoggedIn,
        )
    }
}
