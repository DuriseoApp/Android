package com.duriseo.android.data.remote.auth

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.domain.auth.IssuedTokens
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.result.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 API 호출을 [DataResult]와 도메인 모델로 돌려준다 (Retrofit·DTO는 이 모듈 밖으로 나가지 않는다).
 * 토큰 갱신(`/auth/refresh`)은 Authenticator가 따로 부른다.
 */
@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val authApi: AuthApi,
    private val sessionApi: AuthSessionApi,
    private val apiCaller: ApiCaller,
) {
    suspend fun guest(): DataResult<IssuedTokens> =
        apiCaller.call { authApi.guest() }.map { it.toIssued() }

    suspend fun social(provider: SocialProvider, token: String): DataResult<IssuedTokens> =
        apiCaller.call { authApi.social(SocialTokenRequest(provider.toDto(), token)) }.map { it.toIssued() }

    suspend fun link(provider: SocialProvider, token: String): DataResult<Unit> =
        apiCaller.callUnit { sessionApi.link(SocialTokenRequest(provider.toDto(), token)) }

    suspend fun switch(provider: SocialProvider, token: String): DataResult<IssuedTokens> =
        apiCaller.call { sessionApi.switch(SocialTokenRequest(provider.toDto(), token)) }.map { it.toIssued() }

    suspend fun logout(): DataResult<Unit> =
        apiCaller.callUnit { sessionApi.logout() }
}
