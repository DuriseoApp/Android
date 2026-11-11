package com.duriseo.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.duriseo.android.core.common.social.SocialSdkInitializer
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

    override fun onCreate() {
        super.onCreate()

        initTimber()
        // 카카오 SDK 초기화 (앱 키가 비면 건너뜀)
        socialSdkInitializer.initialize()
        // 토큰 저장소를 읽고, 로그인 상태면 뒤에서 내 정보·시간대를 갱신한다 (FR-AUTH-01, NFR-TIME-01)
        sessionStartup.start()
        // 앱으로 돌아올 때 기기 시간대가 저장값과 다르면 다시 알린다 (Q-19 ①)
        registerActivityLifecycleCallbacks(ForegroundCallbacks(onForeground = sessionStartup::onForeground))
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
internal class ForegroundCallbacks(private val onForeground: () -> Unit) : Application.ActivityLifecycleCallbacks {
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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
