package com.duriseo.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.duriseo.android.core.common.social.SocialSdkInitializer
import com.duriseo.android.core.push.ProcessUiState
import com.duriseo.android.core.push.PushInitializer
import com.duriseo.android.data.repository.auth.SessionStartup
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var socialSdkInitializer: SocialSdkInitializer

    @Inject
    lateinit var sessionStartup: SessionStartup

    @Inject
    lateinit var pushInitializer: PushInitializer

    @Inject
    lateinit var processUiState: ProcessUiState

    override fun onCreate() {
        super.onCreate()

        initTimber()
        // 카카오 SDK 초기화 (앱 키가 비면 건너뜀)
        socialSdkInitializer.initialize()
        // 토큰 저장소를 읽고, 로그인 상태면 뒤에서 내 정보·시간대를 갱신한다 (FR-AUTH-01, NFR-TIME-01)
        sessionStartup.start()
        // 알림 채널 5개를 만들고, 로그인 상태면 FCM 토큰을 서버에 등록한다 (단위 7). google-services.json이 없으면 FCM 없이 동작한다
        pushInitializer.start()
        // 앱으로 돌아올 때 기기 시간대가 저장값과 다르면 다시 알린다 (Q-19 ①).
        // 화면이 한 번이라도 만들어진 프로세스만 동기화 신호(SYNC)를 처리한다 (API.md 6장 "프로세스가 살아 있을 때만")
        registerActivityLifecycleCallbacks(
            ForegroundCallbacks(
                onForeground = sessionStartup::onForeground,
                onAnyActivityCreated = processUiState::markUiStarted,
            ),
        )
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}

/**
 * 화면(Activity)이 하나라도 보이기 시작하는 순간을 앱 단위 "앱으로 돌아옴"으로 본다
 * (`ProcessLifecycleOwner`의 ON_START와 같다. `lifecycle-process`는 라이브러리 계획에 없어 쓰지 않는다).
 * 프로세스의 첫 시작은 `SessionStartup.start()`가 하므로 건너뛴다. 화면 회전처럼 Activity가 다시 만들어질 때도 불리지만,
 * 시간대가 같으면 `syncTimeZone`이 요청 없이 끝나서 비용이 없다.
 */
internal class ForegroundCallbacks(
    private val onForeground: () -> Unit,
    private val onAnyActivityCreated: () -> Unit = {},
) : Application.ActivityLifecycleCallbacks {
    private var startedCount = 0
    private var hasStartedBefore = false

    override fun onActivityStarted(activity: Activity) {
        if (startedCount == 0 && hasStartedBefore) onForeground()
        startedCount++
        hasStartedBefore = true
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount--
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = onAnyActivityCreated()
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
