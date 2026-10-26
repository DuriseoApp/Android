package com.duriseo.android.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonColorsTest {

    @Test
    fun `FR-CAL-04 아바타 글자색은 배경 상대휘도가 0_2보다 크면 ink-900이다`() {
        val amber = DuriseoPickPalette.colorOf("pick-amber")!!
        val mustard = DuriseoPickPalette.colorOf("pick-mustard")!!

        assertEquals(Color(0xFFF59F00), amber)
        assertEquals(Ink900, onPersonColor(amber))
        // design-system.md: 앰버·머스터드 위 글자는 ink-900
        assertEquals(Ink900, onPersonColor(mustard))
    }

    @Test
    fun `FR-CAL-04 아바타 글자색은 배경 상대휘도가 0_2 이하면 흰색이다`() {
        val blue = DuriseoPickPalette.colorOf("pick-blue")!!
        val ink = DuriseoPickPalette.colorOf("pick-ink")!!

        assertEquals(Color(0xFF2F6FDB), blue)
        assertEquals(OnBrand, onPersonColor(blue))
        assertEquals(OnBrand, onPersonColor(ink))
    }

    @Test
    fun `FR-CAL-04 우리 색상 팔레트는 tokens 순서대로 12색이다`() {
        val keys = DuriseoPickPalette.colors.map { it.key }

        assertEquals(
            listOf(
                "pick-orange", "pick-rose", "pick-amber", "pick-mustard", "pick-green", "pick-teal",
                "pick-blue", "pick-purple", "pick-magenta", "pick-brown", "pick-gray", "pick-ink",
            ),
            keys,
        )
        assertEquals(12, keys.size)
        assertEquals(
            listOf("주황", "로즈", "앰버", "머스터드", "초록", "청록", "파랑", "보라", "자주", "브라운", "회색", "먹색"),
            DuriseoPickPalette.colors.map { it.label },
        )
    }

    @Test
    fun `FR-CAL-04 사람 색 기본값은 person-me, person-partner, person-together다`() {
        assertEquals(Color(0xFFE0612F), DefaultPersonColors.me)
        assertEquals(Color(0xFF2F6FDB), DefaultPersonColors.partner)
        assertEquals(Color(0xFF6D4AE0), DefaultPersonColors.together)
        assertEquals(PersonColors(PersonMe, PersonPartner, PersonTogether), DefaultPersonColors)
    }
}
