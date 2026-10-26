package com.duriseo.android.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.duriseo.android.core.designsystem.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
    }

    @Test
    fun `FR-CAL-04 팔레트 색 이름은 strings의 pick_label 리소스고 값은 tokens 라벨이다`() {
        val expected = listOf(
            "orange" to "주황", "rose" to "로즈", "amber" to "앰버", "mustard" to "머스터드",
            "green" to "초록", "teal" to "청록", "blue" to "파랑", "purple" to "보라",
            "magenta" to "자주", "brown" to "브라운", "gray" to "회색", "ink" to "먹색",
        )
        val xml = File("src/main/res/values/strings.xml").readText(Charsets.UTF_8)

        expected.forEach { (name, label) ->
            assertTrue(xml.contains("<string name=\"pick_label_$name\">$label</string>"))
        }
        assertEquals(
            listOf(
                R.string.pick_label_orange, R.string.pick_label_rose, R.string.pick_label_amber,
                R.string.pick_label_mustard, R.string.pick_label_green, R.string.pick_label_teal,
                R.string.pick_label_blue, R.string.pick_label_purple, R.string.pick_label_magenta,
                R.string.pick_label_brown, R.string.pick_label_gray, R.string.pick_label_ink,
            ),
            DuriseoPickPalette.colors.map { it.labelRes },
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
