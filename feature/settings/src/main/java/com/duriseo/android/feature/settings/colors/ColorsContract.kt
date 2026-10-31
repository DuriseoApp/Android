package com.duriseo.android.feature.settings.colors

import java.time.LocalDate

/** 바꿀 대상 (SCR-SET-03 "바꿀 대상" [나 | 상대 | 함께]) */
enum class ColorTarget { Me, Partner, Together }

/** [기본 색으로 되돌리기]와 처음 값: 나 주황, 상대 파랑, 함께 보라 (보드 `ColorSettings.dc.html` `reset`, FR-CAL-04 AC3) */
object ColorDefaults {
    const val ME = "pick-orange"
    const val PARTNER = "pick-blue"
    const val TOGETHER = "pick-purple"
}

/**
 * SCR-SET-03의 상태 (FR-CAL-04). 색은 팔레트 키(`pick-*`)이고 Room의 내 정보에서 읽는다.
 * 사용자가 고른 색은 화면에만 반영되는 초안이다. **저장 시점·방식은 v0.9 반영 대기** (unit-3.md 확인 필요 10)라 서버로 보내지 않는다.
 *
 * @property loaded 내 정보를 읽었다. 읽기 전에는 앱바만 그린다
 * @property isCouple 커플 공간이면 [나·상대·함께] 셋, 개인 공간이면 [나]만 (FR-CAL-04 AC5)
 * @property weekDays 미리보기 한 주 (일~토). 오늘이 든 주 (unit-3.md 확인 필요 12 A)
 */
data class ColorsUiState(
    val loaded: Boolean = false,
    val isCouple: Boolean = false,
    val myColor: String = ColorDefaults.ME,
    val partnerColor: String = ColorDefaults.PARTNER,
    val togetherColor: String = ColorDefaults.TOGETHER,
    val target: ColorTarget = ColorTarget.Me,
    val today: LocalDate = LocalDate.EPOCH,
    val weekDays: List<LocalDate> = emptyList(),
) {
    fun colorKeyOf(target: ColorTarget): String = when (target) {
        ColorTarget.Me -> myColor
        ColorTarget.Partner -> partnerColor
        ColorTarget.Together -> togetherColor
    }

    /** 고를 수 있는 대상. 개인 공간은 [나]뿐이다 */
    val targets: List<ColorTarget>
        get() = if (isCouple) ColorTarget.entries else listOf(ColorTarget.Me)

    /** 두 대상이 같은 색이면 경고 배너 (FR-CAL-04 AC4). 개인 공간에는 없다 */
    val hasDuplicate: Boolean
        get() = isCouple && (myColor == partnerColor || myColor == togetherColor || partnerColor == togetherColor)
}

sealed interface ColorsAction {
    data class TargetSelected(val target: ColorTarget) : ColorsAction

    /** 팔레트의 색([key]는 `pick-*`)을 골랐다. 지금 고른 대상의 색이 바뀐다 */
    data class ColorPicked(val key: String) : ColorsAction

    data object ResetClicked : ColorsAction

    /**
     * [완료]. 저장 시점·방식이 v0.9에서 정해질 때까지 아무 일도 하지 않는다 (unit-3.md 확인 필요 10).
     * 이 화면이 만든 색 변경을 서버로 보내는 리포지토리 호출(`CoupleRepository.updateColors`)은 이미 있다.
     */
    data object DoneClicked : ColorsAction
}
