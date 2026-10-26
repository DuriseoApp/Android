package com.duriseo.android.feature.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.duriseo.android.core.designsystem.component.DuriseoSnackbarHost
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
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
) {
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

    MainScaffold(
        currentTab = navigator.currentTab,
        showBottomBar = navigator.showBottomBar(),
        snackbarHostState = snackbarHostState,
        onTabSelected = navigator::navigate,
    ) { padding ->
        MainNavHost(
            navigator = navigator,
            padding = padding,
            onShowSnackBar = onShowSnackBar,
        )
    }
}

/**
 * 탭이 있는 화면의 FAB(오른쪽 20, 바닥에서 100, 크기 56 = 탭 위로 20 떨어진 자리, design-system.md)와
 * 겹치지 않게 스낵바를 올리는 간격: FAB 높이 + 탭과 FAB 사이 `space-5` + 여유 `space-2`.
 */
private val SnackbarLiftAboveFab = DuriseoSize.Fab + DuriseoSpacing.Space5 + DuriseoSpacing.Space2

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
            // 스낵바는 하단 탭 위에 놓이고, 탭이 있는 화면에서는 FAB 위로 더 올린다
            DuriseoSnackbarHost(
                hostState = snackbarHostState,
                modifier = if (showBottomBar) Modifier.padding(bottom = SnackbarLiftAboveFab) else Modifier,
            )
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
            CalendarScreen(padding = padding)
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
