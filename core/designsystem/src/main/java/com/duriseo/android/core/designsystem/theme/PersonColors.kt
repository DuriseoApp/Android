package com.duriseo.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * 나·상대·함께의 색. `ColorScheme`에 넣지 않고 [LocalPersonColors]로 제공한다 (design-system.md "Android 매핑").
 *
 * 사용자가 우리 색상에서 바꿀 수 있는 값이라 코드에 고정하지 않는다. 값은 서버·DataStore의
 * 커플 설정에서 읽어 [DuriseoTheme]에 넣는다 (단위 3).
 * 색만으로 뜻을 전하지 않는다. 항상 "나·상대·함께" 라벨이나 아바타 글자를 함께 둔다 (FR-CAL-04).
 */
@Immutable
data class PersonColors(
    val me: Color,
    val partner: Color,
    val together: Color,
)

/** 설정을 읽기 전의 기본값 = tokens.json `person-me`, `person-partner`, `person-together`. */
val DefaultPersonColors = PersonColors(
    me = PersonMe,
    partner = PersonPartner,
    together = PersonTogether,
)

val LocalPersonColors = staticCompositionLocalOf { DefaultPersonColors }

/** 우리 색상 팔레트의 한 칸. [key]는 서버 팔레트 키 그대로다 (ERD `space_members.color`). */
@Immutable
data class PickColor(
    val key: String,
    val label: String,
    val color: Color,
)

/**
 * 우리 색상에서 고를 수 있는 12색 (tokens.json `color.pick`). **팔레트에 표시되는 순서 그대로**다.
 * 서버가 이 밖의 키를 보내면 어떻게 그릴지는 아직 정해지지 않았다 (확인 필요 12, 단위 3 전에 정함).
 */
object DuriseoPickPalette {
    val colors: List<PickColor> = listOf(
        PickColor("pick-orange", "주황", Color(0xFFE0612F)),
        PickColor("pick-rose", "로즈", Color(0xFFD6336C)),
        PickColor("pick-amber", "앰버", Color(0xFFF59F00)),
        PickColor("pick-mustard", "머스터드", Color(0xFFB8860B)),
        PickColor("pick-green", "초록", Color(0xFF2F9E44)),
        PickColor("pick-teal", "청록", Color(0xFF0C8599)),
        PickColor("pick-blue", "파랑", Color(0xFF2F6FDB)),
        PickColor("pick-purple", "보라", Color(0xFF6D4AE0)),
        PickColor("pick-magenta", "자주", Color(0xFFAE3EC9)),
        PickColor("pick-brown", "브라운", Color(0xFF8D6E63)),
        PickColor("pick-gray", "회색", Color(0xFF868E96)),
        PickColor("pick-ink", "먹색", Color(0xFF1C1A22)),
    )

    /** 키 → 색. 모르는 키면 null. */
    fun colorOf(key: String): Color? = colors.firstOrNull { it.key == key }?.color
}

/** 이 값보다 상대휘도가 크면 사람 색 위 글자를 [Ink900]으로 쓴다 (design-system.md "사람 색 규칙"). */
private const val LIGHT_BACKGROUND_LUMINANCE = 0.2f

/**
 * 사람 색 배경 위 글자색(아바타 "나/너"). WCAG 상대휘도가 0.2보다 크면 [Ink900], 아니면 흰색([OnBrand]).
 * (design-system.md "사람 색 규칙", SCREENS.md 공통 "아바타 글자색", FR-CAL-04)
 */
fun onPersonColor(background: Color): Color =
    if (relativeLuminance(background) > LIGHT_BACKGROUND_LUMINANCE) Ink900 else OnBrand

/** WCAG 2.x 상대휘도 (sRGB 선형화 후 0.2126 R + 0.7152 G + 0.0722 B). */
internal fun relativeLuminance(color: Color): Float {
    fun linear(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return (0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)).toFloat()
}
