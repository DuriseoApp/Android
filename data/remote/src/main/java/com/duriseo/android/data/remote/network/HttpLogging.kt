package com.duriseo.android.data.remote.network

import okhttp3.logging.HttpLoggingInterceptor

/**
 * OkHttp 로깅. debug 빌드에서만 켠다.
 *
 * - debug: 요청·응답 줄, 헤더, 본문(BODY)을 **가리지 않고** 모두 남긴다. `Authorization` 헤더와 로그인·갱신 응답의 토큰도
 *   그대로 보인다 (개발 확인용, 루트 CLAUDE.md 규칙 5, 2026-11-23 사용자 결정). 사진처럼 글자가 아닌 본문은 OkHttp가
 *   "binary … body omitted"로만 남긴다.
 * - release: 아무것도 남기지 않는다 (NFR-SEC-01).
 *
 * @param output 로그 한 줄을 받을 곳. 앱에서는 `NetworkModule`이 `core:common`의 `Logger`(Timber, 태그 `okhttp`)를 넣고,
 *   테스트는 리스트에 담는다.
 */
internal fun createLoggingInterceptor(
    enabled: Boolean,
    output: HttpLoggingInterceptor.Logger,
): HttpLoggingInterceptor = HttpLoggingInterceptor(output).apply {
    level = if (enabled) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
}
