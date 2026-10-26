package com.duriseo.android.di

import com.duriseo.android.BuildConfig
import com.duriseo.android.core.common.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 빌드 타입별 설정을 `data:remote` 등에 넘긴다. `data:remote`는 app의 `BuildConfig`를 볼 수 없다.
 * 값은 `local.properties`에서 왔고 코드에는 없다 (docs/SETUP.md 2장).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = AppConfig(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        kakaoNativeAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY,
    )
}
