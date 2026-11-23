package com.duriseo.android.data.remote.network

import okhttp3.logging.HttpLoggingInterceptor

/**
 * OkHttp 로깅 (NFR-SEC-01). 켜는 것은 debug 빌드뿐이다 (Logcat 태그 `okhttp`).
 *
 * - 요청·응답 줄, 헤더, JSON 본문(BODY)까지 남긴다. 사진처럼 글자가 아닌 본문은 OkHttp가 "binary … body omitted"로만 남긴다.
 * - `Authorization` 헤더 값은 가린다.
 * - 본문의 토큰 필드는 값을 `***`로 바꿔서 남긴다: 로그인·갱신 응답의 `accessToken`·`refreshToken`,
 *   소셜 로그인 요청의 `token`(Google ID 토큰, Kakao 액세스 토큰), 갱신 요청의 `refreshToken`, 기기 등록의 `fcmToken`.
 */
internal fun createLoggingInterceptor(
    enabled: Boolean,
    logger: HttpLoggingInterceptor.Logger,
): HttpLoggingInterceptor = HttpLoggingInterceptor { line -> logger.log(redactTokens(line)) }.apply {
    redactHeader("Authorization")
    level = if (enabled) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
}

/** 값을 가리는 JSON 필드 이름. API.md의 토큰 필드와 같다 */
internal val RedactedJsonFields = listOf("accessToken", "refreshToken", "token", "fcmToken")

private val tokenFieldPattern = Regex(
    "\"(${RedactedJsonFields.joinToString("|")})\"\\s*:\\s*\"(?:[^\"\\\\]|\\\\.)*\"",
)

/** 로그 한 줄 안의 토큰 필드 값을 `***`로 바꾼다 (필드 이름은 남겨 어떤 요청인지는 보이게) */
internal fun redactTokens(line: String): String =
    tokenFieldPattern.replace(line) { match -> "\"${match.groupValues[1]}\":\"***\"" }
