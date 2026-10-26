package com.duriseo.android.feature.main.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.calendar.navigation.calendarNavGraph
import com.duriseo.android.feature.main.component.MemoTabPlaceholder
import com.duriseo.android.feature.settings.navigation.settingsNavGraph

/**
 * @param onShowSnackBar 스낵바를 띄운다. 인자는 문구 리소스 ID(`@StringRes`)다.
 *   기능 화면이 오프라인 안내 같은 문구를 띄울 때 쓴다 (아직 쓰는 화면 없음).
 */
@Composable
fun MainNavHost(
    navigator: MainNavigator,
    padding: PaddingValues,
    @Suppress("UNUSED_PARAMETER") onShowSnackBar: (Int) -> Unit,
) {
    NavHost(
        navController = navigator.navController,
        startDestination = navigator.startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        calendarNavGraph(
            padding = padding,
        )
        // feature:memo(단위 6)가 생기면 그 모듈의 그래프로 바꾼다
        composable<MainRouteModel.Memo> {
            MemoTabPlaceholder(padding = padding)
        }
        // 하단 탭이 아니다. 진입은 캘린더 앱바(단위 4)
        settingsNavGraph(
            padding = padding,
        )
    }
}
