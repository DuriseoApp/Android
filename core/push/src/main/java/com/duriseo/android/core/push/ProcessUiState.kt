package com.duriseo.android.core.push

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이 프로세스에서 화면(Activity)이 한 번이라도 만들어졌는지. 동기화 신호(`SYNC`)는 "프로세스가 살아 있을 때만" 처리한다
 * (API.md 6장): FCM이 앱을 깨우려고 새로 만든 프로세스(화면 없음)는 살아 있던 앱이 아니라서 신호를 무시한다.
 * 다음 진입 때 화면이 다시 불러온다 (NFR-SYNC-02). `MyApp`이 Activity가 만들어질 때 [markUiStarted]를 부른다.
 */
@Singleton
class ProcessUiState @Inject constructor() {
    @Volatile
    var uiStarted: Boolean = false
        private set

    fun markUiStarted() {
        uiStarted = true
    }
}
