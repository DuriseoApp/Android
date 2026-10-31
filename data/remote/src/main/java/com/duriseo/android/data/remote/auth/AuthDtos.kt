package com.duriseo.android.data.remote.auth

import com.duriseo.android.domain.auth.AuthTokens
import com.duriseo.android.domain.auth.IssuedTokens
import com.duriseo.android.domain.me.SocialProvider
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

// API.md 3.1 (인증). 필드 이름과 타입은 문서 그대로. 토큰이 든 DTO는 toString이 값을 찍지 않는다 (NFR-SEC-01).

/** `AuthTokens` (`/auth/guest`, `/auth/social`, `/auth/switch`, `/auth/refresh`의 응답 본문) */
@Serializable
data class AuthTokensDto(
    val accessToken: String,
    @Contextual val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    @Contextual val refreshTokenExpiresAt: Instant,
    val isNewUser: Boolean,
) {
    override fun toString(): String = "AuthTokensDto(isNewUser=$isNewUser, tokens=***)"
}

/** 요청의 `provider` (`"GOOGLE" | "KAKAO"`). 이름이 곧 JSON 값이다. */
@Serializable
enum class SocialProviderDto { GOOGLE, KAKAO }

/** `POST /auth/social`, `/auth/link`, `/auth/switch` 요청 본문. Google은 ID 토큰, Kakao는 액세스 토큰 */
@Serializable
data class SocialTokenRequest(
    val provider: SocialProviderDto,
    val token: String,
) {
    override fun toString(): String = "SocialTokenRequest(provider=$provider, token=***)"
}

/** `POST /auth/refresh` 요청 본문. access 토큰 없이 refresh 토큰만 보낸다 */
@Serializable
data class RefreshRequest(
    val refreshToken: String,
) {
    override fun toString(): String = "RefreshRequest(refreshToken=***)"
}

internal fun SocialProvider.toDto(): SocialProviderDto = when (this) {
    SocialProvider.GOOGLE -> SocialProviderDto.GOOGLE
    SocialProvider.KAKAO -> SocialProviderDto.KAKAO
}

internal fun SocialProviderDto.toDomain(): SocialProvider = when (this) {
    SocialProviderDto.GOOGLE -> SocialProvider.GOOGLE
    SocialProviderDto.KAKAO -> SocialProvider.KAKAO
}

fun AuthTokensDto.toTokens(): AuthTokens = AuthTokens(
    accessToken = accessToken,
    accessTokenExpiresAt = accessTokenExpiresAt,
    refreshToken = refreshToken,
    refreshTokenExpiresAt = refreshTokenExpiresAt,
)

internal fun AuthTokensDto.toIssued(): IssuedTokens = IssuedTokens(tokens = toTokens(), isNewUser = isNewUser)
