package com.duriseo.android.feature.calendar

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
import com.duriseo.android.domain.result.DataResult
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

    override suspend fun updateStartDate(date: LocalDate): DataResult<Unit> {
        startDateCalls += date
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

    override fun observeEvents(from: LocalDate, to: LocalDate, zone: ZoneId): Flow<List<Event>> =
        events.map { list ->
            list.filter { e ->
                EventRules.firstDay(e, zone) <= to && EventRules.lastDay(e, zone) >= from
            }
        }

    override fun observeEvent(id: String): Flow<Event?> = events.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refreshRange(from: LocalDate, to: LocalDate, zone: ZoneId): DataResult<Unit> {
        refreshRanges += from to to
        return DataResult.Success(Unit)
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

    override suspend fun updateColors(change: ColorChange): DataResult<Unit> = error("캘린더는 쓰지 않아요")
    override suspend fun unlink(): DataResult<Unit> = error("캘린더는 쓰지 않아요")

    override suspend fun updateStartDate(date: LocalDate): DataResult<Unit> {
        startDateCalls += date
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

    override fun observeEvents(from: LocalDate, to: LocalDate, zone: ZoneId): Flow<List<Event>> =
        events.map { list ->
            list.filter { e ->
                EventRules.firstDay(e, zone) <= to && EventRules.lastDay(e, zone) >= from
            }
        }

    override fun observeEvent(id: String): Flow<Event?> = events.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refreshRange(from: LocalDate, to: LocalDate, zone: ZoneId): DataResult<Unit> {
        refreshRanges += from to to
        return DataResult.Success(Unit)
    }

    override suspend fun refreshEvent(id: String): DataResult<Unit> {
        refreshedIds += id
        return refreshEventResult
    }

    override suspend fun create(event: NewEvent): DataResult<Event> {
        created += event
        val failure = createResult as? DataResult.Failure ?: return DataResult.Success(
            event(id = "new-${created.size}", title = event.title, note = event.note, time = event.time, isTogether = event.isTogether == true)
                .also { events.value = events.value + it },
        )
        return failure
    }

    override suspend fun update(id: String, patch: EventPatch): DataResult<Unit> {
        patches += id to patch
        return updateResult
    }

    override suspend fun delete(id: String): DataResult<Unit> {
        deleted += id
        if (deleteResult is DataResult.Success) events.value = events.value.filterNot { it.id == id }
        return deleteResult
    }

    override suspend fun setReminder(id: String, option: ReminderOption?): DataResult<Unit> {
        reminderCalls += id to option
        if (reminderResult is DataResult.Success) {
            events.value = events.value.map {
                if (it.id == id) it.copy(reminder = option?.let { o -> EventReminder(o, Instant.parse("2026-10-12T10:00:00Z")) }) else it
            }
        }
        return reminderResult
    }
}
