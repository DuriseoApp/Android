package com.duriseo.android.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.theme.DuriseoTheme

// SCR-SET-01 (전체 메뉴)의 자리표시. 진입은 캘린더 앱바(단위 4)이고 화면은 단위 2부터 만든다 (SCR-SET-01 계정 부분, docs/PLAN.md 단위 2).
// 하단 탭이 아니다 (D-30). 지금은 들어갈 길이 없고, 문구 없는 빈 화면이다.

@Composable
fun SettingsRoute(
    padding: PaddingValues,
    @Suppress("UNUSED_PARAMETER") viewModel: SettingsViewModel = hiltViewModel(),
) {
    SettingsScreen(padding = padding)
}

@Composable
fun SettingsScreen(
    padding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPlaceholderPreview() {
    DuriseoTheme {
        SettingsScreen(padding = PaddingValues())
    }
}
