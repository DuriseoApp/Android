package com.duriseo.android.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.anniversary.AnniversaryKind
import com.duriseo.android.domain.anniversary.AnniversaryOccurrence
import com.duriseo.android.domain.anniversary.AnniversaryRepository
import com.duriseo.android.domain.anniversary.DayCount
import com.duriseo.android.domain.anniversary.calendarMonthRange
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.StartDateRules
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.event.Event
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.event.EventRules
import com.duriseo.android.domain.event.EventTarget
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.memo.Memo
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.memo.MemoTexts
import com.duriseo.android.domain.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * SCR-CAL-01 캘린더 홈. 화면은 Room의 내 정보·일정·기념일·메모만 구독하고(NFR-OFF-01), 보이는 달은 화면에 들어올 때,
 * 달을 넘길 때, 당겨서 새로고침할 때 `GET /events`와 `GET /anniversaries`와 `GET /memos/dated`로 다시 불러온다 (NFR-SYNC-02, Q-16,
 * 오프라인이면 조용히 실패). 월 조회는 앞뒤 하루 여유를 둔다 (Q-19 ②).
 * 쓰기(처음 만난 날 저장)는 온라인에서만 한다 (NFR-OFF-02).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val anniversaryRepository: AnniversaryRepository,
    private val memoRepository: MemoRepository,
    private val meRepository: MeRepository,
    private val coupleRepository: CoupleRepository,
    private val clock: Clock,
) : ViewModel() {

    private data class Local(
        val visibleMonth: YearMonth,
        val selectedDate: LocalDate,
        val expanded: Boolean = false,
        val startDateSheet: CalendarStartDateSheet? = null,
        val refreshing: Boolean = false,
    )

    private val zone: ZoneId get() = clock.zone

    /** FR-CAL-01 AC1: 앱을 열면 오늘이 속한 달, 오늘 날짜 선택 */
    private val local = MutableStateFlow(
        LocalDate.now(clock).let { Local(visibleMonth = YearMonth.from(it), selectedDate = it) },
    )

    private val monthEvents = local
        .map { it.visibleMonth }
        .distinctUntilChanged()
        .flatMapLatest { month ->
            eventRepository.observeEvents(month.atDay(1), month.atEndOfMonth(), zone)
        }

    private val monthAnniversaries = local
        .map { it.visibleMonth }
        .distinctUntilChanged()
        .flatMapLatest { month ->
            val (from, to) = calendarMonthRange(month)
            anniversaryRepository.observeRange(from, to)
        }

    /** 메모 날짜가 보이는 달(앞뒤 하루 포함)에 있는 메모 (D-119) */
    private val monthMemos = local
        .map { it.visibleMonth }
        .distinctUntilChanged()
        .flatMapLatest { month ->
            val (from, to) = calendarMonthRange(month)
            memoRepository.observeDated(from, to)
        }

    val uiState: StateFlow<CalendarUiState> = combine(
        local,
        meRepository.observeMe(),
        monthEvents,
        monthAnniversaries,
        monthMemos,
    ) { local, me, events, anniversaries, memos ->
        val today = LocalDate.now(clock)
        CalendarUiState(
            header = me?.toHeader(today) ?: CalendarHeader.Loading,
            today = today,
            visibleMonth = local.visibleMonth,
            selectedDate = local.selectedDate,
            expanded = local.expanded,
            eventsByDay = groupByDay(local.visibleMonth, events, me?.id.orEmpty(), zone),
            anniversariesByDay = groupAnniversariesByDay(
                local.visibleMonth,
                anniversaries,
                me?.id.orEmpty(),
                me?.space?.startDate,
            ),
            memosByDay = groupMemosByDay(local.visibleMonth, memos, me?.id.orEmpty()),
            canGoPrevious = EventRules.previousMonth(local.visibleMonth) != null,
            canGoNext = EventRules.nextMonth(local.visibleMonth) != null,
            startDateSheet = local.startDateSheet,
            refreshing = local.refreshing,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        LocalDate.now(clock).let { CalendarUiState(today = it, visibleMonth = YearMonth.from(it), selectedDate = it) },
    )

    private val effectChannel = Channel<CalendarSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onAction(action: CalendarAction) {
        when (action) {
            CalendarAction.Shown -> refreshVisibleMonth()
            CalendarAction.Refresh -> pullToRefresh()
            CalendarAction.AnniversariesClicked -> effectChannel.trySend(CalendarSideEffect.OpenAnniversaries)
            CalendarAction.PreviousMonth -> moveMonth(forward = false)
            CalendarAction.NextMonth -> moveMonth(forward = true)
            is CalendarAction.DateSelected -> selectDate(action.date)
            CalendarAction.ToggleExpanded -> local.update { it.copy(expanded = !it.expanded) }
            is CalendarAction.EventClicked -> effectChannel.trySend(CalendarSideEffect.OpenEvent(action.id))
            is CalendarAction.MemoClicked -> effectChannel.trySend(CalendarSideEffect.OpenMemo(action.id))
            CalendarAction.AddClicked ->
                effectChannel.trySend(CalendarSideEffect.OpenCreate(local.value.selectedDate))
            CalendarAction.StartDatePromptClicked -> openStartDateSheet()
            CalendarAction.StartDatePreviousMonth -> moveStartDateMonth(forward = false)
            CalendarAction.StartDateNextMonth -> moveStartDateMonth(forward = true)
            is CalendarAction.StartDateSelected -> selectStartDate(action.date)
            is CalendarAction.StartDateMonthSelected -> selectStartMonth(action.month)
            CalendarAction.StartDateSaved -> saveStartDate()
            CalendarAction.StartDateSheetDismissed -> {
                if (local.value.startDateSheet?.saving != true) local.update { it.copy(startDateSheet = null) }
            }
        }
    }

    /**
     * 보이는 달의 일정과 기념일과 메모를 함께 다시 불러온다. 앞뒤 하루 여유를 두고 요청한다 (Q-19 ②, 3개월 상한 안).
     * 오프라인·실패는 조용히 넘기고 Room의 값을 그대로 보여 준다 (ARCHITECTURE.md 2장)
     */
    private fun refreshVisibleMonth(): Job {
        val (from, to) = calendarMonthRange(local.value.visibleMonth)
        // 저장소가 쓰기 뒤에 다시 불러올 범위는 보이는 달이다 (요청이 끝나기를 기다리지 않고 바로 알린다)
        anniversaryRepository.setVisibleRange(from, to)
        memoRepository.setVisibleRange(from, to)
        // 달을 빨리 넘기면 이전 달 요청은 취소한다. 겹치는 이틀을 오래된 응답이 덮지 않게
        refreshJob?.cancel()
        return viewModelScope.launch {
            launch { eventRepository.refreshRange(from, to, zone) }
            launch { anniversaryRepository.refreshRange(from, to) }
            launch { memoRepository.refreshDated(from, to) }
        }.also { refreshJob = it }
    }

    private var refreshJob: Job? = null

    /** 당겨서 새로고침 (Q-16): 다시 불러오는 동안 표시를 켠다. 요청 중 또 당겨도 한 번만 한다 */
    private fun pullToRefresh() {
        if (local.value.refreshing) return
        local.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            refreshVisibleMonth().join()
            local.update { it.copy(refreshing = false) }
        }
    }

    private fun moveMonth(forward: Boolean) {
        local.update { current ->
            val next = if (forward) EventRules.nextMonth(current.visibleMonth) else EventRules.previousMonth(current.visibleMonth)
            if (next == null) {
                current
            } else {
                // 선택한 날은 새 달 안에 있어야 한다: 같은 날짜가 있으면 그 날, 없으면 말일
                val day = current.selectedDate.dayOfMonth.coerceAtMost(next.lengthOfMonth())
                current.copy(visibleMonth = next, selectedDate = next.atDay(day))
            }
        }
        refreshVisibleMonth()
    }

    private fun selectDate(date: LocalDate) {
        if (!EventRules.isSelectable(date)) return
        local.update { it.copy(selectedDate = date, visibleMonth = YearMonth.from(date)) }
        // 다른 달의 날짜를 골라 보이는 달이 바뀌어도 저장소가 아는 보이는 범위는 따라간다
        val (from, to) = calendarMonthRange(local.value.visibleMonth)
        anniversaryRepository.setVisibleRange(from, to)
        memoRepository.setVisibleRange(from, to)
    }

    // --- 처음 만난 날 시트 (FR-COUPLE-04 AC2, 설정 화면 시트와 같은 규칙) ---

    private fun openStartDateSheet() {
        viewModelScope.launch {
            val me = meRepository.observeMe().first() ?: return@launch
            if (me.space.type != SpaceType.COUPLE) return@launch
            val today = LocalDate.now(clock)
            local.update {
                it.copy(
                    startDateSheet = CalendarStartDateSheet(
                        visibleMonth = StartDateRules.initialMonth(me.space.startDate, today),
                        selected = me.space.startDate,
                    ),
                )
            }
        }
    }

    private fun moveStartDateMonth(forward: Boolean) {
        val today = LocalDate.now(clock)
        local.update { current ->
            val sheet = current.startDateSheet ?: return@update current
            val next = if (forward) StartDateRules.nextMonth(sheet.visibleMonth, today) else StartDateRules.previousMonth(sheet.visibleMonth)
            if (next == null) current else current.copy(startDateSheet = sheet.copy(visibleMonth = next))
        }
    }

    private fun selectStartMonth(month: YearMonth) {
        if (!StartDateRules.isMonthSelectable(month, LocalDate.now(clock))) return
        local.update { current ->
            val sheet = current.startDateSheet ?: return@update current
            if (sheet.saving) current else current.copy(startDateSheet = sheet.copy(visibleMonth = month))
        }
    }

    private fun selectStartDate(date: LocalDate) {
        if (!StartDateRules.isSelectable(date, LocalDate.now(clock))) return
        local.update { current ->
            val sheet = current.startDateSheet ?: return@update current
            if (sheet.saving) current else current.copy(startDateSheet = sheet.copy(selected = date))
        }
    }

    /** [저장]: `PATCH /space`. 성공하면 시트를 닫고 Room이 바뀐 D+N을 보여 준다 (FR-COUPLE-04 AC1) */
    private fun saveStartDate() {
        val sheet = local.value.startDateSheet ?: return
        val date = sheet.selected ?: return
        if (sheet.saving) return
        local.update { it.copy(startDateSheet = sheet.copy(saving = true)) }
        viewModelScope.launch {
            when (val result = coupleRepository.updateStartDate(date)) {
                is DataResult.Success -> {
                    local.update { it.copy(startDateSheet = null) }
                    // 자동 기념일은 처음 만난 날로 서버가 계산한다: 보이는 달의 기념일을 다시 불러온다 (FR-ANNIV-02 AC3)
                    val (from, to) = calendarMonthRange(local.value.visibleMonth)
                    anniversaryRepository.refreshRange(from, to)
                }
                is DataResult.Failure -> {
                    // 실패·오프라인이면 시트를 그대로 두고 안내한다. 상대가 먼저 해제했으면(NOT_COUPLE) 앱 초기화가 이동을 맡는다 (D-103)
                    local.update { it.copy(startDateSheet = sheet.copy(saving = false)) }
                    if (!result.error.hasCode(ErrorCode.NOT_COUPLE)) {
                        effectChannel.trySend(CalendarSideEffect.ShowMessage(result.error.toMessageRes()))
                    }
                }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun Me.toHeader(today: LocalDate): CalendarHeader {
    val partner = space.partner
    if (space.type != SpaceType.COUPLE || partner == null) return CalendarHeader.Solo(space.myColor)
    return CalendarHeader.Couple(
        myColorKey = space.myColor,
        partnerColorKey = partner.color,
        dayNumber = space.startDate?.let { DayCount.of(it, today) },
    )
}

/** 보이는 달의 날짜마다 걸친 일정을 모은다. 여러 날 일정은 모든 날에 들어간다 (FR-CAL-02 AC5) */
internal fun groupByDay(
    month: YearMonth,
    events: List<Event>,
    myId: String,
    zone: ZoneId,
): Map<LocalDate, List<CalendarEvent>> {
    val result = LinkedHashMap<LocalDate, List<CalendarEvent>>()
    for (day in 1..month.lengthOfMonth()) {
        val date = month.atDay(day)
        val onDay = events.filter { EventRules.occursOn(it, date, zone) }
        if (onDay.isEmpty()) continue
        result[date] = sortForCalendarDay(onDay, zone).map { it.toCalendarEvent(myId, zone) }
    }
    return result
}

/**
 * 날짜 목록의 일정 순서 (D-119): 종일(시작 날짜순) -> 시간(목록 카드에 보이는 시각순, 같으면 시작 일시순). 같으면 id순.
 * 여러 날 일정은 걸친 날마다 시작 시각이 보이므로 그 시각으로 정렬한다 (D-113).
 */
internal fun sortForCalendarDay(events: List<Event>, zone: ZoneId): List<Event> {
    val allDay = events.filter { it.isAllDay }.sortedWith(
        compareBy<Event>({ (it.time as? EventTime.AllDay)?.start ?: LocalDate.MIN }, { it.id }),
    )
    val timed = events.filterNot { it.isAllDay }.sortedWith(
        compareBy<Event>(
            { (it.time as? EventTime.Timed)?.start?.atZone(zone)?.toLocalTime()?.truncatedTo(ChronoUnit.MINUTES) },
            { (it.time as? EventTime.Timed)?.start },
            { it.id },
        ),
    )
    return allDay + timed
}

/**
 * 보이는 달의 날짜마다 기념일을 모은다. 저장소의 순서(서버 응답 순서)를 그대로 쓴다 (D-119, FR-ANNIV-04 AC5).
 * 앞뒤 하루 여유로 받은 이웃 달 날짜는 달력에 그리지 않으므로 뺀다.
 * 자동 기념일 카드의 D+N은 날짜 − 처음 만난 날 + 1이다 (D-117).
 */
internal fun groupAnniversariesByDay(
    month: YearMonth,
    occurrences: List<AnniversaryOccurrence>,
    myId: String,
    startDate: LocalDate?,
): Map<LocalDate, List<CalendarAnniversary>> =
    occurrences
        .filter { YearMonth.from(it.date) == month }
        .groupBy { it.date }
        .mapValues { (date, list) -> list.map { it.toCalendarAnniversary(myId, startDate, date) } }

/**
 * 보이는 달의 날짜마다 메모를 모은다 (메모 날짜가 있는 메모만). 순서는 작성 최신순, 같으면 id순 (D-119).
 * 앞뒤 하루 여유로 받은 이웃 달 날짜는 달력에 그리지 않으므로 뺀다. 제목은 목록 제목 규칙이다 (D-131).
 */
internal fun groupMemosByDay(
    month: YearMonth,
    memos: List<Memo>,
    myId: String,
): Map<LocalDate, List<CalendarMemo>> =
    memos
        .sortedWith(compareByDescending<Memo> { it.createdAt }.thenBy { it.id })
        .mapNotNull { memo -> memo.memoDate?.takeIf { YearMonth.from(it) == month }?.let { it to memo } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, list) ->
            list.map {
                CalendarMemo(
                    id = it.id,
                    title = MemoTexts.listTitle(it),
                    author = if (it.authorId == myId) EventTarget.ME else EventTarget.PARTNER,
                )
            }
        }

private fun AnniversaryOccurrence.toCalendarAnniversary(myId: String, startDate: LocalDate?, date: LocalDate) =
    CalendarAnniversary(
        name = name,
        isAuto = kind == AnniversaryKind.AUTO,
        dayNumber = if (kind == AnniversaryKind.AUTO) startDate?.let { DayCount.onDate(it, date) } else null,
        author = if (kind == AnniversaryKind.AUTO) null else if (authorId == myId) EventTarget.ME else EventTarget.PARTNER,
        repeatsYearly = repeatsYearly,
    )

private fun Event.toCalendarEvent(myId: String, zone: ZoneId) = CalendarEvent(
    id = id,
    title = title,
    note = note,
    isAllDay = isAllDay,
    startTime = (time as? EventTime.Timed)?.start?.atZone(zone)?.toLocalTime(),
    target = EventRules.targetOf(this, myId),
)
