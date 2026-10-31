package com.duriseo.android

import android.app.Application
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
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}
