package com.duriseo.android.feature.settings

import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MePartner
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate

class FakeMeRepository(initial: Me? = null) : MeRepository {
    val me = MutableStateFlow(initial)

    override fun observeMe(): Flow<Me?> = me

    var refreshCalls = 0
    var refreshResult: DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun refreshMe(): DataResult<Unit> {
        refreshCalls++
        return refreshResult
    }

    override suspend fun syncTimeZone(zoneId: String): DataResult<Unit> = DataResult.Success(Unit)
}

/**
 * 설정 화면 테스트용 가짜 AuthRepository. 연동이 성공하면 실제 리포지토리가 `GET /me`를 다시 읽어 Room을 바꾸는 것처럼
 * [meRepository]의 내 정보를 회원으로 바꾼다.
 */
class FakeAuthRepository(private val meRepository: FakeMeRepository) : AuthRepository {
    override val authState = MutableStateFlow(AuthState.SignedIn)
    override val sessionEvents: Flow<SessionEndReason> = MutableSharedFlow()

    var linkResult: DataResult<Unit> = DataResult.Success(Unit)

    /** 앞에서부터 하나씩 쓴다. 비면 성공 */
    val switchResults = ArrayDeque<DataResult<LoginResult>>()
    var logoutResult: DataResult<Unit> = DataResult.Success(Unit)
    var withdrawResult: DataResult<Unit> = DataResult.Success(Unit)

    val linkCalls = mutableListOf<Pair<SocialProvider, String>>()
    val switchCalls = mutableListOf<Pair<SocialProvider, String>>()
    var logoutCalls = 0
    var withdrawCalls = 0

    override suspend fun restoreSession() = Unit

    override suspend fun startAsGuest(): DataResult<LoginResult> = error("설정 화면은 쓰지 않아요")

    override suspend fun signIn(provider: SocialProvider, token: String): DataResult<LoginResult> =
        error("설정 화면은 쓰지 않아요")

    override suspend fun link(provider: SocialProvider, token: String): DataResult<Unit> {
        linkCalls += provider to token
        if (linkResult is DataResult.Success) {
            meRepository.me.value = meRepository.me.value?.copy(
                accountType = AccountType.MEMBER,
                providers = listOf(provider),
            )
        }
        return linkResult
    }

    override suspend fun switchAccount(provider: SocialProvider, token: String): DataResult<LoginResult> {
        switchCalls += provider to token
        return switchResults.removeFirstOrNull() ?: DataResult.Success(LoginResult(isNewUser = false))
    }

    override suspend fun logout(): DataResult<Unit> {
        logoutCalls++
        return logoutResult
    }

    override suspend fun withdraw(): DataResult<Unit> {
        withdrawCalls++
        return withdrawResult
    }
}

fun apiError(code: String) = DataResult.Failure(DataError.ApiError(code = code, details = null, traceId = null))

fun socialToken(provider: SocialProvider, token: String = "social-token") =
    SocialTokenResult.Success(provider, token)

const val GuestId = "11111111-1111-1111-1111-111111111111"

fun guestMe() = Me(
    id = GuestId,
    accountType = AccountType.GUEST,
    nickname = "게스트",
    timeZone = "Asia/Seoul",
    providers = emptyList(),
    hasProfileImage = false,
    createdAt = Instant.parse("2026-10-12T00:00:00Z"),
    space = MeSpace(
        id = "22222222-2222-2222-2222-222222222222",
        type = SpaceType.PERSONAL,
        myColor = "orange",
        partner = null,
        togetherColor = null,
        startDate = null,
        importPending = false,
    ),
)

fun memberMe(
    provider: SocialProvider = SocialProvider.GOOGLE,
    createdAt: Instant = Instant.parse("2026-10-12T00:00:00Z"),
    partnerNickname: String? = null,
) = guestMe().copy(
    accountType = AccountType.MEMBER,
    nickname = "닉네임",
    providers = listOf(provider),
    createdAt = createdAt,
    space = guestMe().space.let { space ->
        if (partnerNickname == null) {
            space
        } else {
            space.copy(
                type = SpaceType.COUPLE,
                partner = MePartner(
                    id = "33333333-3333-3333-3333-333333333333",
                    nickname = partnerNickname,
                    color = "blue",
                    hasProfileImage = false,
                ),
                togetherColor = "purple",
                startDate = LocalDate.of(2026, 1, 1),
            )
        }
    },
)
