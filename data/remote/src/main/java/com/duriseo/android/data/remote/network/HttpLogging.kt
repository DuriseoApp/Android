package com.duriseo.android.data.remote.network

import okhttp3.logging.HttpLoggingInterceptor

/**
 * OkHttp 로깅 (NFR-SEC-01). 켜는 것은 debug 빌드뿐이다.
 *
 * - 요청·응답 줄, 헤더, JSON 본문(BODY)까지 남긴다. 사진처럼 글자가 아닌 본문은 OkHttp가 "binary … body omitted"로만 남긴다.
 * - 기본으로 `Authorization` 헤더 값은 가리고, 본문의 토큰 필드는 값을 `***`로 바꿔 남긴다: 로그인·갱신 응답의
 *   `accessToken`·`refreshToken`, 소셜 로그인 요청의 `token`(Google ID 토큰, Kakao 액세스 토큰), 갱신 요청의 `refreshToken`,
 *   기기 등록의 `fcmToken`.
 * - [showTokens]가 true면 둘 다 가리지 않는다. 로컬 디버깅 스위치 `duriseo.debug.logTokens`(docs/SETUP.md 2장)로만 켜지고,
 *   [enabled]가 false(release)면 아무것도 남기지 않으므로 함께 꺼진다.
 *
 * @param output 로그 한 줄을 받을 곳. 앱에서는 `NetworkModule`이 `core:common`의 `Logger`(Timber, 태그 `okhttp`)를 넣고,
 *   테스트는 리스트에 담는다.
 */
internal fun createLoggingInterceptor(
    enabled: Boolean,
    showTokens: Boolean = false,
    output: HttpLoggingInterceptor.Logger,
): HttpLoggingInterceptor {
    val redact = !(enabled && showTokens)
    return HttpLoggingInterceptor { line -> output.log(if (redact) redactTokens(line) else line) }.apply {
        if (redact) redactHeader("Authorization")
        level = if (enabled) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }
}

/** 값을 가리는 JSON 필드 이름. API.md의 토큰 필드와 같다 */
internal val RedactedJsonFields = listOf("accessToken", "refreshToken", "token", "fcmToken")

private val tokenFieldPattern = Regex(
    "\"(${RedactedJsonFields.joinToString("|")})\"\\s*:\\s*\"(?:[^\"\\\\]|\\\\.)*\"",
)

/** 로그 한 줄 안의 토큰 필드 값을 `***`로 바꾼다 (필드 이름은 남겨 어떤 요청인지는 보이게) */
internal fun redactTokens(line: String): String =
    tokenFieldPattern.replace(line) { match -> "\"${match.groupValues[1]}\":\"***\"" }
