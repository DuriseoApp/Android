package com.duriseo.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.duriseo.android.core.ads.AdsInitializer
import com.duriseo.android.core.common.social.SocialSdkInitializer
import com.duriseo.android.core.common.update.AppForegroundTracker
import com.duriseo.android.core.common.update.AppUpdateSource
import com.duriseo.android.core.push.ProcessUiState
import com.duriseo.android.core.push.PushInitializer
import com.duriseo.android.data.repository.auth.SessionStartup
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var socialSdkInitializer: SocialSdkInitializer

    @Inject
    lateinit var sessionStartup: SessionStartup

    @Inject
    lateinit var pushInitializer: PushInitializer

    /** 광고 SDK 초기화 (D-186). noAdsDebug·ID 없는 release는 아무것도 안 한다 */
    @Inject
    lateinit var adsInitializer: AdsInitializer

    @Inject
    lateinit var processUiState: ProcessUiState

    @Inject
    lateinit var appUpdateSource: AppUpdateSource

    /** 앱으로 돌아옴을 공지·업데이트 팝업(`feature:appops`)에 알린다 (D-178) */
    @Inject
    lateinit var appForeground: AppForegroundTracker

    /** 프로필 사진을 받는 로더 (`data:remote`의 `ProfileImageModule`: 인증 OkHttp). 처음 사진을 그릴 때 만든다 */
    @Inject
    lateinit var imageLoader: Lazy<ImageLoader>

    override fun onCreate() {
        super.onCreate()

        initTimber()
        // 카카오 SDK 초기화 (앱 키가 비면 건너뜀)
        socialSdkInitializer.initialize()
        // 토큰 저장소를 읽고, 로그인 상태면 뒤에서 내 정보·시간대를 갱신한다 (FR-AUTH-01, NFR-TIME-01)
        sessionStartup.start()
        // 알림 채널 5개를 만들고, 로그인 상태면 FCM 토큰을 서버에 등록한다 (단위 7). google-services.json이 없으면 FCM 없이 동작한다
        pushInitializer.start()
        // 앱 시작 때 이미 받아 둔 인앱 업데이트가 있는지 본다. Play가 설치하지 않은 앱이면 조용히 끝난다 (D-151)
        appUpdateSource.start()
        // 광고가 켜져 있을 때만 백그라운드에서 SDK를 한 번 초기화한다 (FR-AD-04 AC4)
        adsInitializer.start()
        // 앱으로 돌아올 때 기기 시간대가 저장값과 다르면 다시 알린다 (Q-19 ①).
        // 화면이 한 번이라도 만들어진 프로세스만 동기화 신호(SYNC)를 처리한다 (API.md 6장 "프로세스가 살아 있을 때만")
        registerActivityLifecycleCallbacks(
            ForegroundCallbacks(
                onForeground = {
                    sessionStartup.onForeground()
                    // 회전이 아니라 백그라운드를 거쳐 돌아왔을 때만 팝업을 다시 확인한다
                    appForeground.onForeground()
                },
                // 인앱 업데이트를 다 받았으면 앱이 백그라운드로 갈 때 설치한다 (사용 중에 앱을 다시 시작하지 않는다)
                onBackground = {
                    appUpdateSource.completeIfDownloaded()
                    appForeground.onBackground()
                },
                onAnyActivityCreated = processUiState::markUiStarted,
            ),
        )
    }

    /** Coil의 `AsyncImage`가 쓰는 앱 전체 로더. 사진 요청에 Bearer 헤더와 401 토큰 갱신이 붙는다 */
    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()

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
    private val onBackground: () -> Unit = {},
) : Application.ActivityLifecycleCallbacks {
    private var startedCount = 0
    private var hasStartedBefore = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onActivityStarted(activity: Activity) {
        if (startedCount == 0 && hasStartedBefore) onForeground()
        startedCount++
        hasStartedBefore = true
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount--
        // 화면 회전처럼 Activity가 잠깐 없어지는 경우를 걸러내려고 잠시 뒤에도 보이는 화면이 없을 때만 "백그라운드"로 본다
        // (`ProcessLifecycleOwner`의 ON_STOP 지연 700ms와 같다)
        if (startedCount == 0) {
            handler.postDelayed({ if (startedCount == 0) onBackground() }, BackgroundDelayMillis)
        }
    }

    private companion object {
        const val BackgroundDelayMillis = 700L
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = onAnyActivityCreated()
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
