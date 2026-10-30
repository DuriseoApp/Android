package com.duriseo.android.feature.auth.start

import androidx.annotation.StringRes
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.domain.me.SocialProvider

/**
 * 로그인에 성공한 뒤 갈 곳 (SCREENS.md SCR-START-01 "동작"). 실제 화면 이동은 `feature:main`이 정한다.
 */
enum class AfterLogin {
    /** 게스트, 또는 기존 회원 → 캘린더 (그 계정의 현재 상태) */
    Home,

    /** 신규 소셜 회원 → SCR-COUPLE-01 (FR-AUTH-02 AC1, D-31). 화면은 단위 3이라 그때까지 `feature:main`이 캘린더로 보낸다 */
    CoupleConnect,

    /** 기존 회원인데 가져올 기록이 남은 경우(`importPending`) → SCR-COUPLE-04. 단위 3 전까지 캘린더 */
    ImportPending,
}

/** SCR-START-01의 상태. 로그인 요청이 도는 동안은 시작 버튼 3개가 모두 비활성이다 (SCREENS.md 공통 "요청 중", D-88). */
data class StartUiState(
    val requesting: Boolean = false,
)

sealed interface StartAction {
    data object GuestClicked : StartAction

    data class SocialClicked(val provider: SocialProvider) : StartAction

    /** 소셜 SDK가 끝났다. 성공이면 서버에 보내고, 취소면 아무 안내 없이 머문다 (D-88) */
    data class SocialTokenReceived(
        val provider: SocialProvider,
        val result: SocialTokenResult,
    ) : StartAction

    /** 다른 기기 로그인으로 로그아웃돼 이 화면이 열렸다. 안내 스낵바를 한 번 보여 준다 (D-51) */
    data object SessionReplacedArrived : StartAction
}

sealed interface StartSideEffect {
    /** 화면이 Activity로 소셜 SDK를 연다 (Activity는 ViewModel이 들고 있지 않는다) */
    data class RequestSocialToken(val provider: SocialProvider) : StartSideEffect

    /** 이 화면의 스낵바(위에서 40)에 문구를 보여 준다 */
    data class ShowMessage(@StringRes val messageRes: Int) : StartSideEffect

    data class LoggedIn(val destination: AfterLogin) : StartSideEffect
}
