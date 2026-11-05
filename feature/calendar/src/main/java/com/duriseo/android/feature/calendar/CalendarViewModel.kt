package com.duriseo.android.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.StartDateRules
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.event.Event
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.event.EventRules
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import javax.inject.Inject

/**
 * SCR-CAL-01 캘린더 홈. 화면은 Room의 내 정보와 일정만 구독하고(NFR-OFF-01), 보이는 달은 화면에 들어올 때와
 * 달을 넘길 때 `GET /events`로 다시 불러온다 (NFR-SYNC-02, 오프라인이면 조용히 실패).
 * 쓰기(처음 만난 날 저장)는 온라인에서만 한다 (NFR-OFF-02).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val meRepository: MeRepository,
    private val coupleRepository: CoupleRepository,
    private val clock: Clock,
) : ViewModel() {

    private data class Local(
        val visibleMonth: YearMonth,
        val selectedDate: LocalDate,
        val expanded: Boolean = false,
        val startDateSheet: CalendarStartDateSheet? = null,
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

    val uiState: StateFlow<CalendarUiState> = combine(
        local,
        meRepository.observeMe(),
        monthEvents,
    ) { local, me, events ->
        val today = LocalDate.now(clock)
        CalendarUiState(
            header = me?.toHeader(today) ?: CalendarHeader.Loading,
            today = today,
            visibleMonth = local.visibleMonth,
            selectedDate = local.selectedDate,
            expanded = local.expanded,
            eventsByDay = groupByDay(local.visibleMonth, events, me?.id.orEmpty(), zone),
            canGoPrevious = EventRules.previousMonth(local.visibleMonth) != null,
            canGoNext = EventRules.nextMonth(local.visibleMonth) != null,
            startDateSheet = local.startDateSheet,
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
            CalendarAction.PreviousMonth -> moveMonth(forward = false)
            CalendarAction.NextMonth -> moveMonth(forward = true)
            is CalendarAction.DateSelected -> selectDate(action.date)
            CalendarAction.ToggleExpanded -> local.update { it.copy(expanded = !it.expanded) }
            is CalendarAction.EventClicked -> effectChannel.trySend(CalendarSideEffect.OpenEvent(action.id))
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

    /** 오프라인·실패는 조용히 넘기고 Room의 값을 그대로 보여 준다 (ARCHITECTURE.md 2장) */
    private fun refreshVisibleMonth() {
        val month = local.value.visibleMonth
        viewModelScope.launch { eventRepository.refreshRange(month.atDay(1), month.atEndOfMonth(), zone) }
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
                is DataResult.Success -> local.update { it.copy(startDateSheet = null) }
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
        dayNumber = space.startDate?.let { StartDateRules.dayNumber(it, today) },
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
        result[date] = EventRules.sortForDay(onDay).map { it.toCalendarEvent(myId, zone) }
    }
    return result
}

private fun Event.toCalendarEvent(myId: String, zone: ZoneId) = CalendarEvent(
    id = id,
    title = title,
    note = note,
    isAllDay = isAllDay,
    startTime = (time as? EventTime.Timed)?.start?.atZone(zone)?.toLocalTime(),
    target = EventRules.targetOf(this, myId),
)
