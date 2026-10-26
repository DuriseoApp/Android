package com.duriseo.android.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class TokenColorsTest {

    @Test
    fun `tokens scrim은 rgba 0_48을 ARGB 0x7A1C1A22로 옮긴다`() {
        // rgba(28, 26, 34, 0.48): 알파 0.48 x 255 = 122.4 -> 122 = 0x7A
        assertEquals(0x7A, (0.48 * 255).roundToInt())
        assertEquals(Color(0x7A1C1A22), Scrim)
        // RGB는 ink-900(28, 26, 34)과 같다
        assertEquals(Ink900.red, Scrim.red, 0f)
        assertEquals(Ink900.green, Scrim.green, 0f)
        assertEquals(Ink900.blue, Scrim.blue, 0f)
    }

    @Test
    fun `tokens 그림자 색은 ink-900 RGB에 토큰의 알파를 쓴다`() {
        // sRGB Color는 알파를 8비트(1/255)로 저장하므로 그 오차까지 허용한다
        val eightBit = 1f / 255f
        assertEquals(0.12f, DuriseoShadows.Sm.color.alpha, eightBit)
        assertEquals(0.18f, DuriseoShadows.Fab.color.alpha, eightBit)
        assertEquals(0.24f, DuriseoShadows.Dialog.color.alpha, eightBit)
        assertEquals(Ink900.red, DuriseoShadows.Fab.color.red, 0f)
    }
}
