package com.juco.duriseo_android.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalJucoColors = staticCompositionLocalOf<JucoColors> {
    error("No JucoColors provided")
}
private val LocalJucoTypography = staticCompositionLocalOf<JucoTypography> {
    error("No JucoTypography provided")
}

object JucoTheme {
    val colors: JucoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalJucoColors.current
    val typography: JucoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalJucoTypography.current
}

@Composable
fun ProvideJucoColors(
    colors: JucoColors,
    typography: JucoTypography,
    content: @Composable () -> Unit
) {
    val provideColors = remember { colors.copy() }.apply { update(colors) }
    CompositionLocalProvider(
        LocalJucoColors provides provideColors,
        LocalJucoTypography provides typography,
        content = content
    )
}

@Composable
fun JucoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val typography = typography()
    val colors = if (darkTheme) {
        JucoDarkColors()
    } else {
        JucoLightColors()
    }

    ProvideJucoColors(
        colors = colors,
        typography = typography
    ) {
        MaterialTheme(
            content = content
        )
    }
}