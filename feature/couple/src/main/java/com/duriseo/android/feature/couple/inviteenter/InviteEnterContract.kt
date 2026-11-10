package com.duriseo.android.feature.couple.inviteenter

import androidx.annotation.StringRes
import com.duriseo.android.domain.couple.InviteCodeInput

/**
 * SCR-COUPLE-03의 상태 (FR-COUPLE-02, FR-COUPLE-01 AC4).
 *
 * @property code 입력한 코드. 항상 대문자 영문·숫자 6자 이하로 [InviteCodeInput.clean]을 거친 값이다
 * @property error 코드 칸 아래에 한 줄로 보일 오류 (D-95, SCREENS.md SCR-COUPLE-03 "오류"). 없으면 null
 * @property requesting 연결 요청 중. [연결하고 시작하기]가 비활성이다 (SCREENS 공통 "요청 중")
 */
data class InviteEnterUiState(
    val code: String = "",
    val error: InviteEnterError? = null,
    val requesting: Boolean = false,
) {
    /** 연결 요청이 도는 동안 true. 로딩 덮개를 그린다 (D-115) */
    val loading: Boolean get() = requesting


    /** 6자리가 다 차야 켜진다 (FR-COUPLE-02 AC1). 입력 제한 안내가 있는 동안은 꺼져 있다 (FR-COUPLE-02 AC4) */
    val canConnect: Boolean
        get() = InviteCodeInput.isComplete(code) && !requesting && error !is InviteEnterError.RateLimited
}

/**
 * 코드 오류 6종 (API.md 4.4 12단계 대응표). 문구는 SCREENS.md SCR-COUPLE-03 표 그대로다.
 * 코드를 고치면 사라지지만 [RateLimited]는 제한이 풀릴 때까지 남는다 (리드 보충).
 */
sealed interface InviteEnterError {
    /** `INVITE_NOT_FOUND`: 없는 코드, 형식이 틀린 코드 포함 */
    data object NotFound : InviteEnterError

    /** `INVITE_EXPIRED`: 만료됐거나 재요청·연결로 바뀐 이전 코드 (D-47) */
    data object Expired : InviteEnterError

    /** `INVITE_USED` */
    data object Used : InviteEnterError

    /** `INVITE_SELF` */
    data object Self : InviteEnterError

    /** `INVITE_UNAVAILABLE`: 만든 사람이 다른 사람과 연결 중 (FR-COUPLE-02 AC3) */
    data object Unavailable : InviteEnterError

    /**
     * `INVITE_RATE_LIMITED`: 1시간 10회 실패 (D-83). [minutes]는 오류를 받은 순간 `retryAfterSeconds`를
     * 분 단위로 올림해 정한 값이고 화면에서 줄어들지 않는다.
     */
    data class RateLimited(val minutes: Int) : InviteEnterError
}

sealed interface InviteEnterAction {
    /** 코드 칸에 글자를 넣었다. [raw]는 입력한 그대로고 ViewModel이 [InviteCodeInput.clean]으로 거른다 */
    data class CodeChanged(val raw: String) : InviteEnterAction

    /** [붙여넣기]. [text]는 클립보드 첫 항목의 글자이고 비었으면 null (아무 일도 없다, 결정 11) */
    data class Pasted(val text: String?) : InviteEnterAction

    data object ConnectClicked : InviteEnterAction
}

sealed interface InviteEnterSideEffect {
    /** 연결됐다 → SCR-CAL-01 (그 위의 연결 완료 시트 SCR-COUPLE-04는 `feature:main`이 띄운다, D-48) */
    data object NavigateToHome : InviteEnterSideEffect

    /** 이 화면의 스낵바(위에서 40)에 문구를 보여 준다 */
    data class ShowMessage(@StringRes val messageRes: Int) : InviteEnterSideEffect
}
