package com.duriseo.android.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalDuriseoColors = staticCompositionLocalOf<DuriseoColors> {
    error("No DuriseoColors provided")
}
private val LocalDuriseoTypography = staticCompositionLocalOf<DuriseoTypography> {
    error("No DuriseoTypography provided")
}

object DuriseoTheme {
    val colors: DuriseoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDuriseoColors.current
    val typography: DuriseoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalDuriseoTypography.current
}

@Composable
fun ProvideDuriseoColors(
    colors: DuriseoColors,
    typography: DuriseoTypography,
    content: @Composable () -> Unit
) {
    val provideColors = remember { colors.copy() }.apply { update(colors) }
    CompositionLocalProvider(
        LocalDuriseoColors provides provideColors,
        LocalDuriseoTypography provides typography,
        content = content
    )
}

@Composable
fun DuriseoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val typography = typography()
    val colors = if (darkTheme) {
        DuriseoDarkColors()
    } else {
        DuriseoLightColors()
    }

    ProvideDuriseoColors(
        colors = colors,
        typography = typography
    ) {
        MaterialTheme(
            content = content
        )
    }
}