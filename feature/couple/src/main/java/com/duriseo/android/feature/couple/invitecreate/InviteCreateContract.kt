package com.duriseo.android.feature.couple.invitecreate

import androidx.annotation.StringRes

/**
 * SCR-COUPLE-02의 상태 (FR-COUPLE-01, FR-COUPLE-02).
 *
 * @property code 6자리 대문자 코드. 아직 받지 못했으면 null (칸이 빈 채로 보인다. 첫 진입 모양은 문서에 없어 v0.9 반영 대기)
 * @property remainingSeconds 코드가 만료되기까지 남은 초 (기기 시계 기준, 진행 기록 결정 4)
 * @property reissueWaitSeconds 0보다 크면 [코드 새로 만들기]가 꺼지고 "N초 뒤에 다시 만들 수 있어요"가 보인다
 * @property renewedNotice 만료돼 앱이 새 코드를 받았다는 안내 (FR-COUPLE-01 AC5)
 * @property copied [코드 복사]를 눌렀다 (버튼 글자가 "복사됨"으로 바뀜. 새 코드를 받으면 원래대로)
 * @property requesting [코드 새로 만들기] 요청 중
 */
data class InviteCreateUiState(
    val code: String? = null,
    val remainingSeconds: Int = 0,
    val reissueWaitSeconds: Int = 0,
    val renewedNotice: Boolean = false,
    val copied: Boolean = false,
    val requesting: Boolean = false,
) {
    val canReissue: Boolean get() = code != null && reissueWaitSeconds == 0 && !requesting
}

sealed interface InviteCreateAction {
    /** 화면이 보이기 시작했다 (`STARTED`). 5초 확인과 코드 만료 확인을 시작한다 */
    data object Shown : InviteCreateAction

    /** 화면이 보이지 않게 됐다. 확인과 새 코드 받기를 멈춘다 (D-42) */
    data object Hidden : InviteCreateAction

    data object CopyClicked : InviteCreateAction

    data object ShareClicked : InviteCreateAction

    data object ReissueClicked : InviteCreateAction

    /** [연결 확인하기] */
    data object CheckClicked : InviteCreateAction
}

sealed interface InviteCreateSideEffect {
    /** Route가 클립보드에 [code]를 넣는다 */
    data class CopyCode(val code: String) : InviteCreateSideEffect

    /** Route가 공유 시트(`ACTION_SEND`, `text/plain`)에 [code]를 넘긴다 (코드만, unit-3.md 확인 필요 13) */
    data class ShareCode(val code: String) : InviteCreateSideEffect

    /** 연결됐다 → SCR-CAL-01 (연결 완료 시트는 v0.9 반영 대기) */
    data object NavigateToHome : InviteCreateSideEffect

    /** 화면 아래(버튼 위 112)의 스낵바 */
    data class ShowMessage(@StringRes val messageRes: Int) : InviteCreateSideEffect
}
