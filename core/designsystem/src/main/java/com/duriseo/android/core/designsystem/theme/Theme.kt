package com.duriseo.android.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * design-system.md "Android (Jetpack Compose) 매핑" 표에 있는 14개 역할만 지정한다.
 * 표에 없는 역할(secondary·tertiary·background·surfaceContainerHigh/Highest·inverseSurface 등)은
 * M3 기본값으로 남는다. TODO(Docs PR): 매핑이 보충되면 추가.
 * 다크 모드는 design-system.md에 정의되기 전까지 만들지 않는다 (라이트만).
 */
private val DuriseoLightColorScheme = lightColorScheme(
    primary = Brand500,
    onPrimary = OnBrand,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand700,
    surface = Surface0,
    onSurface = Ink900,
    onSurfaceVariant = Ink600,
    surfaceContainerLow = Surface50,
    surfaceContainer = Surface100,
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
