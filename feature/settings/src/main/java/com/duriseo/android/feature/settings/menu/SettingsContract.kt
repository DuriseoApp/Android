package com.duriseo.android.feature.settings.menu

import androidx.annotation.StringRes
import com.duriseo.android.domain.couple.StartDateRules
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.me.SpaceType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 전체 메뉴의 계정 부분이 보여 줄 내용 (SCREENS.md SCR-SET-01, FR-SET-01) */
sealed interface SettingsAccount {
    /** 게스트: 맨 위 안내 배너 + [로그인해서 기록 지키기], 계정 행 값 "게스트 · 로그인하기", 로그아웃 없음 (FR-AUTH-04 AC1, FR-AUTH-05 AC2) */
    data object Guest : SettingsAccount

    /** 회원: 계정 행 값은 로그인 수단 ("Google 로그인" / "카카오 로그인"), 로그아웃 있음 */
    data class Member(val provider: SocialProvider?) : SettingsAccount
}

/** 프로필 카드·연결 정보에 보이는 한 사람. [colorKey]는 팔레트 키 (모르는 키는 아바타가 기본 색으로 그린다) */
data class SettingsPerson(val name: String, val colorKey: String)

/**
 * 프로필 카드와 연결 정보 화면이 쓰는 공간 요약 (SCREENS.md SCR-SET-01 "프로필 카드", FR-SET-01).
 * 개인 공간이거나 게스트면 [Alone], 커플이면 [Couple]이다.
 */
sealed interface SettingsSpace {
    data class Alone(val me: SettingsPerson) : SettingsSpace

    /**
     * @property togetherColorKey 함께 색 팔레트 키. 없거나 모르는 키면 기본 함께 색으로 그린다
     * @property startDate 처음 만난 날. 없으면 null이고 D+N과 "부터 함께하고 있어요" 줄을 숨긴다 (D-99)
     * @property dayNumber 함께한 지 D+N의 N (처음 만난 날이 1일). [startDate]가 없으면 null
     * @property linkedDate 연결한 날: `linkedAt`을 기기 시간대 날짜로 바꾼 값 (D-94). 서버가 안 주면 null
     */
    data class Couple(
        val me: SettingsPerson,
        val partner: SettingsPerson,
        val togetherColorKey: String,
        val startDate: LocalDate?,
        val dayNumber: Long?,
        val linkedDate: LocalDate?,
    ) : SettingsSpace
}

/** 내 정보에서 공간 요약을 만든다. [today]는 기기 시계의 오늘, [zone]은 기기 시간대 */
internal fun Me.toSettingsSpace(today: LocalDate, zone: ZoneId): SettingsSpace {
    val myself = SettingsPerson(nickname, space.myColor)
    val partner = space.partner
    if (space.type != SpaceType.COUPLE || partner == null) return SettingsSpace.Alone(myself)
    val start = space.startDate
    return SettingsSpace.Couple(
        me = myself,
        partner = SettingsPerson(partner.nickname, partner.color),
        togetherColorKey = space.togetherColor.orEmpty(),
        startDate = start,
        dayNumber = start?.let { StartDateRules.dayNumber(it, today) },
        linkedDate = space.linkedAt?.atZone(zone)?.toLocalDate(),
    )
}

/** 처음 만난 날 선택 시트의 상태 (SCR-SET-01 [처음 만난 날 수정], D-74). [저장]은 바로 서버에 저장한다 */
data class StartDateSheetState(
    val visibleMonth: YearMonth,
    val selected: LocalDate?,
    /** `PATCH /space` 요청 중 */
    val saving: Boolean = false,
)

/**
 * 전체 메뉴(SCR-SET-01)의 상태. 알림 설정·정보 섹션·앱 정보 카드는 다른 단위(7·8)다.
 *
 * @property account 내 정보를 아직 못 받았으면 null (화면은 앱바만 그린다)
 * @property space 프로필 카드와 연결 정보 행의 값 (커플/혼자). 내 정보를 못 받았으면 null
 * @property startDateSheet [처음 만난 날 수정]으로 연 선택 시트. 닫혀 있으면 null
 * @property today 선택 시트의 마지막 날짜 (기기 시계의 오늘)
 * @property colorKeys 꾸미기 › 우리 색상 행에 보일 현재 색 점의 팔레트 키. 나, 상대, 함께 순서이고 개인 공간은 나 하나다
 */
data class SettingsUiState(
    val account: SettingsAccount? = null,
    val space: SettingsSpace? = null,
    val startDateSheet: StartDateSheetState? = null,
    val today: LocalDate = LocalDate.of(1970, 1, 1),
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

    /** 프로필 카드의 [처음 만난 날 수정] */
    data object StartDateEditClicked : SettingsAction
    data object StartDatePreviousMonth : SettingsAction
    data object StartDateNextMonth : SettingsAction
    data class StartDateSelected(val date: LocalDate) : SettingsAction

    /** 선택 시트의 [저장]: `PATCH /space {startDate}` (FR-COUPLE-04 AC1) */
    data object StartDateSaved : SettingsAction
    data object StartDateSheetDismissed : SettingsAction
}

sealed interface SettingsSideEffect {
    /** 공통 스낵바 */
    data class ShowMessage(@StringRes val messageRes: Int) : SettingsSideEffect

    /** 로그아웃이 끝났다 → SCR-START-01 (백스택을 비운다) */
    data object NavigateToStart : SettingsSideEffect
}
