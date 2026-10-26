package com.duriseo.android.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * design-system.md "Android (Jetpack Compose) 매핑" 표대로 지정한다.
 * 표에서 "쓰지 않음"인 역할(tertiary 4개, inversePrimary, errorContainer·onErrorContainer)은 지정하지 않는다.
 * 그 역할을 쓰는 M3 컴포넌트는 화면에 두지 않고 `core:designsystem` 컴포넌트로 그린다.
 * 다크 모드는 design-system.md에 정의되기 전까지 만들지 않는다 (라이트만).
 */
internal val DuriseoLightColorScheme = lightColorScheme(
    primary = Brand500,
    onPrimary = OnBrand,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand700,
    secondary = Ink900,
    onSecondary = OnBrand,
    secondaryContainer = Brand100,
    onSecondaryContainer = Brand700,
    background = Surface0,
    onBackground = Ink900,
    surface = Surface0,
    onSurface = Ink900,
    surfaceVariant = Surface100,
    onSurfaceVariant = Ink600,
    surfaceContainerLowest = Surface0,
    surfaceContainerLow = Surface50,
    surfaceContainer = Surface100,
    surfaceContainerHigh = Surface0,
    surfaceContainerHighest = Line300,
    inverseSurface = Ink900,
    inverseOnSurface = OnBrand,
    outline = Line300,
    outlineVariant = Line200,
    error = Danger600,
    onError = OnBrand,
    scrim = Scrim,
)

/** `MaterialTheme`로 표현되지 않는 둘이서 전용 값. */
object DuriseoTheme {
    /** 나·상대·함께의 현재 색 (사용자가 바꿀 수 있음) */
    val personColors: PersonColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPersonColors.current
}

/**
 * 앱 테마. 색은 `MaterialTheme.colorScheme`, 글자는 `MaterialTheme.typography`, 모서리는
 * `MaterialTheme.shapes`, 사람 색은 [DuriseoTheme.personColors]로 읽는다.
 *
 * @param personColors 서버·DataStore의 커플 설정에서 읽은 값. 읽기 전에는 기본값.
 */
@Composable
fun DuriseoTheme(
    personColors: PersonColors = DefaultPersonColors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPersonColors provides personColors) {
        MaterialTheme(
            colorScheme = DuriseoLightColorScheme,
            typography = DuriseoTypography,
            shapes = DuriseoMaterialShapes,
            content = content,
        )
    }
}
