package com.duriseo.android.data.remote.network

import com.duriseo.android.core.common.config.AppConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 서버 주소가 비어 있는 빌드(CI, `local.properties` 없음)에서 요청을 보내려 할 때 던진다.
 * 앱을 죽이지 않고 그 요청만 실패하게 한다 (unit-1.md 결정 1).
 */
internal class ServerNotConfiguredException : IOException("서버 주소가 설정되지 않았어요")

/**
 * Retrofit은 기본 주소가 비어 있으면 만들 때 예외를 낸다. 주소가 비어 있어도 Retrofit은 만들어지도록
 * 쓰는 자리 주소. `.invalid`는 이름 풀이가 되지 않는 예약 주소이고, 요청은 [ServerConfigInterceptor]가 먼저 막는다.
 */
internal const val UNCONFIGURED_BASE_URL = "http://unconfigured.invalid/api/v1/"

/** 설정의 서버 주소(호스트)로 `/api/v1/`이 붙은 기본 주소를 만든다. 비었거나 주소 모양이 아니면 null. */
internal fun resolveApiBaseUrl(config: AppConfig): HttpUrl? = config.apiV1BaseUrl()?.toHttpUrlOrNull()

/** 서버 주소가 설정돼 있지 않으면 요청을 보내지 않고 [ServerNotConfiguredException]으로 실패시킨다. */
internal class ServerConfigInterceptor(private val config: AppConfig) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (resolveApiBaseUrl(config) == null) throw ServerNotConfiguredException()
        return chain.proceed(chain.request())
    }
}
