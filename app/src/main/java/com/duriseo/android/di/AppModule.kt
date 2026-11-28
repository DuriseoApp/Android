package com.duriseo.android.di

import android.os.Build
import androidx.annotation.RawRes
import com.duriseo.android.BuildConfig
import com.duriseo.android.R
import com.duriseo.android.core.common.config.AdsConfig
import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.core.common.config.LicensesJsonRes
import com.duriseo.android.core.common.config.OsVersion
import com.duriseo.android.core.common.update.AppVersion
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

    /** 광고 설정 (D-186). release인데 ID가 비면 [AdsConfig.create]가 광고를 끈다 */
    @Provides
    @Singleton
    fun provideAdsConfig(): AdsConfig = AdsConfig.create(
        enabled = BuildConfig.ADS_ENABLED,
        memoListNativeId = BuildConfig.ADMOB_MEMO_LIST_NATIVE_ID,
        anniversaryListNativeId = BuildConfig.ADMOB_ANNIVERSARY_LIST_NATIVE_ID,
        menuBottomBannerId = BuildConfig.ADMOB_MENU_BOTTOM_BANNER_ID,
    )

    /** 앱 정보 카드와 문의 메일의 앱 버전 (FR-SET-02 AC1·AC4). `feature`는 app의 `BuildConfig`를 볼 수 없다 */
    @Provides
    @Singleton
    fun provideAppVersion(): AppVersion = AppVersion(
        name = BuildConfig.VERSION_NAME,
        code = BuildConfig.VERSION_CODE.toLong(),
    )

    /** 공통 요청 헤더 `X-OS-Version`·기기 등록 `osVersion`의 값: API 레벨 (D-182) */
    @Provides
    @Singleton
    fun provideOsVersion(): OsVersion = OsVersion(apiLevel = Build.VERSION.SDK_INT)

    /**
     * 오픈소스 라이선스 JSON(AboutLibraries 플러그인이 `res/raw/aboutlibraries.json`으로 만든다)의 리소스 ID (SCR-SET-05).
     * 컴파일 타임에 직접 참조해서 플러그인이 JSON을 못 만들면 빌드가 깨진다. `res/raw/keep.xml`이 리소스 축소에서도 지킨다.
     */
    @Provides
    @LicensesJsonRes
    @RawRes
    fun provideLicensesJsonRes(): Int = R.raw.aboutlibraries
}
