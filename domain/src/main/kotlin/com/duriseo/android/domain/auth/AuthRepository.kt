package com.duriseo.android.domain.auth

import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

/** 로그인 상태 (결정 2: 토큰 저장소에 토큰이 있으면 로그인) */
enum class AuthState {
    /** 토큰 저장소를 아직 읽지 않았다. 화면은 빈 화면 */
    Unknown,
    SignedOut,
    SignedIn,
}

/**
 * 로그인·로그아웃·탈퇴 (FR-AUTH-01~06). 로그인 3종·연동·로그아웃·탈퇴는 쓰기라서 오프라인이면
 * 요청 없이 `DataError.Offline`을 돌려준다 (NFR-OFF-02, D-86).
 */
interface AuthRepository {
    val authState: Flow<AuthState>

    /** 세션이 서버에서 끊겼을 때 (로컬 정리가 끝난 뒤) 한 번씩 내보낸다. 로그아웃·탈퇴는 내보내지 않는다. */
    val sessionEvents: Flow<SessionEndReason>

    /** 앱 시작 때 토큰 저장소를 읽어 [authState]를 정한다 (FR-AUTH-01 AC2·AC3) */
    suspend fun restoreSession()

    /** `POST /auth/guest` -> 토큰 저장 -> `GET /me` (FR-AUTH-01 AC1) */
    suspend fun startAsGuest(): DataResult<LoginResult>

    /** `POST /auth/social` -> 토큰 저장 -> `GET /me` (FR-AUTH-02) */
    suspend fun signIn(provider: SocialProvider, token: String): DataResult<LoginResult>

    /** `POST /auth/link` 204 -> `GET /me`. 토큰은 그대로 (FR-AUTH-03 AC1) */
    suspend fun link(provider: SocialProvider, token: String): DataResult<Unit>

    /** `POST /auth/switch` -> 새 토큰 저장, 게스트 로컬 데이터 삭제 -> `GET /me` (FR-AUTH-03 AC3) */
    suspend fun switchAccount(provider: SocialProvider, token: String): DataResult<LoginResult>

    /** `POST /auth/logout` 204 -> 토큰·Room·설정·소셜 SDK 상태 삭제 (FR-AUTH-05 AC1, D-86) */
    suspend fun logout(): DataResult<Unit>

    /** `DELETE /me` 204 -> 로그아웃과 같은 삭제 (FR-AUTH-06 AC1, D-86) */
    suspend fun withdraw(): DataResult<Unit>
}
