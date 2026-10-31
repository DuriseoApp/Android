package com.duriseo.android.feature.couple.connect

import androidx.annotation.StringRes

/**
 * SCR-COUPLE-01의 상태 (FR-COUPLE-01, FR-AUTH-04 AC2).
 *
 * @property isGuest 게스트면 카드 대신 안내와 [로그인하고 연결하기]가 보이고 "다른 계정으로 로그인"이 없다
 * @property logoutDialogVisible "다른 계정으로 로그인"을 누르면 뜨는 로그아웃 확인 (FR-AUTH-05 AC3, D-80)
 * @property requesting 로그아웃 요청 중. 확인 다이얼로그 버튼이 비활성이다 (SCREENS 공통 "요청 중")
 */
data class ConnectUiState(
    val isGuest: Boolean = false,
    val logoutDialogVisible: Boolean = false,
    val requesting: Boolean = false,
)

sealed interface ConnectAction {
    data object MakeCodeClicked : ConnectAction

    data object EnterCodeClicked : ConnectAction

    /** [혼자 시작하기](회원), [혼자 계속 쓰기](게스트) */
    data object SoloClicked : ConnectAction

    /** 게스트의 [로그인하고 연결하기] */
    data object GuestLoginClicked : ConnectAction

    /** 회원의 "다른 계정으로 로그인" */
    data object OtherAccountClicked : ConnectAction

    data object LogoutDismissed : ConnectAction

    data object LogoutConfirmed : ConnectAction
}

sealed interface ConnectSideEffect {
    data object NavigateToInviteCreate : ConnectSideEffect

    data object NavigateToInviteEnter : ConnectSideEffect

    /** SCR-CAL-01 */
    data object NavigateToHome : ConnectSideEffect

    /** 게스트의 [로그인하고 연결하기] → 계정 화면 (SCR-SET-01 하위 "계정") */
    data object NavigateToAccount : ConnectSideEffect

    /** 로그아웃을 마쳤다 → SCR-START-01 */
    data object NavigateToStart : ConnectSideEffect

    data class ShowMessage(@StringRes val messageRes: Int) : ConnectSideEffect
}
