package com.duriseo.android.domain.appops

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * 두 곳에서 읽은 값 한 벌. [server]가 null이면 서버 값을 못 받았다(3초 안에 답이 없음, 오류, 오프라인).
 * [remote]는 Remote Config의 (마지막으로 활성화한) 값이다. 값이 없거나 Firebase가 없으면 비어 있다.
 */
data class AppOpsSnapshot(
    val remote: AppOpsData,
    val server: AppOpsData?,
)

/**
 * 공지·업데이트 팝업의 원천 값 (FR-APP-01 AC6, D-176~D-178, API.md 4.9 2단계).
 * 어느 팝업을 띄울지는 [AppPopupResolver]와 [AppPopupCoordinator]가 정한다.
 */
interface AppOpsRepository {
    /**
     * Remote Config와 서버를 **동시에** 읽는다. 서버는 3초까지만 기다리고, 실패·시간 초과·오프라인이면 [AppOpsSnapshot.server]가 null이다.
     * Remote Config도 3초까지만 기다리고 늦으면 마지막으로 활성화한 값을 쓴다(늦게 도착해 값이 바뀌면 [changes]로 알린다).
     * 던지지 않는다. 두 곳 모두 없으면 둘 다 빈 값이다.
     * Remote Config 가져오기 간격은 5분이다(그 안에는 다시 가져오지 않고 활성화된 값을 쓴다, D-178).
     */
    suspend fun check(): AppOpsSnapshot

    /**
     * Remote Config 값이 바뀔 때(실시간 리스너가 활성화한 뒤, 또는 [check] 때 늦게 도착한 값) 마지막 서버 값과 합쳐 낸다 (D-178).
     * 구독하는 동안만 듣는다.
     */
    val changes: Flow<AppOpsSnapshot>
}

/**
 * 서버 어느 요청이든 426 `APP_VERSION_UNSUPPORTED`를 받으면 채운다 (R-20, 4.9 7단계). 토큰·로컬 데이터를 지우지 않는다.
 * 프로세스가 살아 있는 동안 유지한다 (unit-9 결정 2). 구현은 `data:remote`다.
 */
interface ForceUpdateSignals {
    val current: StateFlow<ForceUpdateSignal?>

    /** 서버 목록에서 그 규칙이 사라졌을 때 닫는다 */
    fun clear()
}

/** "오늘 하루 안 보기"를 기기에 기억한다 (D-179). 기기 단위라 로그아웃·탈퇴로 지우지 않는다 */
interface PopupHiddenTodayStore {
    /** [today]에 숨긴 `id`. 지난 날짜 기록은 이때 정리한다 */
    suspend fun hiddenIds(today: LocalDate): HiddenTodayIds

    /** [id]를 [today](기기 시간대의 오늘) 자정까지 숨긴다. 공지와 규칙의 `id`는 따로 기억한다 */
    suspend fun hideToday(space: PopupIdSpace, id: String, today: LocalDate)
}

/** `https://` 팝업 이미지를 띄우기 전에 미리 받아 둔다 (Q-23). 구현은 인증·공통 헤더 없는 별도 로더다 (unit-9 결정 3) */
interface PopupImagePrefetcher {
    /** 받아 두었으면 true, 못 받으면 false(던지지 않는다). 시간 제한은 부르는 쪽이 건다 */
    suspend fun prefetch(url: String): Boolean
}
