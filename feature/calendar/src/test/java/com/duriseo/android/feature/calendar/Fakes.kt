package com.duriseo.android.feature.calendar

import com.duriseo.android.domain.anniversary.Anniversary
import com.duriseo.android.domain.anniversary.AnniversaryKind
import com.duriseo.android.domain.anniversary.AnniversaryOccurrence
import com.duriseo.android.domain.anniversary.AnniversaryPatch
import com.duriseo.android.domain.anniversary.AnniversaryRepository
import com.duriseo.android.domain.anniversary.NewAnniversary
import com.duriseo.android.domain.anniversary.PastAnniversaries
import com.duriseo.android.domain.anniversary.UpcomingAnniversaries
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.event.Event
import com.duriseo.android.domain.event.EventPatch
import com.duriseo.android.domain.event.EventReminder
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.event.EventRules
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.event.NewEvent
import com.duriseo.android.domain.event.ReminderOption
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MePartner
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SpaceChange
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.memo.Memo
import com.duriseo.android.domain.memo.MemoList
import com.duriseo.android.domain.memo.MemoPatch
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.domain.memo.NewMemo
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

fun sampleEvent(
    id: String = "e1",
    title: String = "저녁 데이트",
    note: String? = null,
    time: EventTime = EventTime.Timed(Instant.parse("2026-10-12T10:30:00Z"), Instant.parse("2026-10-12T12:00:00Z")),
    isTogether: Boolean = false,
    authorId: String = MyId,
    reminder: ReminderOption? = null,
) = Event(
    id = id,
    title = title,
    note = note,
    time = time,
    isTogether = isTogether,
    authorId = authorId,
    updatedBy = null,
    reminder = reminder?.let { EventReminder(it, Instant.parse("2026-10-12T10:00:00Z")) },
    createdAt = Instant.parse("2026-10-01T00:00:00Z"),
    updatedAt = Instant.parse("2026-10-01T00:00:00Z"),
)

fun allDay(start: LocalDate, end: LocalDate = start) = EventTime.AllDay(start, end)

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

    override suspend fun currentInviteCode(): DataResult<InviteCode?> = error("캘린더는 쓰지 않아요")
    override suspend fun issueInviteCode(): DataResult<InviteCode> = error("캘린더는 쓰지 않아요")
    override suspend fun join(code: String): DataResult<CoupleJoinResult> = error("캘린더는 쓰지 않아요")
    override suspend fun decideImport(import: Boolean): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun updateColors(change: ColorChange): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun unlink(): DataResult<Unit> = error("캘린더는 쓰지 않아요")

    /** 설정하면 updateStartDate가 이 신호를 받을 때까지 멈춘다 (요청 중 상태를 확인하려고) */
    var startDateGate: CompletableDeferred<Unit>? = null

    override suspend fun updateStartDate(date: LocalDate): DataResult<Unit> {
        startDateCalls += date
        startDateGate?.await()
        return startDateResult
    }
}

/** Room처럼 동작하는 가짜 저장소: 쓰기가 성공하면 [events]가 바뀌고 Flow가 따라온다 */
class FakeEventRepository(initial: List<Event> = emptyList()) : EventRepository {
    val events = MutableStateFlow(initial)

    val refreshRanges = mutableListOf<Pair<LocalDate, LocalDate>>()
    val refreshedIds = mutableListOf<String>()
    val created = mutableListOf<NewEvent>()
    val patches = mutableListOf<Pair<String, EventPatch>>()
    val deleted = mutableListOf<String>()
    val reminderCalls = mutableListOf<Pair<String, ReminderOption?>>()

    var createResult: DataResult<Unit> = DataResult.Success(Unit)
    var updateResult: DataResult<Unit> = DataResult.Success(Unit)
    var deleteResult: DataResult<Unit> = DataResult.Success(Unit)
    var reminderResult: DataResult<Unit> = DataResult.Success(Unit)
    var refreshEventResult: DataResult<Unit> = DataResult.Success(Unit)
    var refreshRangeResult: DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun forget(id: String) {
        events.value = events.value.filterNot { it.id == id }
    }

    override fun observeEvents(from: LocalDate, to: LocalDate, zone: ZoneId): Flow<List<Event>> =
        events.map { list ->
            list.filter { e ->
                EventRules.firstDay(e, zone) <= to && EventRules.lastDay(e, zone) >= from
            }
        }

    override fun observeEvent(id: String): Flow<Event?> = events.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refreshRange(from: LocalDate, to: LocalDate, zone: ZoneId): DataResult<Unit> {
        refreshRanges += from to to
        return refreshRangeResult
    }

    override suspend fun refreshEvent(id: String): DataResult<Unit> {
        refreshedIds += id
        return refreshEventResult
    }

    override suspend fun create(event: NewEvent): DataResult<Event> {
        created += event
        (createResult as? DataResult.Failure)?.let { return it }
        val made = sampleEvent(
            id = "new-${created.size}",
            title = event.title,
            note = event.note,
            time = event.time,
            isTogether = event.isTogether == true,
        )
        events.value = events.value + made
        return DataResult.Success(made)
    }

    /** 설정하면 update가 이 신호를 받을 때까지 멈춘다 (요청 중 상태를 확인하려고) */
    var updateGate: CompletableDeferred<Unit>? = null

    override suspend fun update(id: String, patch: EventPatch): DataResult<Unit> {
        patches += id to patch
        updateGate?.await()
        // 저장소 계약: 403 NOT_AUTHOR면 그 일정을 다시 읽는다 (D-111, D-121)
        if ((updateResult as? DataResult.Failure)?.error?.hasCode(ErrorCode.NOT_AUTHOR) == true) refreshedIds += id
        return updateResult
    }

    /** 설정하면 delete가 이 신호를 받을 때까지 멈춘다 */
    var deleteGate: CompletableDeferred<Unit>? = null

    override suspend fun delete(id: String): DataResult<Unit> {
        deleted += id
        deleteGate?.await()
        if (deleteResult is DataResult.Success) events.value = events.value.filterNot { it.id == id }
        return deleteResult
    }

    /** 설정하면 setReminder가 이 신호를 받을 때까지 멈춘다 */
    var reminderGate: CompletableDeferred<Unit>? = null

    override suspend fun setReminder(id: String, option: ReminderOption?): DataResult<Unit> {
        reminderCalls += id to option
        reminderGate?.await()
        if (reminderResult is DataResult.Success) {
            events.value = events.value.map {
                if (it.id == id) it.copy(reminder = option?.let { o -> EventReminder(o, Instant.parse("2026-10-12T10:00:00Z")) }) else it
            }
        }
        return reminderResult
    }
}

/** 캘린더가 쓰는 [observeRange]·[refreshRange]만 흉내 낸다. 나머지는 SCR-ANNIV-01·02의 몫이라 부르면 실패한다 */
class FakeAnniversaryRepository(initial: List<AnniversaryOccurrence> = emptyList()) : AnniversaryRepository {
    /** 저장소가 기억하는 위치 순서 그대로의 발생일 목록 */
    val occurrences = MutableStateFlow(initial)

    val refreshRanges = mutableListOf<Pair<LocalDate, LocalDate>>()

    /** 요청 도중 취소된 범위 */
    val cancelledRanges = mutableListOf<Pair<LocalDate, LocalDate>>()
    var refreshRangeResult: DataResult<Unit> = DataResult.Success(Unit)

    /** 설정하면 [refreshRange]가 이 값이 끝날 때까지 기다린다 (당겨서 새로고침 중 상태를 보려고) */
    var refreshGate: CompletableDeferred<Unit>? = null

    /** [setVisibleRange]로 알려 온 범위들 (마지막이 지금 보이는 범위) */
    val visibleRanges = mutableListOf<Pair<LocalDate, LocalDate>>()

    override fun setVisibleRange(from: LocalDate, to: LocalDate) {
        visibleRanges += from to to
    }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<AnniversaryOccurrence>> =
        occurrences.map { list -> list.filter { it.date >= from && it.date <= to }.sortedBy { it.date } }

    override suspend fun refreshRange(from: LocalDate, to: LocalDate): DataResult<Unit> {
        refreshRanges += from to to
        try {
            refreshGate?.await()
        } catch (e: kotlinx.coroutines.CancellationException) {
            cancelledRanges += from to to
            throw e
        }
        return refreshRangeResult
    }

    override fun observeUpcoming(): Flow<UpcomingAnniversaries> = error("캘린더는 쓰지 않아요")
    override fun observePast(): Flow<PastAnniversaries> = error("캘린더는 쓰지 않아요")
    override suspend fun refreshUpcoming(): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun loadMoreUpcoming(): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun refreshPast(): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun loadMorePast(): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun refreshAll(): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun create(anniversary: NewAnniversary): DataResult<Anniversary> = error("캘린더는 쓰지 않아요")
    override suspend fun update(id: String, patch: AnniversaryPatch): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun delete(id: String): DataResult<Unit> = error("캘린더는 쓰지 않아요")
}

fun autoAnniversary(name: String, date: LocalDate) = AnniversaryOccurrence(
    anniversaryId = null,
    kind = AnniversaryKind.AUTO,
    name = name,
    date = date,
    daysLeft = 0,
    repeatsYearly = false,
    authorId = null,
)

fun customAnniversary(
    name: String,
    date: LocalDate,
    authorId: String = MyId,
    repeatsYearly: Boolean = true,
) = AnniversaryOccurrence(
    anniversaryId = "ann-$name",
    kind = AnniversaryKind.CUSTOM,
    name = name,
    date = date,
    daysLeft = 0,
    repeatsYearly = repeatsYearly,
    authorId = authorId,
)

/** 캘린더가 쓰는 [observeDated]·[refreshDated]·[setVisibleRange]만 흉내 낸다. 나머지는 메모 화면의 몫이라 부르면 실패한다 */
class FakeMemoRepository(initial: List<Memo> = emptyList()) : MemoRepository {
    val memos = MutableStateFlow(initial)

    val refreshRanges = mutableListOf<Pair<LocalDate, LocalDate>>()
    val visibleRanges = mutableListOf<Pair<LocalDate, LocalDate>>()
    var refreshDatedResult: DataResult<Unit> = DataResult.Success(Unit)

    override fun setVisibleRange(from: LocalDate, to: LocalDate) {
        visibleRanges += from to to
    }

    override fun observeDated(from: LocalDate, to: LocalDate): Flow<List<Memo>> =
        memos.map { list -> list.filter { m -> m.memoDate?.let { it >= from && it <= to } == true } }

    override suspend fun refreshDated(from: LocalDate, to: LocalDate): DataResult<Unit> {
        refreshRanges += from to to
        return refreshDatedResult
    }

    override suspend fun forget(id: String): Unit = error("캘린더는 쓰지 않아요")
    override fun observeList(view: MemoView): Flow<MemoList> = error("캘린더는 쓰지 않아요")
    override fun observeFavoriteCount(): Flow<Int> = error("캘린더는 쓰지 않아요")
    override fun observePinned(): Flow<List<Memo>> = error("캘린더는 쓰지 않아요")
    override fun observePinnedLoaded(): Flow<Boolean> = error("캘린더는 쓰지 않아요")
    override fun observeMemo(id: String): Flow<Memo?> = error("캘린더는 쓰지 않아요")
    override suspend fun refreshList(view: MemoView): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun loadMoreList(view: MemoView): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun refreshPinned(): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun refreshMemo(id: String): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun refreshAll(): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun create(memo: NewMemo): DataResult<Memo> = error("캘린더는 쓰지 않아요")
    override suspend fun update(id: String, patch: MemoPatch): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun delete(id: String): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun setPinned(id: String, pinned: Boolean): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun setFavorite(id: String, favorite: Boolean): DataResult<Unit> = error("캘린더는 쓰지 않아요")
}

fun sampleMemo(
    id: String,
    title: String? = "제목 $id",
    body: String? = null,
    memoDate: LocalDate? = LocalDate.of(2026, 10, 12),
    authorId: String = MyId,
    createdAt: Instant = Instant.parse("2026-10-11T01:00:00Z"),
) = Memo(
    id = id,
    title = title,
    body = body,
    memoDate = memoDate,
    authorId = authorId,
    isPinned = false,
    pinnedAt = null,
    isFavorite = false,
    createdAt = createdAt,
    updatedAt = createdAt,
)
