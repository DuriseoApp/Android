package com.duriseo.android.data.remote.network

import com.duriseo.android.core.common.config.OsVersion
import com.duriseo.android.core.common.update.AppVersion
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

internal const val HEADER_PLATFORM = "X-Platform"
internal const val HEADER_APP_VERSION = "X-App-Version"
internal const val HEADER_APP_BUILD = "X-App-Build"
internal const val HEADER_OS_VERSION = "X-OS-Version"

/**
 * 모든 API 요청(로그인 전·공개 조회 포함)에 공통 헤더 네 개를 붙인다 (API.md 1장, D-182, FR-APP-03 AC1):
 * `X-Platform: ANDROID`, `X-App-Version`(versionName), `X-App-Build`(versionCode), `X-OS-Version`(API 레벨).
 * 서버의 강제 업데이트 차단(R-20)과 기기 정보 저장이 쓴다. "인증 없음"·"인증" 두 클라이언트에 모두 붙인다.
 */
@Singleton
class CommonHeadersInterceptor @Inject constructor(
    private val version: AppVersion,
    private val os: OsVersion,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(HEADER_PLATFORM, PLATFORM_ANDROID)
            .header(HEADER_APP_VERSION, version.name)
            .header(HEADER_APP_BUILD, version.code.toString())
            .header(HEADER_OS_VERSION, os.apiLevel.toString())
            .build()
        return chain.proceed(request)
    }

    private companion object {
        const val PLATFORM_ANDROID = "ANDROID"
    }
}
