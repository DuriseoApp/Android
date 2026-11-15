package com.duriseo.android.core.common.navigation

import kotlinx.serialization.Serializable

sealed interface RouteModel

sealed interface MainRouteModel : RouteModel {
    /** 하단 탭 1: 캘린더. 앱의 첫 화면 (D-30) */
    @Serializable
    data object Calendar : MainRouteModel

    /** 하단 탭 2: 메모 (SCR-MEMO-01, D-30) */
    @Serializable
    data object Memo : MainRouteModel

    /** 전체 메뉴(설정). 하단 탭이 아니라 캘린더 앱바에서 들어간다. 화면은 단위 2부터 (SCR-SET-01 계정 부분, docs/PLAN.md 단위 2) */
    @Serializable
    data object Settings : MainRouteModel
}

/** 캘린더에서 들어가는 일정 화면 (`feature:calendar`). 하단 탭이 없다 */
sealed interface CalendarRouteModel : RouteModel {
    /** 일정 상세 (SCR-CAL-02) */
    @Serializable
    data class EventDetail(val eventId: String) : CalendarRouteModel

    /**
     * 일정 작성·수정 (SCR-CAL-03).
     *
     * @property eventId 있으면 수정, 없으면 새로 추가
     * @property date 새로 추가할 때 처음 채울 날짜 (`YYYY-MM-DD`, 캘린더에서 선택한 날). 없으면 오늘.
     *   일정 탭의 날짜일 뿐이고 메모 날짜는 미리 채우지 않는다 (D-133)
     * @property startOnMemo 새로 추가할 때 [메모] 탭을 고른 채로 연다 (메모 탭의 FAB, D-133). 수정에서는 쓰지 않는다
     */
    @Serializable
    data class EventEdit(
        val eventId: String? = null,
        val date: String? = null,
        val startOnMemo: Boolean = false,
    ) : CalendarRouteModel
}

/** 기념일 화면 (`feature:anniversary`). 하단 탭이 없다. 진입은 캘린더 (SCR-CAL-01 앱바·기념일 카드, 단위 5 묶음 B) */
sealed interface AnniversaryRouteModel : RouteModel {
    /** 기념일 목록 (SCR-ANNIV-01) */
    @Serializable
    data object Anniversaries : AnniversaryRouteModel

    /**
     * 직접 기념일 작성·수정 (SCR-ANNIV-02).
     *
     * @property anniversaryId 있으면 수정, 없으면 새로 추가
     * @property date 수정할 때 누른 항목의 날짜 (`YYYY-MM-DD`). 매년 반복 기념일은 그 해의 발생일이고, 등록한 날짜가 아니다
     *   (unit-5.md 확인 필요 1: 날짜 칸이 이 값을 보여 주고, 사용자가 바꾼 때만 `PATCH`에 날짜를 보낸다)
     */
    @Serializable
    data class Edit(val anniversaryId: String? = null, val date: String? = null) : AnniversaryRouteModel
}

/** 메모 하위 화면 (`feature:memo`). 하단 탭이 없다. 진입은 메모 탭(SCR-MEMO-01), 단위 6 묶음 C부터는 캘린더 날짜 목록의 메모도 상세로 온다 */
sealed interface MemoRouteModel : RouteModel {
    /** 고정 메모 전체 (SCR-MEMO-02). 고정이 4개 이상일 때만 SCR-MEMO-01이 연다 */
    @Serializable
    data object Pinned : MemoRouteModel

    /** 메모 상세 (SCR-MEMO-03) */
    @Serializable
    data class Detail(val memoId: String) : MemoRouteModel

    /**
     * 메모 수정 (SCR-MEMO-04, 종류 탭 없음). 새 메모는 이 경로가 아니라 [CalendarRouteModel.EventEdit]의 [메모] 탭이다 (D-133).
     *
     * @property memoId 수정할 메모. 속성 이름 `memoId`는 `MemoEditViewModel`이
     *   `SavedStateHandle`에서 같은 이름으로 읽는다 (`MemoEditViewModel.ARG_MEMO_ID`)
     */
    @Serializable
    data class Edit(val memoId: String) : MemoRouteModel
}

/** 로그인 전 화면 (`feature:auth`). 하단 탭이 없다. */
sealed interface AuthRouteModel : RouteModel {
    /**
     * 시작 화면 (SCR-START-01).
     *
     * @property sessionReplaced 다른 기기 로그인으로 로그아웃돼 온 경우 true. 이 화면이 스낵바
     *   "다른 기기에서 로그인해서 로그아웃됐어요"를 보여 준다 (D-51, FR-AUTH-02 AC3)
     */
    @Serializable
    data class Start(val sessionReplaced: Boolean = false) : AuthRouteModel
}

/** 알림 목록 (`feature:notification`). 하단 탭이 없다. 진입은 SCR-CAL-01 앱바의 알림 버튼 (단위 7 묶음 C) */
sealed interface NotificationRouteModel : RouteModel {
    /** 알림 목록 (SCR-NOTI-01) */
    @Serializable
    data object Notifications : NotificationRouteModel
}

/** 전체 메뉴(설정)에서 들어가는 하위 화면 (`feature:settings`) */
sealed interface SettingsRouteModel : RouteModel {
    /**
     * 계정 화면 (SCR-SET-01 하위 화면 "계정": 게스트·회원)
     *
     * @property fromCouple 게스트가 SCR-COUPLE-01의 [로그인하고 연결하기]로 들어왔다. 연동을 마치면 연동 성공 안내 없이
     *   SCR-COUPLE-01(회원 화면)로 돌아간다 (FR-AUTH-03 AC4, D-79, SCREENS.md "연동 성공 안내")
     */
    @Serializable
    data class Account(val fromCouple: Boolean = false) : SettingsRouteModel

    /** 우리 색상 (SCR-SET-03) */
    @Serializable
    data object Colors : SettingsRouteModel

    /** 연결 정보 (SCR-SET-01 하위 화면 "연결 정보": 연결됨·혼자 사용 중) */
    @Serializable
    data object ConnectionInfo : SettingsRouteModel

    /** 알림 설정 (SCR-SET-02). 진입은 SCR-SET-01의 알림 설정 행 (단위 7 묶음 E) */
    @Serializable
    data object NotificationSettings : SettingsRouteModel
}

/** 커플 연결 화면 (`feature:couple`). 하단 탭이 없다. */
sealed interface CoupleRouteModel : RouteModel {
    /** 연결 방법 선택 (SCR-COUPLE-01). 회원·게스트 모양은 화면이 내 정보로 정한다 */
    @Serializable
    data object Connect : CoupleRouteModel

    /** 초대코드 만들기 (SCR-COUPLE-02) */
    @Serializable
    data object InviteCreate : CoupleRouteModel

    /** 초대코드 입력하기 (SCR-COUPLE-03) */
    @Serializable
    data object InviteEnter : CoupleRouteModel
}
