package com.duriseo.android.domain.auth

import java.time.Instant

/**
 * 서버가 발급한 토큰 한 벌 (API.md 3.1 `AuthTokens`에서 `isNewUser`를 뺀 것).
 * refresh 토큰은 43자 무작위 문자열이다 (API.md 1장). 기기에는 암호화해 저장한다 (NFR-SEC-01).
 *
 * [toString]은 토큰 값을 찍지 않는다. 로그나 크래시 보고에 새면 안 된다.
 */
data class AuthTokens(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
) {
    override fun toString(): String =
        "AuthTokens(accessTokenExpiresAt=$accessTokenExpiresAt, refreshTokenExpiresAt=$refreshTokenExpiresAt, tokens=***)"
}

/** 로그인 계열 응답: 토큰과 "이번 요청으로 사용자가 새로 만들어졌는지" (API.md 3.1 `AuthTokens.isNewUser`) */
data class IssuedTokens(
    val tokens: AuthTokens,
    val isNewUser: Boolean,
)

/** 로그인(게스트 시작, 소셜 로그인, switch)의 결과. 화면은 [isNewUser]로 갈 곳을 고른다 (FR-AUTH-02 AC1). */
data class LoginResult(
    val isNewUser: Boolean,
)
