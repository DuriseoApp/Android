package com.duriseo.android.feature.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.theme.DuriseoTheme

@Composable
fun MenuRoute(
    padding: PaddingValues,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    MenuScreen(
        padding = padding
    )
}

@Composable
fun MenuScreen(
    padding: PaddingValues
) {
    Column(
        modifier = Modifier.padding(padding)
    ) {
        Text(
            text = "Menu",
            color = DuriseoTheme.colors.primaryText,
            style = DuriseoTheme.typography.h1Bold
        )
    }
}

@Preview
@Composable
private fun MenuScreenPreview() {
    DuriseoTheme {
        MenuScreen(
            padding = PaddingValues()
        )
    }
}