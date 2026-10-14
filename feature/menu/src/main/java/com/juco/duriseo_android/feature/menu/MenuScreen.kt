package com.juco.duriseo_android.feature.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.juco.duriseo_android.core.designsystem.theme.JucoTheme

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
            color = JucoTheme.colors.primaryText,
            style = JucoTheme.typography.h1Bold
        )
    }
}

@Preview
@Composable
private fun MenuScreenPreview() {
    JucoTheme {
        MenuScreen(
            padding = PaddingValues()
        )
    }
}