package com.duriseo.android.core.common.social

import android.content.Context
import com.duriseo.android.core.common.config.AppConfig
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 시작 때 소셜 SDK를 초기화한다. 카카오 앱 키가 비어 있으면(CI) 건너뛴다 (unit-2.md G절). */
@Singleton
class SocialSdkInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AppConfig,
) {
    fun initialize() {
        if (config.kakaoNativeAppKey.isNotBlank()) {
            KakaoSdk.init(context, config.kakaoNativeAppKey)
        }
    }
}
