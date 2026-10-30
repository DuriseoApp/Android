package com.duriseo.android.feature.auth.start

import android.app.Activity
import com.duriseo.android.core.common.social.SocialTokenProvider
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/** 시작 화면 테스트용 가짜 AuthRepository. 부른 횟수와 인자를 기록한다. */
class FakeAuthRepository : AuthRepository {
    override val authState = MutableStateFlow(AuthState.SignedOut)
    override val sessionEvents: Flow<SessionEndReason> = MutableSharedFlow()

    var guestResult: DataResult<LoginResult> = DataResult.Success(LoginResult(isNewUser = false))
    var signInResult: DataResult<LoginResult> = DataResult.Success(LoginResult(isNewUser = false))

    /** 설정하면 signIn이 이 신호를 받을 때까지 멈춘다 (요청 중 상태를 확인하려고) */
    var signInGate: CompletableDeferred<Unit>? = null

    var guestCalls = 0
    val signInCalls = mutableListOf<Pair<SocialProvider, String>>()

    override suspend fun restoreSession() = Unit

    override suspend fun startAsGuest(): DataResult<LoginResult> {
        guestCalls++
        return guestResult
    }

    override suspend fun signIn(provider: SocialProvider, token: String): DataResult<LoginResult> {
        signInCalls += provider to token
        signInGate?.await()
        return signInResult
    }

    override suspend fun link(provider: SocialProvider, token: String): DataResult<Unit> = error("시작 화면은 쓰지 않아요")

    override suspend fun switchAccount(provider: SocialProvider, token: String): DataResult<LoginResult> =
        error("시작 화면은 쓰지 않아요")

    override suspend fun logout(): DataResult<Unit> = error("시작 화면은 쓰지 않아요")

    override suspend fun withdraw(): DataResult<Unit> = error("시작 화면은 쓰지 않아요")
}

class FakeMeRepository(initial: Me? = null) : MeRepository {
    val me = MutableStateFlow(initial)

    override fun observeMe(): Flow<Me?> = me

    override suspend fun refreshMe(): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun syncTimeZone(zoneId: String): DataResult<Unit> = DataResult.Success(Unit)
}

/** SDK 호출은 이 테스트들이 직접 부르지 않는다 (결과는 `SocialTokenReceived` 액션으로 넣는다) */
object UnusedSocialTokenProvider : SocialTokenProvider {
    override suspend fun requestToken(activity: Activity, provider: SocialProvider): SocialTokenResult =
        SocialTokenResult.Cancelled
}

fun apiError(code: String) = DataResult.Failure(DataError.ApiError(code = code, details = null, traceId = null))

fun testMe(importPending: Boolean = false) = Me(
    id = "11111111-1111-1111-1111-111111111111",
    accountType = AccountType.MEMBER,
    nickname = "닉네임",
    timeZone = "Asia/Seoul",
    providers = listOf(SocialProvider.GOOGLE),
    hasProfileImage = false,
    createdAt = Instant.parse("2026-10-12T00:00:00Z"),
    space = MeSpace(
        id = "22222222-2222-2222-2222-222222222222",
        type = SpaceType.PERSONAL,
        myColor = "orange",
        partner = null,
        togetherColor = null,
        startDate = null,
        importPending = importPending,
    ),
)
