package com.duriseo.android.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
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
import com.duriseo.android.core.designsystem.component.DuriseoTopSnackbarHost
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.feature.calendar.CalendarScreen
import com.duriseo.android.feature.calendar.CalendarUiState
import com.duriseo.android.feature.couple.linkedsheet.LinkedSheetRoute
import com.duriseo.android.feature.main.component.MainBottomBar
import com.duriseo.android.feature.main.component.MainTab
import com.duriseo.android.feature.main.component.MemoTabPlaceholder
import com.duriseo.android.feature.main.navigation.MainNavHost
import com.duriseo.android.feature.main.navigation.MainNavigator
import com.duriseo.android.feature.main.navigation.rememberMainNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 스낵바와 하단 탭(80) 사이 간격 16 (SCREENS.md 42·85행, `Calendar.dc.html` 148행: 바닥에서 96 = 탭 80 + 16) */
private val SnackbarAboveTab = DuriseoSpacing.Space4

@Composable
fun MainScreen(
    navigator: MainNavigator = rememberMainNavigator(),
    viewModel: MainViewModel = hiltViewModel(),
) {
    val start by viewModel.start.collectAsState()
    val linkedSheetNeeded by viewModel.linkedSheetNeeded.collectAsState()
    val personColors by viewModel.personColors.collectAsState()
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
                // 연결 해제를 알았다: 앱 초기화가 끝났으니 SCR-COUPLE-01 하나로 다시 만든다 (D-37, D-103)
                MainEffect.SpaceUnlinked -> navigator.navigateToCoupleConnect()
            }
        }
    }

    val currentTab = navigator.currentTab
    // 일정 색 등 사람 색은 내 정보의 색 키에서 온다 (FR-CAL-04 AC1)
    DuriseoTheme(personColors = personColors) {
        Box {
            MainScaffold(
                currentTab = currentTab,
                showBottomBar = navigator.showBottomBar(),
                snackbarHostState = snackbarHostState,
                onTabSelected = navigator::navigate,
                snackbarAtTop = navigator.snackbarAtTop(),
            ) { padding ->
                // 토큰 저장소를 읽는 동안은 문구 없는 빈 화면이다 (FR-AUTH-01)
                if (startDestination != null) {
                    MainNavHost(
                        navigator = navigator,
                        snackbarVisible = snackbarHostState.currentSnackbarData != null,
                        startDestination = startDestination,
                        padding = padding,
                        onShowSnackBar = onShowSnackBar,
                    )
                }
            }
            // 연결 완료 시트(SCR-COUPLE-04)는 캘린더(SCR-CAL-01) 위에 뜬다 (D-48, D-49). 새 내비게이션 경로가 아니다
            if (linkedSheetNeeded && currentTab == MainTab.CALENDAR) {
                LinkedSheetRoute()
            }
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
    snackbarAtTop: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier = modifier) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            snackbarHost = {
                // 위에서 띄우는 화면(SCR-CAL-02·03)은 아래 Box 위층에서 그린다. 나머지는 Scaffold 기본 자리.
                // 하단 탭이 있는 화면(SCR-CAL-01)은 탭 위로 16 띄운다: 바닥에서 96 (SCREENS.md 42·85행)
                if (!snackbarAtTop) {
                    DuriseoSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = if (showBottomBar) Modifier.padding(bottom = SnackbarAboveTab) else Modifier,
                    )
                }
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
        // 하단 탭이 없는 SCR-CAL-02·03의 스낵바는 위에서 40 (SCREENS.md 112·142행, D-88)
        if (snackbarAtTop) {
            DuriseoTopSnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
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
            CalendarScreen(state = CalendarUiState(), padding = padding, onOpenMenu = {}, onAction = {})
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
