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
