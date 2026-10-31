package com.duriseo.android.data.remote.network

import com.duriseo.android.domain.auth.TokenStorage
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

internal const val AUTHORIZATION = "Authorization"
internal const val BEARER_PREFIX = "Bearer "

/** 요청에 실린 `Authorization: Bearer <access>`의 access 값. 없거나 Bearer 형식이 아니면 null */
internal fun Request.bearerToken(): String? =
    header(AUTHORIZATION)?.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)

internal fun Request.withBearer(accessToken: String): Request =
    newBuilder().header(AUTHORIZATION, BEARER_PREFIX + accessToken).build()

/**
 * 저장된 access 토큰을 `Authorization: Bearer <access>`로 붙인다 (API.md 1장).
 * 토큰이 없으면(로그아웃 상태) 헤더 없이 보낸다. 값은 로그에 남기지 않는다 (HTTP 로깅이 헤더를 가린다).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val access = tokenStorage.current()?.accessToken
        return if (access == null || request.header(AUTHORIZATION) != null) {
            chain.proceed(request)
        } else {
            chain.proceed(request.withBearer(access))
        }
    }
}
