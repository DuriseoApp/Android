package com.duriseo.android.feature.settings

import com.duriseo.android.domain.me.SpaceChange
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MePartner
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

    val changes = MutableSharedFlow<SpaceChange>(extraBufferCapacity = 4)
    override val spaceChanges: Flow<SpaceChange> = changes

    val sheetNeeded = MutableStateFlow(false)
    override fun observeLinkedSheetNeeded(): Flow<Boolean> = sheetNeeded

    var acknowledgeCalls = 0
    override suspend fun acknowledgeSpace() {
        acknowledgeCalls++
        sheetNeeded.value = false
    }

    var resetCalls = 0
    override suspend fun resetAfterUnlink() {
        resetCalls++
    }
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

    /** 설정하면 logout이 이 신호를 받을 때까지 멈춘다 (요청 중 상태를 확인하려고) */
    var logoutGate: CompletableDeferred<Unit>? = null

    override suspend fun logout(): DataResult<Unit> {
        logoutCalls++
        logoutGate?.await()
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

/** 커플 공간의 내 정보. 처음 만난 날과 연결한 시각을 고를 수 있다 */
fun coupleMe(startDate: LocalDate? = LocalDate.of(2026, 10, 12), linkedAt: Instant? = Instant.parse("2026-10-12T03:00:00Z")) =
    memberMe(partnerNickname = "지우").let { me ->
        me.copy(space = me.space.copy(startDate = startDate, linkedAt = linkedAt))
    }

/** 설정 화면 테스트용 가짜 CoupleRepository. 쓰는 것만 기록하고 나머지는 쓰지 않는다 */
class FakeCoupleRepository : CoupleRepository {
    val startDateCalls = mutableListOf<LocalDate>()
    var startDateResult: DataResult<Unit> = DataResult.Success(Unit)
    val colorCalls = mutableListOf<ColorChange>()
    var colorResult: DataResult<Unit> = DataResult.Success(Unit)
    var unlinkCalls = 0
    var unlinkResult: DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun currentInviteCode(): DataResult<InviteCode?> = error("설정 화면은 쓰지 않아요")
    override suspend fun issueInviteCode(): DataResult<InviteCode> = error("설정 화면은 쓰지 않아요")
    override suspend fun join(code: String): DataResult<CoupleJoinResult> = error("설정 화면은 쓰지 않아요")
    override suspend fun decideImport(import: Boolean): DataResult<Unit> = error("설정 화면은 쓰지 않아요")

    /** 설정하면 해당 요청이 이 신호를 받을 때까지 멈춘다 (요청 중 상태를 확인하려고) */
    var startDateGate: CompletableDeferred<Unit>? = null
    var colorGate: CompletableDeferred<Unit>? = null
    var unlinkGate: CompletableDeferred<Unit>? = null

    override suspend fun updateStartDate(date: LocalDate): DataResult<Unit> {
        startDateCalls += date
        startDateGate?.await()
        return startDateResult
    }

    override suspend fun updateColors(change: ColorChange): DataResult<Unit> {
        colorCalls += change
        colorGate?.await()
        return colorResult
    }

    override suspend fun unlink(): DataResult<Unit> {
        unlinkCalls++
        unlinkGate?.await()
        return unlinkResult
    }
}
