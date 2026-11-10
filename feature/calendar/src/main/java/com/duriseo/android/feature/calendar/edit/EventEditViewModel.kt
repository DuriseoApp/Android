package com.duriseo.android.feature.calendar.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duriseo.android.core.common.navigation.CalendarRouteModel
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.event.Event
import com.duriseo.android.domain.event.EventPatch
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.event.EventRules
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.event.NewEvent
import com.duriseo.android.domain.event.NoteChange
import com.duriseo.android.domain.event.ReminderOption
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import com.duriseo.android.core.designsystem.R as DesignR

/**
 * SCR-CAL-03 일정 작성·수정. 입력은 화면 안에서만 들고 있고, [저장]은 온라인에서만 한다 (NFR-OFF-02).
 * 저장이 성공해야 Room이 바뀐다 (낙관적 갱신 없음).
 *
 * - 새 일정의 기본값 (D-114, Q-17): 날짜는 캘린더에서 고른 날, 시간은 기기 시계의 다음 정시부터 1시간.
 * - 알림은 일정을 만든 뒤 따로 정한다. 알림 설정이 실패해도 일정은 남기고 오류 안내만 한 뒤 닫는다 (D-108).
 */
@HiltViewModel
class EventEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<CalendarRouteModel.EventEdit>()
    private val eventId: String? = route.eventId
    private val zone: ZoneId get() = clock.zone

    private val _uiState = MutableStateFlow(EventEditUiState(isEdit = eventId != null, today = LocalDate.now(clock)))
    val uiState: StateFlow<EventEditUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<EventEditSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    /** 수정 화면의 원래 값. 바뀐 필드만 보내려고 들고 있다 */
    private var original: Event? = null
    private var myId: String = ""

    init {
        viewModelScope.launch { initialize() }
    }

    private suspend fun initialize() {
        val me = meRepository.observeMe().first()
        if (me == null) {
            effectChannel.trySend(EventEditSideEffect.Close)
            return
        }
        myId = me.id
        val isCouple = me.space.type == SpaceType.COUPLE && me.space.partner != null
        val defaults = defaultTimes(route.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() })
        if (eventId == null) {
            _uiState.update {
                it.copy(
                    loaded = true,
                    isCouple = isCouple,
                    startDate = defaults.startDate,
                    startTime = defaults.startTime,
                    endDate = defaults.endDate,
                    endTime = defaults.endTime,
                ).recomputed()
            }
            return
        }
        val event = eventRepository.observeEvent(eventId).first()
        if (event == null || !EventRules.canEdit(event, myId)) {
            // 사라졌거나 수정 권한이 없는 일정이다 (직접 열 수 없는 경로)
            effectChannel.trySend(EventEditSideEffect.ShowMessage(DesignR.string.error_common))
            effectChannel.trySend(EventEditSideEffect.Close)
            return
        }
        original = event
        val t = event.time
        val start = (t as? EventTime.Timed)?.start?.atZone(zone)
        val end = (t as? EventTime.Timed)?.end?.atZone(zone)
        _uiState.update {
            it.copy(
                loaded = true,
                isCouple = isCouple,
                targetReadOnly = !EventRules.canChangeTarget(event, myId),
                title = event.title,
                note = event.note.orEmpty(),
                isAllDay = event.isAllDay,
                startDate = start?.toLocalDate() ?: (t as EventTime.AllDay).start,
                startTime = start?.toLocalTime() ?: defaults.startTime,
                endDate = end?.toLocalDate() ?: (t as EventTime.AllDay).end,
                endTime = end?.toLocalTime() ?: defaults.endTime,
                together = event.isTogether,
                reminder = event.reminder?.option,
            ).recomputed()
        }
    }

    private data class Defaults(val startDate: LocalDate, val startTime: LocalTime, val endDate: LocalDate, val endTime: LocalTime)

    /**
     * 선택한 날의 기기 시각 다음 정시부터 1시간 (D-114, FR-CAL-02 AC8).
     * 다음 정시가 다음 날(23시대)이면 시작은 선택한 날 23:00, 종료가 다음 날로 넘어가면(22시대 이후) 종료는 선택한 날 23:59 (Q-17 기본값).
     */
    private fun defaultTimes(date: LocalDate?): Defaults {
        val day = date?.takeIf { EventRules.isSelectable(it) } ?: LocalDate.now(clock)
        val nowHour = LocalDateTime.now(clock).hour
        val start = LocalTime.of(minOf(nowHour + 1, LAST_HOUR), 0)
        val end = if (start.hour >= LAST_HOUR) LocalTime.of(LAST_HOUR, LAST_MINUTE) else start.plusHours(1)
        return Defaults(day, start, day, end)
    }

    fun onAction(action: EventEditAction) {
        val s = _uiState.value
        if (!s.loaded || s.saving) {
            if (action == EventEditAction.CloseClicked && !s.saving) effectChannel.trySend(EventEditSideEffect.Close)
            return
        }
        when (action) {
            is EventEditAction.TitleChanged ->
                _uiState.update { it.copy(title = EventRules.limit(action.text, EventRules.TITLE_MAX)).recomputed() }
            is EventEditAction.NoteChanged ->
                _uiState.update { it.copy(note = EventRules.limit(action.text, EventRules.NOTE_MAX)) }
            is EventEditAction.AllDayChanged -> setAllDay(action.allDay)
            is EventEditAction.TogetherSelected -> if (s.showTargetSelector) _uiState.update { it.copy(together = action.together) }
            is EventEditAction.NotifyPartnerChanged -> _uiState.update { it.copy(notifyPartner = action.on) }
            EventEditAction.ColorChangeClicked -> if (s.showTargetSelector) effectChannel.trySend(EventEditSideEffect.OpenColors)

            is EventEditAction.DateRowClicked -> openDateSheet(action.field)
            is EventEditAction.TimeRowClicked -> if (!s.isAllDay) {
                val time = if (action.field == EditField.Start) s.startTime else s.endTime
                _uiState.update { it.copy(sheet = EditSheet.TimePicker(action.field, time)) }
            }
            EventEditAction.ReminderRowClicked -> _uiState.update { it.copy(sheet = EditSheet.Reminder) }
            is EventEditAction.ReminderSelected -> _uiState.update {
                // 칩을 눌러도 시트는 열려 있고 [완료]가 닫는다 (그림 `SCR-CAL-03--reminder-sheet.png`)
                it.copy(reminder = action.option.takeIf { option -> option == null || option.forAllDay == it.isAllDay })
            }

            EventEditAction.DatePreviousMonth -> moveSheetMonth(forward = false)
            EventEditAction.DateNextMonth -> moveSheetMonth(forward = true)
            is EventEditAction.DateMonthSelected -> updateDateSheet { it.copy(visibleMonth = EventRules.clampMonth(action.month)) }
            is EventEditAction.DatePicked -> if (EventRules.isSelectable(action.date)) updateDateSheet { it.copy(selected = action.date) }
            EventEditAction.DateConfirmed -> confirmDate()
            is EventEditAction.TimeConfirmed -> confirmTime(action.time)
            EventEditAction.SheetDismissed -> _uiState.update { it.copy(sheet = null) }

            EventEditAction.SaveClicked -> save()
            EventEditAction.CloseClicked -> effectChannel.trySend(EventEditSideEffect.Close)
        }
    }

    private fun setAllDay(allDay: Boolean) {
        _uiState.update { state ->
            // 종일 <-> 시간 전환 때 시간 값은 마지막 값을 유지한다 (D-114). 알림 선택지가 달라 고른 알림은 "없음"으로 돌린다 (D-107)
            state.copy(isAllDay = allDay, reminder = null).recomputed()
        }
    }

    private fun openDateSheet(field: EditField) {
        val s = _uiState.value
        val date = if (field == EditField.Start) s.startDate else s.endDate
        _uiState.update { it.copy(sheet = EditSheet.DatePicker(field, YearMonth.from(date), date)) }
    }

    private fun updateDateSheet(block: (EditSheet.DatePicker) -> EditSheet.DatePicker) {
        _uiState.update { state ->
            val sheet = state.sheet as? EditSheet.DatePicker ?: return@update state
            state.copy(sheet = block(sheet))
        }
    }

    private fun moveSheetMonth(forward: Boolean) = updateDateSheet { sheet ->
        val next = if (forward) EventRules.nextMonth(sheet.visibleMonth) else EventRules.previousMonth(sheet.visibleMonth)
        if (next == null) sheet else sheet.copy(visibleMonth = next)
    }

    private fun confirmDate() {
        _uiState.update { state ->
            val sheet = state.sheet as? EditSheet.DatePicker ?: return@update state
            val next = if (sheet.field == EditField.Start) {
                state.copy(startDate = sheet.selected, sheet = null)
            } else {
                state.copy(endDate = sheet.selected, sheet = null)
            }
            next.recomputed()
        }
    }

    private fun confirmTime(time: LocalTime) {
        _uiState.update { state ->
            val sheet = state.sheet as? EditSheet.TimePicker ?: return@update state
            val next = if (sheet.field == EditField.Start) {
                state.copy(startTime = time, sheet = null)
            } else {
                state.copy(endTime = time, sheet = null)
            }
            next.recomputed()
        }
    }

    // --- 저장 ---

    private fun EventEditUiState.toEventTime(): EventTime = if (isAllDay) {
        EventTime.AllDay(startDate, endDate)
    } else {
        EventTime.Timed(
            start = LocalDateTime.of(startDate, startTime).atZone(zone).toInstant(),
            end = LocalDateTime.of(endDate, endTime).atZone(zone).toInstant(),
        )
    }

    /** 종료 오류와 [저장] 활성 (FR-CAL-02 AC1·AC3): 제목이 있고 종료가 시작보다 뒤(종일은 같아도 됨)여야 한다 */
    private fun EventEditUiState.recomputed(): EventEditUiState {
        val endError = !EventRules.isEndValid(toEventTime())
        return copy(
            endError = endError,
            saveEnabled = EventRules.isTitleValid(title) && !endError,
        )
    }

    private fun save() {
        val s = _uiState.value
        if (!s.saveEnabled) return
        _uiState.update { it.copy(saving = true, loading = true) }
        viewModelScope.launch {
            val before = original
            if (before == null) create(s) else edit(s, before)
        }
    }

    private suspend fun create(s: EventEditUiState) {
        val note = s.note.takeIf { it.isNotBlank() }
        val result = eventRepository.create(
            NewEvent(
                title = EventRules.normalizeTitle(s.title),
                note = note,
                time = s.toEventTime(),
                isTogether = if (s.isCouple) s.together else null,
                notifyPartner = if (s.isCouple) s.notifyPartner else null,
            ),
        )
        when (result) {
            is DataResult.Failure -> fail(result)
            is DataResult.Success -> {
                if (s.reminder != null) {
                    val reminderResult = eventRepository.setReminder(result.value.id, s.reminder)
                    if (reminderResult is DataResult.Failure) {
                        // 일정은 이미 저장됐다. 남기고 공통 오류 안내만 한 뒤 닫는다 (D-108)
                        effectChannel.trySend(EventEditSideEffect.ShowMessage(reminderResult.error.toMessageRes()))
                    }
                }
                finishLoading()
                effectChannel.trySend(EventEditSideEffect.Close)
            }
        }
    }

    private suspend fun edit(s: EventEditUiState, before: Event) {
        val time = s.toEventTime()
        val title = EventRules.normalizeTitle(s.title)
        val note = s.note.takeIf { it.isNotBlank() }
        val patch = EventPatch(
            title = title.takeIf { it != before.title },
            note = when {
                note == before.note -> NoteChange.Keep
                note == null -> NoteChange.Clear
                else -> NoteChange.Set(note)
            },
            time = time.takeIf { it != before.time },
            // 대상은 작성자만 바꾼다. 상대가 수정할 때는 보내지 않는다 (R-13)
            isTogether = s.together.takeIf { EventRules.canChangeTarget(before, myId) && it != before.isTogether },
        )
        when (val result = eventRepository.update(before.id, patch)) {
            is DataResult.Failure -> return fail(result)
            is DataResult.Success -> Unit
        }
        // 종일을 바꿔 저장하면 서버가 알림을 없음으로 돌린다 (FR-CAL-05 AC4, D-91)
        val serverReminder = if (time.isAllDay != before.isAllDay) null else before.reminder?.option
        if (s.reminder != serverReminder) {
            val reminderResult = eventRepository.setReminder(before.id, s.reminder)
            if (reminderResult is DataResult.Failure) {
                effectChannel.trySend(EventEditSideEffect.ShowMessage(reminderResult.error.toMessageRes()))
            }
        }
        finishLoading()
        effectChannel.trySend(EventEditSideEffect.Close)
    }

    /**
     * 요청이 끝났다: 로딩 덮개를 걷는다 (D-115). 결과 안내와 닫기는 그 뒤에 보낸다.
     * `saving`은 화면이 닫힐 때까지 켜 두어 닫히는 동안 입력과 [저장]을 막는다.
     */
    private fun finishLoading() {
        _uiState.update { it.copy(loading = false) }
    }

    /**
     * 저장 실패 (D-111, D-121): 어떤 코드든 공통 오류 안내. 일정이 없다고 하면(404) 저장소가 Room에서 지웠으므로 화면을 닫고,
     * 권한이 없다고 하면(403 NOT_AUTHOR) 저장소가 그 일정을 `GET /events/{id}`로 다시 불러오고 이 수정 화면을 닫아 상세로
     * 돌아간다. 문구는 모두 공통 오류다 (새 문구 없음)
     */
    private fun fail(result: DataResult.Failure) {
        _uiState.update { it.copy(saving = false, loading = false) }
        effectChannel.trySend(EventEditSideEffect.ShowMessage(result.error.toMessageRes()))
        if (result.error.hasCode(ErrorCode.NOT_FOUND) || result.error.hasCode(ErrorCode.NOT_AUTHOR)) {
            effectChannel.trySend(EventEditSideEffect.Close)
        }
    }
}

private const val LAST_HOUR = 23
private const val LAST_MINUTE = 59
