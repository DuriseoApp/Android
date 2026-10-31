package com.duriseo.android.feature.settings.account

import androidx.annotation.StringRes
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.domain.me.SocialProvider
import java.time.LocalDate

/** 계정 화면이 보여 줄 내용 (SCREENS.md SCR-SET-01 "계정 · 게스트", "계정 · 회원") */
sealed interface AccountContent {
    /** 게스트: 배너, [Google로 로그인] [카카오로 로그인], 안내, 게스트 ID, [탈퇴하기]. 로그아웃은 없다 (FR-AUTH-05 AC2) */
    data class Guest(val guestId: String) : AccountContent

    /**
     * 회원: 로그인 카드, 로그인 방식, 가입일, [로그아웃], [탈퇴하기]. 이메일은 받지 않아 보여 주지 않는다 (NFR-PRIV-01).
     *
     * @property joinedDate `Me.createdAt`을 기기 시간대의 날짜로 바꾼 가입일 (FR-SET-01)
     */
    data class Member(
        val provider: SocialProvider?,
        val nickname: String,
        val joinedDate: LocalDate,
    ) : AccountContent
}

/** 탈퇴 확인 다이얼로그의 본문 종류 (FR-AUTH-06 AC1·AC2) */
sealed interface WithdrawMessage {
    /** 개인 공간(게스트 포함): "내 계정과 모든 일정, 기념일, 메모가 바로 삭제돼요." */
    data object Personal : WithdrawMessage

    /** 커플 공간: "먼저 [상대 이름]님과의 연결이 해제되고, 내 계정이 삭제돼요." */
    data class Couple(val partnerNickname: String) : WithdrawMessage
}

/** 화면 위에 뜨는 다이얼로그 (한 번에 하나) */
enum class AccountDialog {
    /** 이미 가입된 계정 ① (FR-AUTH-03 AC2) */
    AlreadyUsed,

    /** 그 계정으로 로그인할까요? ② (FR-AUTH-03 AC3) */
    SwitchConfirm,
    Logout,
    Withdraw,
}

/**
 * @property content 내 정보를 아직 못 받았으면 null (앱바만 그린다)
 * @property requesting 연동·switch·로그아웃·탈퇴 요청이 도는 중. 같은 화면의 시작 버튼이 모두 비활성이다 (D-88)
 * @property justLinked 연동 직후 "로그인했어요. 게스트 기록을 계정으로 옮겼어요." 안내를 보인다. 화면을 떠나면 사라진다
 */
data class AccountUiState(
    val content: AccountContent? = null,
    val withdrawMessage: WithdrawMessage = WithdrawMessage.Personal,
    val dialog: AccountDialog? = null,
    val requesting: Boolean = false,
    val justLinked: Boolean = false,
)

sealed interface AccountAction {
    data class LinkClicked(val provider: SocialProvider) : AccountAction

    /** 소셜 SDK가 끝났다 */
    data class SocialTokenReceived(
        val provider: SocialProvider,
        val result: SocialTokenResult,
    ) : AccountAction

    /** 다이얼로그 ①의 [그 계정으로 로그인] */
    data object ExistingLoginClicked : AccountAction

    /** 다이얼로그 ②의 [삭제하고 로그인] */
    data object SwitchConfirmed : AccountAction

    data object LogoutClicked : AccountAction
    data object LogoutConfirmed : AccountAction
    data object WithdrawClicked : AccountAction
    data object WithdrawConfirmed : AccountAction

    /** 뒤 막·뒤로 가기·[취소] */
    data object DialogDismissed : AccountAction
}

sealed interface AccountSideEffect {
    /** Route가 현재 Activity로 소셜 SDK를 열고, 결과를 [AccountAction.SocialTokenReceived]로 돌려준다 (ViewModel은 Activity·SDK를 들고 있지 않는다) */
    data class RequestSocialToken(val provider: SocialProvider) : AccountSideEffect

    data class ShowMessage(@StringRes val messageRes: Int) : AccountSideEffect

    /** 로그아웃·탈퇴가 끝났다 → SCR-START-01 */
    data object NavigateToStart : AccountSideEffect

    /** 그 계정으로 로그인(switch)이 끝났다 → 그 계정의 현재 상태(캘린더) */
    data object NavigateToHome : AccountSideEffect
}
