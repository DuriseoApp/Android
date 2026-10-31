package com.duriseo.android.data.remote.auth

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * access 토큰이 필요 없는 인증 경로 (API.md 1장: `/auth/guest`, `/auth/social`, `/auth/refresh`).
 * "인증 없음" 클라이언트(`@NoAuth`)로 만든다. 경로는 기본 주소 `/api/v1/` 아래 (3.1).
 */
interface AuthApi {
    /** 201 `AuthTokens` (`isNewUser: true`). 본문 없음 (4.1) */
    @POST("auth/guest")
    suspend fun guest(): Response<AuthTokensDto>

    /** 200 `AuthTokens`. 토큰 검증 실패는 401 `SOCIAL_TOKEN_INVALID` (4.2) */
    @POST("auth/social")
    suspend fun social(@Body request: SocialTokenRequest): Response<AuthTokensDto>

    /**
     * 200 `AuthTokens` (새 refresh 토큰). OkHttp Authenticator가 동기로 부르려고 [Call]이다.
     * 401 `SESSION_REPLACED`·`SESSION_INVALID`는 세션 종료 (API.md 1장).
     */
    @POST("auth/refresh")
    fun refresh(@Body request: RefreshRequest): Call<AuthTokensDto>
}

/**
 * 로그인한 상태(게스트 access 토큰 또는 회원 access 토큰)로 부르는 인증 경로.
 * "인증" 클라이언트(`@Authed`)로 만든다.
 */
interface AuthSessionApi {
    /** 204. 새 토큰 없음, 앱은 `GET /me`로 다시 읽는다. 409 `SOCIAL_ALREADY_USED` -> 4.7 (4.3) */
    @POST("auth/link")
    suspend fun link(@Body request: SocialTokenRequest): Response<Unit>

    /** 200 `AuthTokens` (`isNewUser: false`). 404 `SOCIAL_NOT_REGISTERED` (4.7) */
    @POST("auth/switch")
    suspend fun switch(@Body request: SocialTokenRequest): Response<AuthTokensDto>

    /** 204. 본문 없음. 회원 전용 (R-17) */
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
