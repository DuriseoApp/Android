package com.duriseo.android.feature.settings.menu

import androidx.annotation.StringRes
import com.duriseo.android.domain.me.SocialProvider

/** 전체 메뉴의 계정 부분이 보여 줄 내용 (SCREENS.md SCR-SET-01, FR-SET-01) */
sealed interface SettingsAccount {
    /** 게스트: 맨 위 안내 배너 + [로그인해서 기록 지키기], 계정 행 값 "게스트 · 로그인하기", 로그아웃 없음 (FR-AUTH-04 AC1, FR-AUTH-05 AC2) */
    data object Guest : SettingsAccount

    /** 회원: 계정 행 값은 로그인 수단 ("Google 로그인" / "카카오 로그인"), 로그아웃 있음 */
    data class Member(val provider: SocialProvider?) : SettingsAccount
}

/**
 * 전체 메뉴(SCR-SET-01)의 상태. 단위 2는 계정 부분만 그린다 (프로필 카드·우리 색상·알림·연결 정보·정보 섹션은 다른 단위).
 *
 * @property account 내 정보를 아직 못 받았으면 null (화면은 앱바만 그린다)
 * @property colorKeys 꾸미기 › 우리 색상 행에 보일 현재 색 점의 팔레트 키. 나, 상대, 함께 순서이고 개인 공간은 나 하나다
 */
data class SettingsUiState(
    val account: SettingsAccount? = null,
    val colorKeys: List<String> = emptyList(),
    val logoutDialogVisible: Boolean = false,
    val requesting: Boolean = false,
) {
    /** 로그아웃은 회원에게만 보인다 (FR-AUTH-05 AC2) */
    val canLogout: Boolean get() = account is SettingsAccount.Member
}

sealed interface SettingsAction {
    data object LogoutClicked : SettingsAction
    data object LogoutConfirmed : SettingsAction
    data object DialogDismissed : SettingsAction
}

sealed interface SettingsSideEffect {
    /** 공통 스낵바 */
    data class ShowMessage(@StringRes val messageRes: Int) : SettingsSideEffect

    /** 로그아웃이 끝났다 → SCR-START-01 (백스택을 비운다) */
    data object NavigateToStart : SettingsSideEffect
}
