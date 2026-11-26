package com.duriseo.android.core.common.social

import com.duriseo.android.core.common.config.AppConfig
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 카카오 SDK를 실제로 초기화했는지 (`KakaoSdk.init`이 끝까지 성공했는지). 초기화하지 않은 채 SDK를 부르면 앱이 죽는다
 * (2026-11-25 사용자 결정: 앱 키가 비어 초기화를 건너뛴 빌드에서 카카오 로그인을 눌러도 공통 오류로 끝난다).
 * [SocialSdkInitializer]가 성공한 뒤 [markInitialized]를 부르고, 로그인([KakaoTokenSource])·정리([DefaultSocialSessionCleaner])는
 * [isUsable]을 먼저 본다.
 */
@Singleton
class KakaoSdkState @Inject constructor() {
    private val initialized = AtomicBoolean(false)

    fun markInitialized() {
        initialized.set(true)
    }

    /** 앱 키가 있고 초기화도 끝났을 때만 SDK를 부를 수 있다 */
    fun isUsable(config: AppConfig): Boolean = isKakaoSdkUsable(config.kakaoNativeAppKey, initialized.get())
}

/** 판단 함수: 키가 비어 있거나 초기화하지 않았으면 SDK를 부르지 않는다 */
internal fun isKakaoSdkUsable(nativeAppKey: String, initialized: Boolean): Boolean =
    nativeAppKey.isNotBlank() && initialized
