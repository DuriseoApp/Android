package com.duriseo.android.data.repository.auth

import com.duriseo.android.data.remote.auth.AuthRemoteDataSource
import com.duriseo.android.data.remote.me.MeRemoteDataSource
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.IssuedTokens
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.auth.TokenState
import com.duriseo.android.domain.auth.TokenStorage
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인·연동·로그아웃·탈퇴 (FR-AUTH-01~06). 모두 쓰기라서 `runOnlineWrite`로 감싼다 (NFR-OFF-02, D-86).
 * 로그인 상태의 기준은 토큰 저장소에 토큰이 있는 것이다 (결정 2). 내 정보는 로그인 뒤 `GET /me`로 Room에 넣고,
 * 그 실패는 로그인 결과에 영향을 주지 않는다 (화면이 Room을 보고 있다가 다음 refresh에서 채운다).
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemote: AuthRemoteDataSource,
    private val meRemote: MeRemoteDataSource,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val meRepository: MeRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock,
) : AuthRepository {

    override val authState: Flow<AuthState> = tokenStorage.state
        .map { state ->
            when (state) {
                TokenState.Loading -> AuthState.Unknown
                TokenState.Empty -> AuthState.SignedOut
                is TokenState.Present -> AuthState.SignedIn
            }
        }
        .distinctUntilChanged()

    override val sessionEvents: Flow<SessionEndReason> = sessionManager.events

    override suspend fun restoreSession() = tokenStorage.restore()

    override suspend fun startAsGuest(): DataResult<LoginResult> =
        networkMonitor.runOnlineWrite {
            completeLogin(authRemote.guest())
        }

    override suspend fun signIn(provider: SocialProvider, token: String): DataResult<LoginResult> =
        networkMonitor.runOnlineWrite {
            completeLogin(authRemote.social(provider, token))
        }

    override suspend fun link(provider: SocialProvider, token: String): DataResult<Unit> =
        networkMonitor.runOnlineWrite {
            val result = authRemote.link(provider, token)
            when {
                // 토큰은 그대로 두고 내 정보만 다시 읽는다. 같은 화면이 계정 · 회원으로 바뀐다 (API 4.3 5단계)
                result is DataResult.Success -> meRepository.refreshMe()
                // 이미 회원인데 게스트 화면이었다: 상태를 다시 읽어 바로잡는다
                result.isApiError(ErrorCode.GUEST_ONLY) -> meRepository.refreshMe()
                else -> endSessionIfNeeded(result)
            }
            result
        }

    override suspend fun switchAccount(provider: SocialProvider, token: String): DataResult<LoginResult> =
        networkMonitor.runOnlineWrite {
            when (val result = authRemote.switch(provider, token)) {
                is DataResult.Success -> {
                    // 게스트의 Room·설정을 새 토큰을 저장하기 전에 비운다. 게스트 토큰은 아직 있어서 로그인 상태가 풀리지 않는다
                    if (!sessionManager.clearUserDataOrFail()) {
                        localDataFailure()
                    } else {
                        tokenStorage.save(result.value.tokens)
                        afterLogin()
                        DataResult.Success(LoginResult(result.value.isNewUser))
                    }
                }

                is DataResult.Failure -> {
                    endSessionIfNeeded(result)
                    result
                }
            }
        }

    override suspend fun logout(): DataResult<Unit> =
        networkMonitor.runOnlineWrite {
            finishLeaving(authRemote.logout())
        }

    override suspend fun withdraw(): DataResult<Unit> =
        networkMonitor.runOnlineWrite {
            finishLeaving(meRemote.deleteMe())
        }

    /** 로그인 3종 공통: 토큰을 저장하고, `GET /me`를 Room에 넣고, 시간대를 맞춘다. */
    private suspend fun completeLogin(result: DataResult<IssuedTokens>): DataResult<LoginResult> =
        when (result) {
            is DataResult.Success -> {
                // 이전 사용자의 Room·설정이 새 계정에 보이지 않게 새 토큰을 저장하기 전에 비운다.
                // 비우지 못하면 로그인을 실패로 처리한다 (토큰을 저장하지 않는다)
                if (!sessionManager.clearUserDataOrFail()) {
                    localDataFailure()
                } else {
                    tokenStorage.save(result.value.tokens)
                    afterLogin()
                    DataResult.Success(LoginResult(result.value.isNewUser))
                }
            }

            is DataResult.Failure -> result
        }

    /** 기기 안의 오류. 서버 오류 `INTERNAL_ERROR`와 같이 취급해 화면은 공통 오류 안내를 보인다 (D-88) */
    private fun localDataFailure(): DataResult.Failure = DataResult.Failure(
        DataError.ApiError(code = ErrorCode.INTERNAL_ERROR.code, details = null, traceId = null),
    )

    /** 사용자 동작이 아니라서 실패는 조용히 넘긴다 (오프라인·서버 오류여도 로그인은 끝난 것이다) */
    private suspend fun afterLogin() {
        meRepository.refreshMe()
        meRepository.syncTimeZone(clock.zone.id)
    }

    /**
     * 로그아웃·탈퇴: 204면 토큰·Room·설정·소셜 SDK 상태를 지운다 (D-86). 실패하면 아무것도 지우지 않고
     * 로그인 상태 그대로 둔다. 단 응답이 이미 끊긴 세션이면 로컬을 정리한다 (1장).
     */
    private suspend fun finishLeaving(result: DataResult<Unit>): DataResult<Unit> {
        when (result) {
            is DataResult.Success -> sessionManager.clearAll()
            is DataResult.Failure -> endSessionIfNeeded(result)
        }
        return result
    }

    /**
     * 401 `SESSION_REPLACED`·`SESSION_INVALID`면 세션을 끝낸다. 보통 Authenticator가 먼저 끝내지만
     * (`SessionManager.end`는 한 번만 동작한다) 놓치는 경우가 없도록 여기서도 확인한다.
     */
    private fun endSessionIfNeeded(result: DataResult<*>) {
        when {
            result.isApiError(ErrorCode.SESSION_REPLACED) -> sessionManager.end(SessionEndReason.REPLACED)
            result.isApiError(ErrorCode.SESSION_INVALID) -> sessionManager.end(SessionEndReason.INVALID)
        }
    }

    private fun DataResult<*>.isApiError(code: ErrorCode): Boolean =
        ((this as? DataResult.Failure)?.error as? DataError.ApiError)?.knownCode == code
}
