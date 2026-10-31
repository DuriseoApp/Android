package com.duriseo.android.core.common.navigation

import kotlinx.serialization.Serializable

sealed interface RouteModel

sealed interface MainRouteModel : RouteModel {
    /** 하단 탭 1: 캘린더. 앱의 첫 화면 (D-30) */
    @Serializable
    data object Calendar : MainRouteModel

    /** 하단 탭 2: 메모 (D-30). `feature:memo`가 생기기 전까지 빈 자리표시 */
    @Serializable
    data object Memo : MainRouteModel

    /** 전체 메뉴(설정). 하단 탭이 아니라 캘린더 앱바에서 들어간다. 화면은 단위 2부터 (SCR-SET-01 계정 부분, docs/PLAN.md 단위 2) */
    @Serializable
    data object Settings : MainRouteModel
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

/** 전체 메뉴(설정)에서 들어가는 하위 화면 (`feature:settings`) */
sealed interface SettingsRouteModel : RouteModel {
    /** 계정 화면 (SCR-SET-01 하위 화면 "계정": 게스트·회원) */
    @Serializable
    data object Account : SettingsRouteModel
}
