package com.duriseo.android.feature.calendar.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duriseo.android.core.common.navigation.CalendarRouteModel
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.event.Event
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.event.EventRules
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import com.duriseo.android.core.designsystem.R as DesignR

/**
 * SCR-CAL-02 일정 상세 (FR-CAL-03). 화면은 Room의 일정과 내 정보만 구독한다 (NFR-OFF-01).
 * 알림 변경·삭제는 온라인에서만 하고 성공한 뒤에 Room이 바뀐다 (NFR-OFF-02).
 */
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val eventId: String = savedStateHandle.toRoute<CalendarRouteModel.EventDetail>().eventId
    private val zone: ZoneId get() = clock.zone

    private data class Local(
        val deleteDialogVisible: Boolean = false,
        val deleting: Boolean = false,
        val reminderSaving: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    /** 한 번이라도 일정을 읽었는가. 그 뒤에 사라지면(상대가 지움 등) 화면을 닫는다 */
    private var everLoaded = false
    private var vanished = false

    val uiState: StateFlow<EventDetailUiState> = combine(
        eventRepository.observeEvent(eventId),
        meRepository.observeMe(),
        local,
    ) { event, me, local ->
        if (event == null || me == null) {
            if (event == null && everLoaded && !local.deleting) onVanished()
            EventDetailUiState(
                deleteDialogVisible = local.deleteDialogVisible,
                deleting = local.deleting,
                reminderSaving = local.reminderSaving,
            )
        } else {
            everLoaded = true
            EventDetailUiState(
                event = event.toDetail(me, zone),
                deleteDialogVisible = local.deleteDialogVisible,
                deleting = local.deleting,
                reminderSaving = local.reminderSaving,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EventDetailUiState())

    private val effectChannel = Channel<EventDetailSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onAction(action: EventDetailAction) {
        when (action) {
            EventDetailAction.Shown -> viewModelScope.launch {
                val result = eventRepository.refreshEvent(eventId)
                // 404면 저장소가 Room에서 지운다. 사라진 일정은 위 combine이 닫는다. 그 밖의 실패는 Room 값을 그대로 보여 준다
                if (result is DataResult.Failure && result.error.hasCode(ErrorCode.NOT_FOUND) && !everLoaded) {
                    onVanished()
                }
            }
            EventDetailAction.BackClicked -> effectChannel.trySend(EventDetailSideEffect.Close)
            EventDetailAction.EditClicked -> {
                if (uiState.value.event?.canEdit == true) effectChannel.trySend(EventDetailSideEffect.OpenEdit(eventId))
            }
            is EventDetailAction.ReminderSelected -> setReminder(action.option)
            EventDetailAction.DeleteClicked -> {
                if (uiState.value.event?.canEdit == true) local.update { it.copy(deleteDialogVisible = true) }
            }
            EventDetailAction.DeleteCancelled -> local.update { if (it.deleting) it else it.copy(deleteDialogVisible = false) }
            EventDetailAction.DeleteConfirmed -> delete()
        }
    }

    private fun onVanished() {
        // 한 번만 알린다 (같은 일정이 사라진 채 내 정보가 다시 오는 경우)
        if (vanished) return
        vanished = true
        effectChannel.trySend(EventDetailSideEffect.ShowMessage(DesignR.string.error_common))
        effectChannel.trySend(EventDetailSideEffect.Close)
    }

    private fun setReminder(option: com.duriseo.android.domain.event.ReminderOption?) {
        val current = uiState.value
        val event = current.event ?: return
        if (current.reminderSaving || event.myReminder == option) return
        local.update { it.copy(reminderSaving = true) }
        viewModelScope.launch {
            val result = eventRepository.setReminder(eventId, option)
            local.update { it.copy(reminderSaving = false) }
            if (result is DataResult.Failure) {
                effectChannel.trySend(EventDetailSideEffect.ShowMessage(result.error.toMessageRes()))
            }
        }
    }

    private fun delete() {
        val current = local.value
        if (current.deleting || uiState.value.event?.canEdit != true) return
        local.update { it.copy(deleting = true) }
        viewModelScope.launch {
            when (val result = eventRepository.delete(eventId)) {
                is DataResult.Success -> {
                    local.update { it.copy(deleteDialogVisible = false) }
                    effectChannel.trySend(EventDetailSideEffect.Close)
                }
                is DataResult.Failure -> {
                    local.update { it.copy(deleting = false, deleteDialogVisible = false) }
                    effectChannel.trySend(EventDetailSideEffect.ShowMessage(result.error.toMessageRes()))
                    // 이미 지워졌으면(404) 저장소가 Room에서도 지워서 화면이 닫힌다
                }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun Event.toDetail(me: Me, zone: ZoneId): EventDetail {
    val authorName = when (authorId) {
        me.id -> me.nickname
        me.space.partner?.id -> me.space.partner?.nickname.orEmpty()
        else -> ""
    }
    val t = time
    val start = (t as? EventTime.Timed)?.start?.atZone(zone)
    val end = (t as? EventTime.Timed)?.end?.atZone(zone)
    return EventDetail(
        id = id,
        title = title,
        note = note,
        isAllDay = isAllDay,
        startDate = start?.toLocalDate() ?: (t as EventTime.AllDay).start,
        startTime = start?.toLocalTime(),
        endDate = end?.toLocalDate() ?: (t as EventTime.AllDay).end,
        endTime = end?.toLocalTime(),
        target = EventRules.targetOf(this, me.id),
        authorName = authorName,
        myReminder = reminder?.option,
        canEdit = EventRules.canEdit(this, me.id),
    )
}
