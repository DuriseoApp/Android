package com.duriseo.android.feature.couple.inviteenter

import androidx.annotation.StringRes
import com.duriseo.android.domain.couple.InviteCodeInput

/**
 * SCR-COUPLE-03의 상태 (FR-COUPLE-02, FR-COUPLE-01 AC4).
 *
 * @property code 입력한 코드. 항상 대문자 영문·숫자 6자 이하로 [InviteCodeInput.clean]을 거친 값이다
 * @property errorRes 코드 아래에 보일 오류 문구. 지금은 문구가 정해진 `INVITE_EXPIRED` 하나뿐이다
 *   (나머지 6종은 문구·위치가 문서에 없어 v0.9 반영 대기, unit-3.md 확인 필요 3)
 * @property requesting 연결 요청 중. [연결하고 시작하기]가 비활성이다 (SCREENS 공통 "요청 중")
 */
data class InviteEnterUiState(
    val code: String = "",
    @StringRes val errorRes: Int? = null,
    val requesting: Boolean = false,
) {
    /** 6자리가 다 차야 켜진다 (FR-COUPLE-02 AC1) */
    val canConnect: Boolean get() = InviteCodeInput.isComplete(code) && !requesting
}

sealed interface InviteEnterAction {
    /** 코드 칸에 글자를 넣었다. [raw]는 입력한 그대로고 ViewModel이 [InviteCodeInput.clean]으로 거른다 */
    data class CodeChanged(val raw: String) : InviteEnterAction

    /** [붙여넣기]. [text]는 클립보드 첫 항목의 글자이고 비었으면 null (아무 일도 없다, 결정 11) */
    data class Pasted(val text: String?) : InviteEnterAction

    data object ConnectClicked : InviteEnterAction
}

sealed interface InviteEnterSideEffect {
    /** 연결됐다 → SCR-CAL-01 (연결 완료 시트 SCR-COUPLE-04는 v0.9 반영 대기) */
    data object NavigateToHome : InviteEnterSideEffect

    /** 이 화면의 스낵바(위에서 40)에 문구를 보여 준다 */
    data class ShowMessage(@StringRes val messageRes: Int) : InviteEnterSideEffect
}
