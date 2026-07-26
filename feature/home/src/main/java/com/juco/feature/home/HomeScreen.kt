package com.juco.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.juco.core.designsystem.theme.JucoTheme

@Composable
fun HomeRoute(
    padding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    HomeScreen(
        padding = padding
    )
}

@Composable
fun HomeScreen(
    padding: PaddingValues
) {
    Column(
        modifier = Modifier.padding(padding)
    ) {
        Text(
            text = "Home",
            color = JucoTheme.colors.primaryText,
            style = JucoTheme.typography.h1Bold
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    JucoTheme {
        HomeScreen(
            padding = PaddingValues()
        )
    }
}