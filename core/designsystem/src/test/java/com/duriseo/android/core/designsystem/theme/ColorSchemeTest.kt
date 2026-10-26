package com.duriseo.android.core.designsystem.theme

import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Test

/** design-system.md "Android (Jetpack Compose) 매핑" 표 */
class ColorSchemeTest {
    private val scheme = DuriseoLightColorScheme

    @Test
    fun `매핑 표의 Material 3 역할은 토큰 색으로 지정한다`() {
        assertEquals(Brand500, scheme.primary)
        assertEquals(OnBrand, scheme.onPrimary)
        assertEquals(Brand100, scheme.primaryContainer)
        assertEquals(Brand700, scheme.onPrimaryContainer)
        assertEquals(Ink900, scheme.secondary)
        assertEquals(OnBrand, scheme.onSecondary)
        // 하단 탭 선택 알약·아이콘
        assertEquals(Brand100, scheme.secondaryContainer)
        assertEquals(Brand700, scheme.onSecondaryContainer)
        assertEquals(Surface0, scheme.background)
        assertEquals(Ink900, scheme.onBackground)
        assertEquals(Surface100, scheme.surfaceVariant)
        assertEquals(Surface0, scheme.surfaceContainerLowest)
        assertEquals(Surface50, scheme.surfaceContainerLow)
        assertEquals(Surface100, scheme.surfaceContainer)
        assertEquals(Surface0, scheme.surfaceContainerHigh)
        assertEquals(Line300, scheme.surfaceContainerHighest)
        // 스낵바
        assertEquals(Ink900, scheme.inverseSurface)
        assertEquals(OnBrand, scheme.inverseOnSurface)
        assertEquals(Scrim, scheme.scrim)
    }

    @Test
    fun `매핑 표에서 쓰지 않음인 역할은 지정하지 않는다`() {
        val defaults = lightColorScheme()

        assertEquals(defaults.tertiary, scheme.tertiary)
        assertEquals(defaults.onTertiary, scheme.onTertiary)
        assertEquals(defaults.tertiaryContainer, scheme.tertiaryContainer)
        assertEquals(defaults.onTertiaryContainer, scheme.onTertiaryContainer)
        assertEquals(defaults.inversePrimary, scheme.inversePrimary)
        assertEquals(defaults.errorContainer, scheme.errorContainer)
        assertEquals(defaults.onErrorContainer, scheme.onErrorContainer)
    }
}
