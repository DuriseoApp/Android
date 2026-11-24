package com.duriseo.android.core.common.social

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.config.AppConfig
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
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
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            logDiagnostics()
        }
    }

    /**
     * debug 빌드에서만 콘솔 등록값과 비교할 값을 남긴다.
     * - Google Cloud: Android OAuth 클라이언트의 패키지 이름·SHA-1, 같은 프로젝트의 웹 클라이언트 ID
     * - Kakao Developers: 플랫폼 Android의 패키지 이름·키 해시
     */
    private fun logDiagnostics() {
        runCatching {
            Logger.i(SOCIAL_LOG_TAG, "패키지 이름: ${context.packageName}")
            Logger.i(SOCIAL_LOG_TAG, "서버 주소: ${config.apiV1BaseUrl() ?: "(비어 있음) → 서버를 부를 수 없어요"}")
            Logger.i(SOCIAL_LOG_TAG, "서명 SHA-1: ${signingSha1() ?: "읽지 못함"}")
            val webClientId = config.googleWebClientId
            Logger.i(SOCIAL_LOG_TAG, "Google 웹 클라이언트 ID: ${webClientId.ifBlank { "(비어 있음)" }}")
            if (config.kakaoNativeAppKey.isBlank()) {
                Logger.w(SOCIAL_LOG_TAG, "Kakao 네이티브 앱 키: (비어 있음) → SDK를 초기화하지 않았어요")
            } else {
                Logger.i(SOCIAL_LOG_TAG, "Kakao 네이티브 앱 키: 설정됨 (${config.kakaoNativeAppKey.takeLast(4)}로 끝남)")
                Logger.i(SOCIAL_LOG_TAG, "Kakao 키 해시: ${Utility.getKeyHash(context)}")
            }
        }.onFailure { Logger.w(SOCIAL_LOG_TAG, "진단 로그를 남기지 못했어요", it) }
    }

    @Suppress("DEPRECATION")
    private fun signingSha1(): String? {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val cert = signatures?.firstOrNull()?.toByteArray() ?: return null
        return MessageDigest.getInstance("SHA-1").digest(cert)
            .joinToString(":") { "%02X".format(it) }
    }
}
