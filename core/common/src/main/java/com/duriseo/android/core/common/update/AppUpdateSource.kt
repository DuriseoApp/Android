package com.duriseo.android.core.common.update

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** 실행 중인 앱의 버전. 값은 app의 `BuildConfig`에서 온다 (`feature`는 app을 볼 수 없다) */
data class AppVersion(val name: String, val code: Long)

/** 새 버전이 있는지 확인한 결과 (FR-SET-02 AC4, D-151) */
enum class AppUpdateAvailability {
    /** 최신이다 */
    UP_TO_DATE,

    /** 새 버전이 있고 앱을 쓰는 중에 받을 수 있다 (FLEXIBLE) */
    AVAILABLE,

    /** 확인할 수 없다: Play 미설치·직접 설치, API 실패, 받을 수 없는 방식 */
    UNKNOWN,
}

/** 새 버전을 받는 진행 */
enum class AppUpdatePhase {
    /** 받지 않는다 (시작 전, 실패, 취소) */
    IDLE,

    /** 받는 중 (대기·설치 포함) */
    DOWNLOADING,

    /** 다 받았다. 앱이 백그라운드로 가면 설치한다 ([AppUpdateSource.completeIfDownloaded]) */
    DOWNLOADED,
}

/**
 * Google Play 인앱 업데이트를 감싼다 (D-151). 화면의 ViewModel은 이 인터페이스만 보고, 실제 구현은 [PlayAppUpdateSource]다.
 * 방식은 FLEXIBLE: 앱을 쓰는 중에 받고, 다 받으면 앱이 백그라운드로 갈 때 설치한다 (사용 중에 앱을 다시 시작하지 않는다).
 */
interface AppUpdateSource {
    /** 받는 진행. 앱 시작 때 이미 받아 둔 것이 있으면 [AppUpdatePhase.DOWNLOADED]가 된다 */
    val phase: StateFlow<AppUpdatePhase>

    /** 새 버전이 있는지 묻는다. 실패하면 던지지 않고 [AppUpdateAvailability.UNKNOWN]을 돌려준다 */
    suspend fun check(): AppUpdateAvailability

    /**
     * 받기를 시작한다 (Play가 확인 창을 띄운다). 사용자가 받기로 했으면 true, 취소하거나 실패하면 false.
     * 받는 진행은 [phase]로 알린다
     */
    suspend fun startUpdate(activity: Activity): Boolean

    /** 앱 시작 때 한 번: 진행 알림을 구독하고 이미 받아 둔 것이 있는지 본다 */
    fun start()

    /** 앱이 백그라운드로 갔다. 다 받은 것이 있으면 지금 설치한다 */
    fun completeIfDownloaded()
}
