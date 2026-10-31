package com.duriseo.android.feature.couple

import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MePartner
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** 가상 시간(`TestCoroutineScheduler`)과 같이 흐르는 시계. `advanceTimeBy`가 시계도 앞으로 보낸다 */
@OptIn(ExperimentalCoroutinesApi::class)
class SchedulerClock(
    private val scheduler: TestCoroutineScheduler,
    private val base: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = base.plusMillis(scheduler.currentTime)
}

/** 화면 쪽 ViewModel 테스트용 가짜 CoupleRepository. 부른 횟수와 인자를 기록한다 */
class FakeCoupleRepository : CoupleRepository {
    /** 호출마다 하나씩 꺼내 쓴다. 비면 [defaultCurrent] */
    val currentResults = ArrayDeque<DataResult<InviteCode?>>()
    var defaultCurrent: DataResult<InviteCode?> = DataResult.Success(null)
    val issueResults = ArrayDeque<DataResult<InviteCode>>()
    var defaultIssue: DataResult<InviteCode> = DataResult.Failure(apiErrorOf("INTERNAL_ERROR"))

    var currentCalls = 0
    var issueCalls = 0

    var joinResult: DataResult<CoupleJoinResult> = DataResult.Success(CoupleJoinResult("space-1", false, false))
    val joinCalls = mutableListOf<String>()

    /** 설정하면 join이 이 신호를 받을 때까지 멈춘다 (요청 중 상태를 확인하려고) */
    var joinGate: CompletableDeferred<Unit>? = null

    val importCalls = mutableListOf<Boolean>()
    val startDateCalls = mutableListOf<LocalDate>()
    val colorCalls = mutableListOf<ColorChange>()
    var colorResult: DataResult<Unit> = DataResult.Success(Unit)
    var unlinkCalls = 0

    override suspend fun currentInviteCode(): DataResult<InviteCode?> {
        currentCalls++
        return currentResults.removeFirstOrNull() ?: defaultCurrent
    }

    override suspend fun issueInviteCode(): DataResult<InviteCode> {
        issueCalls++
        return issueResults.removeFirstOrNull() ?: defaultIssue
    }

    override suspend fun join(code: String): DataResult<CoupleJoinResult> {
        joinCalls += code
        joinGate?.await()
        return joinResult
    }

    override suspend fun decideImport(import: Boolean): DataResult<Unit> {
        importCalls += import
        return DataResult.Success(Unit)
    }

    override suspend fun updateStartDate(date: LocalDate): DataResult<Unit> {
        startDateCalls += date
        return DataResult.Success(Unit)
    }

    override suspend fun updateColors(change: ColorChange): DataResult<Unit> {
        colorCalls += change
        return colorResult
    }

    override suspend fun unlink(): DataResult<Unit> {
        unlinkCalls++
        return DataResult.Success(Unit)
    }
}

class FakeMeRepository(initial: Me? = null) : MeRepository {
    val me = MutableStateFlow(initial)

    var refreshCalls = 0
    var refreshResult: DataResult<Unit> = DataResult.Success(Unit)

    /** refreshMe가 불릴 때마다 실행한다. 서버에서 상대가 연결한 것처럼 [me]를 바꾸는 데 쓴다 */
    var onRefresh: (FakeMeRepository) -> Unit = {}

    override fun observeMe(): Flow<Me?> = me

    override suspend fun refreshMe(): DataResult<Unit> {
        refreshCalls++
        onRefresh(this)
        return refreshResult
    }

    override suspend fun syncTimeZone(zoneId: String): DataResult<Unit> = DataResult.Success(Unit)
}

/** 커플 연결 화면은 로그아웃만 쓴다 */
class FakeAuthRepository : AuthRepository {
    override val authState = MutableStateFlow(AuthState.SignedIn)
    override val sessionEvents: Flow<SessionEndReason> = MutableSharedFlow()

    var logoutResult: DataResult<Unit> = DataResult.Success(Unit)
    var logoutCalls = 0

    /** 설정하면 logout이 이 신호를 받을 때까지 멈춘다 */
    var logoutGate: CompletableDeferred<Unit>? = null

    override suspend fun restoreSession() = Unit

    override suspend fun startAsGuest(): DataResult<LoginResult> = error("커플 연결 화면은 쓰지 않아요")

    override suspend fun signIn(provider: SocialProvider, token: String): DataResult<LoginResult> =
        error("커플 연결 화면은 쓰지 않아요")

    override suspend fun link(provider: SocialProvider, token: String): DataResult<Unit> =
        error("커플 연결 화면은 쓰지 않아요")

    override suspend fun switchAccount(provider: SocialProvider, token: String): DataResult<LoginResult> =
        error("커플 연결 화면은 쓰지 않아요")

    override suspend fun logout(): DataResult<Unit> {
        logoutCalls++
        logoutGate?.await()
        return logoutResult
    }

    override suspend fun withdraw(): DataResult<Unit> = error("커플 연결 화면은 쓰지 않아요")
}

fun apiErrorOf(code: String, details: ErrorDetails? = null) = DataError.ApiError(code = code, details = details, traceId = null)

fun failure(code: String, details: ErrorDetails? = null) = DataResult.Failure(apiErrorOf(code, details))

fun offline() = DataResult.Failure(DataError.Offline)

fun testMe(
    type: SpaceType = SpaceType.PERSONAL,
    accountType: AccountType = AccountType.MEMBER,
) = Me(
    id = "11111111-1111-1111-1111-111111111111",
    accountType = accountType,
    nickname = "하나",
    timeZone = "Asia/Seoul",
    providers = listOf(SocialProvider.GOOGLE),
    hasProfileImage = false,
    createdAt = Instant.parse("2026-10-12T00:00:00Z"),
    space = MeSpace(
        id = "22222222-2222-2222-2222-222222222222",
        type = type,
        myColor = "pick-orange",
        partner = if (type == SpaceType.COUPLE) MePartner("33333333-3333-3333-3333-333333333333", "둘", "pick-blue", false) else null,
        togetherColor = if (type == SpaceType.COUPLE) "pick-purple" else null,
        startDate = null,
        importPending = false,
    ),
)

/** 효과 흐름을 모아 둔다. 본문을 멈추지 않으므로 가상 시간이 저절로 흐르지 않는다 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.collectInto(flow: Flow<T>): MutableList<T> {
    val items = mutableListOf<T>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { items += it } }
    return items
}
