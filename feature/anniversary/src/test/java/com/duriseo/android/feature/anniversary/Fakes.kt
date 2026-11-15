package com.duriseo.android.feature.anniversary

import com.duriseo.android.domain.anniversary.Anniversary
import com.duriseo.android.domain.anniversary.AnniversaryKind
import com.duriseo.android.domain.anniversary.AnniversaryOccurrence
import com.duriseo.android.domain.anniversary.AnniversaryPatch
import com.duriseo.android.domain.anniversary.AnniversaryRepository
import com.duriseo.android.domain.anniversary.NewAnniversary
import com.duriseo.android.domain.anniversary.PastAnniversaries
import com.duriseo.android.domain.anniversary.PastAnniversary
import com.duriseo.android.domain.anniversary.UpcomingAnniversaries
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MePartner
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SpaceChange
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate

const val MyId = "11111111-1111-1111-1111-111111111111"
const val PartnerId = "33333333-3333-3333-3333-333333333333"

fun apiError(code: String) = DataResult.Failure(DataError.ApiError(code = code, details = null, traceId = null))

val Offline = DataResult.Failure(DataError.Offline)

fun soloMe() = Me(
    id = MyId,
    accountType = AccountType.MEMBER,
    nickname = "민지",
    timeZone = "Asia/Seoul",
    providers = emptyList(),
    hasProfileImage = false,
    createdAt = Instant.parse("2026-10-01T00:00:00Z"),
    space = MeSpace(
        id = "22222222-2222-2222-2222-222222222222",
        type = SpaceType.PERSONAL,
        myColor = "pick-orange",
        partner = null,
        togetherColor = null,
        startDate = null,
        importPending = false,
    ),
)

fun coupleMe(startDate: LocalDate? = LocalDate.of(2026, 4, 7)): Me = soloMe().let { me ->
    me.copy(
        space = me.space.copy(
            type = SpaceType.COUPLE,
            partner = MePartner(id = PartnerId, nickname = "지우", color = "pick-blue", hasProfileImage = false),
            togetherColor = "pick-purple",
            startDate = startDate,
        ),
    )
}

fun auto(name: String, date: LocalDate, daysLeft: Int = 10) = AnniversaryOccurrence(
    anniversaryId = null,
    kind = AnniversaryKind.AUTO,
    name = name,
    date = date,
    daysLeft = daysLeft,
    repeatsYearly = false,
    authorId = null,
)

fun custom(
    id: String,
    name: String,
    date: LocalDate,
    daysLeft: Int = 10,
    yearly: Boolean = true,
    authorId: String = MyId,
) = AnniversaryOccurrence(
    anniversaryId = id,
    kind = AnniversaryKind.CUSTOM,
    name = name,
    date = date,
    daysLeft = daysLeft,
    repeatsYearly = yearly,
    authorId = authorId,
)

fun past(id: String, name: String, date: LocalDate, authorId: String = MyId) = PastAnniversary(id, name, date, authorId)

class FakeMeRepository(initial: Me? = null) : MeRepository {
    val me = MutableStateFlow(initial)
    override fun observeMe(): Flow<Me?> = me
    override suspend fun refreshMe(): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun syncTimeZone(zoneId: String): DataResult<Unit> = DataResult.Success(Unit)
    override val spaceChanges: Flow<SpaceChange> = MutableSharedFlow()
    override fun observeLinkedSheetNeeded(): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun acknowledgeSpace() = Unit
    override suspend fun resetAfterUnlink() = Unit
}

class FakeCoupleRepository : CoupleRepository {
    val startDateCalls = mutableListOf<LocalDate>()
    var startDateResult: DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun currentInviteCode(): DataResult<InviteCode?> = error("기념일은 쓰지 않아요")
    override suspend fun issueInviteCode(): DataResult<InviteCode> = error("기념일은 쓰지 않아요")
    override suspend fun join(code: String): DataResult<CoupleJoinResult> = error("기념일은 쓰지 않아요")
    override suspend fun decideImport(import: Boolean): DataResult<Unit> = error("기념일은 쓰지 않아요")
    override suspend fun updateColors(change: ColorChange): DataResult<Unit> = error("기념일은 쓰지 않아요")
    override suspend fun unlink(): DataResult<Unit> = error("기념일은 쓰지 않아요")

    /** 설정하면 updateStartDate가 이 신호를 받을 때까지 멈춘다 (요청 중 상태를 확인하려고) */
    var startDateGate: CompletableDeferred<Unit>? = null

    override suspend fun updateStartDate(date: LocalDate): DataResult<Unit> {
        startDateCalls += date
        startDateGate?.await()
        return startDateResult
    }
}

/** Room처럼 동작하는 가짜 저장소: [upcoming]·[past]가 바뀌면 Flow가 따라온다. 요청은 횟수와 인자를 기록한다 */
class FakeAnniversaryRepository(
    upcoming: List<AnniversaryOccurrence> = emptyList(),
    pastItems: List<PastAnniversary> = emptyList(),
    hasMoreUpcoming: Boolean = false,
    hasMorePast: Boolean = false,
) : AnniversaryRepository {
    val upcoming = MutableStateFlow(UpcomingAnniversaries(upcoming, hasMoreUpcoming))
    val past = MutableStateFlow(PastAnniversaries(pastItems, pastItems.size, hasMorePast))

    var refreshUpcomingCalls = 0
    var refreshPastCalls = 0
    var refreshAllCalls = 0
    var loadMoreUpcomingCalls = 0
    var loadMorePastCalls = 0
    val created = mutableListOf<NewAnniversary>()
    val patches = mutableListOf<Pair<String, AnniversaryPatch>>()
    val deleted = mutableListOf<String>()

    var createResult: DataResult<Unit> = DataResult.Success(Unit)
    var updateResult: DataResult<Unit> = DataResult.Success(Unit)
    var deleteResult: DataResult<Unit> = DataResult.Success(Unit)

    override fun setVisibleRange(from: LocalDate, to: LocalDate) = Unit

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<AnniversaryOccurrence>> =
        MutableStateFlow(emptyList())

    override fun observeUpcoming(): Flow<UpcomingAnniversaries> = upcoming
    override fun observePast(): Flow<PastAnniversaries> = past

    override suspend fun refreshRange(from: LocalDate, to: LocalDate): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun refreshUpcoming(): DataResult<Unit> {
        refreshUpcomingCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun loadMoreUpcoming(): DataResult<Unit> {
        loadMoreUpcomingCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun refreshPast(): DataResult<Unit> {
        refreshPastCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun loadMorePast(): DataResult<Unit> {
        loadMorePastCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun refreshAll(): DataResult<Unit> {
        refreshAllCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun create(anniversary: NewAnniversary): DataResult<Anniversary> {
        created += anniversary
        (createResult as? DataResult.Failure)?.let { return it }
        return DataResult.Success(
            Anniversary(
                id = "new-${created.size}",
                name = anniversary.name,
                date = anniversary.date,
                repeatsYearly = anniversary.repeatsYearly ?: true,
                authorId = MyId,
                createdAt = Instant.parse("2026-10-12T00:00:00Z"),
                updatedAt = Instant.parse("2026-10-12T00:00:00Z"),
            ),
        )
    }

    override suspend fun update(id: String, patch: AnniversaryPatch): DataResult<Unit> {
        patches += id to patch
        return updateResult.alsoApplyServerAnswer(id)
    }

    /** 설정하면 delete가 이 신호를 받을 때까지 멈춘다 */
    var deleteGate: CompletableDeferred<Unit>? = null

    override suspend fun delete(id: String): DataResult<Unit> {
        deleted += id
        deleteGate?.await()
        return deleteResult.alsoApplyServerAnswer(id)
    }

    /**
     * 저장소의 약속(`AnniversaryRepository` 문서)을 흉내 낸다: 404면 기기에서 지우고, 403이면 다시 불러온다.
     * 진짜 동작은 `AnniversaryRepositoryTest`가 확인한다.
     */
    private fun DataResult<Unit>.alsoApplyServerAnswer(id: String): DataResult<Unit> {
        val code = ((this as? DataResult.Failure)?.error as? DataError.ApiError)?.code
        if (code == "NOT_FOUND") {
            upcoming.value = upcoming.value.copy(items = upcoming.value.items.filterNot { it.anniversaryId == id })
            past.value = past.value.copy(items = past.value.items.filterNot { it.anniversaryId == id })
        }
        if (code == "NOT_AUTHOR") refreshAllCalls++
        return this
    }
}

/** 알림 권한 "이미 물었는지" 가짜: 처음 한 번만 true (FR-NOTI-01 AC3) */
class FakeNotificationPermissionGate(private var asked: Boolean = false) : com.duriseo.android.domain.notification.NotificationPermissionGate {
    override suspend fun shouldAskNow(): Boolean {
        if (asked) return false
        asked = true
        return true
    }
}
