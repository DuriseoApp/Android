package com.duriseo.android.data.remote.network

import okhttp3.logging.HttpLoggingInterceptor

/**
 * OkHttp 로깅 (NFR-SEC-01). 켜는 것은 debug 빌드뿐이고 `Authorization` 헤더 값은 가린다.
 * 본문(BODY)은 로그인·토큰 갱신 응답에 토큰이 들어 있어서 남기지 않고 요청·응답 줄과 헤더(HEADERS)까지만 남긴다.
 */
internal fun createLoggingInterceptor(
    enabled: Boolean,
    logger: HttpLoggingInterceptor.Logger,
): HttpLoggingInterceptor = HttpLoggingInterceptor(logger).apply {
    redactHeader("Authorization")
    level = if (enabled) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
}
