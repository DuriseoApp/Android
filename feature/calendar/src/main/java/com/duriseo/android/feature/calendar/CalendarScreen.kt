package com.duriseo.android.feature.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.theme.DuriseoTheme

// SCR-CAL-01 (월 캘린더)의 자리표시. 화면 구성은 단위 4에서 SCREENS.md대로 만든다.
// 문서에 없는 문구를 만들지 않기 위해 지금은 문구 없는 빈 화면이다.

@Composable
fun CalendarRoute(
    padding: PaddingValues,
    @Suppress("UNUSED_PARAMETER") viewModel: CalendarViewModel = hiltViewModel(),
) {
    CalendarScreen(padding = padding)
}

@Composable
fun CalendarScreen(
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
private fun CalendarScreenPlaceholderPreview() {
    DuriseoTheme {
        CalendarScreen(padding = PaddingValues())
    }
}
