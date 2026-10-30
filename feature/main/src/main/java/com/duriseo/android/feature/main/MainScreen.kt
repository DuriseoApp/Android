package com.duriseo.android.feature.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.component.DuriseoSnackbarHost
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.feature.calendar.CalendarScreen
import com.duriseo.android.feature.main.component.MainBottomBar
import com.duriseo.android.feature.main.component.MainTab
import com.duriseo.android.feature.main.component.MemoTabPlaceholder
import com.duriseo.android.feature.main.navigation.MainNavHost
import com.duriseo.android.feature.main.navigation.MainNavigator
import com.duriseo.android.feature.main.navigation.rememberMainNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    navigator: MainNavigator = rememberMainNavigator(),
    viewModel: MainViewModel = hiltViewModel(),
) {
    val start by viewModel.start.collectAsState()
    val startDestination = navigator.startDestination(start)

    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val snackbarHostState = remember { SnackbarHostState() }

    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    // 문구는 @StringRes로 받는다 (오프라인 안내 등 문구는 strings.xml에 있다)
    val onShowSnackBar: (Int) -> Unit = { messageRes ->
        snackbarJob?.cancel()
        snackbarJob = scope.launch {
            snackbarHostState.showSnackbar(resources.getString(messageRes))
        }
    }

    // 세션이 서버에서 끊기면 시작 화면으로 간다 (FR-AUTH-02 AC3). 내비게이션 그래프가 생긴 뒤부터 받는다
    val ready = startDestination != null
    LaunchedEffect(viewModel, ready) {
        if (!ready) return@LaunchedEffect
        viewModel.effects.collect { effect ->
            when (effect) {
                is MainEffect.SessionEnded -> navigator.navigateToStart(sessionReplaced = effect.replaced)
            }
        }
    }

    MainScaffold(
        currentTab = navigator.currentTab,
        showBottomBar = navigator.showBottomBar(),
        snackbarHostState = snackbarHostState,
        onTabSelected = navigator::navigate,
    ) { padding ->
        // 토큰 저장소를 읽는 동안은 문구 없는 빈 화면이다 (FR-AUTH-01)
        if (startDestination != null) {
            MainNavHost(
                navigator = navigator,
                startDestination = startDestination,
                padding = padding,
                onShowSnackBar = onShowSnackBar,
            )
        }
    }
}

/** 화면 뼈대(스낵바 + 본문 + 하단 탭). 상태만 받는다. */
@Composable
internal fun MainScaffold(
    currentTab: MainTab?,
    showBottomBar: Boolean,
    snackbarHostState: SnackbarHostState,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = {
            // 탭 있는 화면의 스낵바 위치는 Docs에서 아직 정하지 않았다 (design-system.md "스낵바").
            // 정해질 때까지 Scaffold 기본 자리(하단 탭 바로 위)에 둔다. FAB가 생기는 단위 4 전에 정한다.
            DuriseoSnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            MainBottomBar(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                isVisible = showBottomBar,
            )
        },
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenCalendarTabPreview() {
    DuriseoTheme {
        MainScaffold(
            currentTab = MainTab.CALENDAR,
            showBottomBar = true,
            snackbarHostState = remember { SnackbarHostState() },
            onTabSelected = {},
        ) { padding ->
            CalendarScreen(padding = padding, onOpenMenu = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenMemoTabPreview() {
    DuriseoTheme {
        MainScaffold(
            currentTab = MainTab.MEMO,
            showBottomBar = true,
            snackbarHostState = remember { SnackbarHostState() },
            onTabSelected = {},
        ) { padding ->
            MemoTabPlaceholder(padding = padding)
        }
    }
}
