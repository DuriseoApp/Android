package com.duriseo.android.feature.settings.colors

import androidx.annotation.StringRes
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
 * 사용자가 고른 색은 [완료]를 누를 때까지 화면에만 반영되는 초안이다 (D-100).
 *
 * @property loaded 내 정보를 읽었다. 읽기 전에는 앱바만 그린다
 * @property isCouple 커플 공간이면 [나·상대·함께] 셋, 개인 공간이면 [나]만 (FR-CAL-04 AC5)
 * @property weekDays 미리보기 한 주 (일~토). 오늘이 든 주 (SCREENS.md SCR-SET-03, 확인 필요 12)
 * @property saving [완료]의 `PATCH /space/colors` 요청 중. 이 동안 색 고르기와 [완료]는 받지 않는다
 */
data class ColorsUiState(
    val loaded: Boolean = false,
    val isCouple: Boolean = false,
    val myColor: String = ColorDefaults.ME,
    val partnerColor: String = ColorDefaults.PARTNER,
    val togetherColor: String = ColorDefaults.TOGETHER,
    val target: ColorTarget = ColorTarget.Me,
    val today: LocalDate = LocalDate.of(1970, 1, 1),
    val weekDays: List<LocalDate> = emptyList(),
    val saving: Boolean = false,
) {
    /** 저장 요청이 도는 동안 true. 로딩 덮개를 그린다 (D-115) */
    val loading: Boolean get() = saving

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

    /** [완료]: 바뀐 색만 한 번에 저장하고 닫는다. 바뀐 것이 없으면 요청 없이 닫는다 (D-100) */
    data object DoneClicked : ColorsAction
}

sealed interface ColorsSideEffect {
    /** 저장했거나 바뀐 것이 없어 화면을 닫는다 */
    data object Close : ColorsSideEffect

    /** 실패·오프라인 안내 스낵바. 화면은 그대로 둔다 */
    data class ShowMessage(@StringRes val messageRes: Int) : ColorsSideEffect
}
