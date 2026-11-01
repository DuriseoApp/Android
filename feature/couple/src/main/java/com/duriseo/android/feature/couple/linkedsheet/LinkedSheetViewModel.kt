package com.duriseo.android.feature.couple.linkedsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.StartDateRules
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.me.ImportCounts
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
import java.time.LocalDate
import javax.inject.Inject

/**
 * SCR-COUPLE-04 연결 완료 시트 (FR-COUPLE-03, FR-COUPLE-04 AC4, API.md 4.4의 8·9·10단계, D-93, D-98).
 *
 * 시트를 띄울지는 `MeRepository.observeLinkedSheetNeeded()`가 정하고(호스트는 `feature:main`), 이 ViewModel은
 * 시트 안의 선택과 [시작하기] 순서만 맡는다.
 *
 * [시작하기] 순서 (D-98): ① 처음 만난 날을 새로 골랐으면 `PATCH /space` ② 기록이 있으면 `POST /couple/import`
 * ③ 확인한 공간 ID를 저장하고 시트를 닫는다. 중간에 실패하면 거기서 멈추고 공통 오류를 보여 주며 시트는 그대로다
 * (다시 누르면 처음부터. 같은 날짜의 `PATCH`는 무해하다). 되돌릴 수 없는 삭제를 마지막에 하려는 순서다.
 * ②가 409 `NO_IMPORT_PENDING`이면 이미 정한 것으로 보고 ③으로 간 뒤 `GET /me`를 다시 읽는다 (해제와 겹쳤으면 거기서 알게 된다).
 */
@HiltViewModel
class LinkedSheetViewModel @Inject constructor(
    private val coupleRepository: CoupleRepository,
    private val meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    /** 이 화면이 들고 있는 선택. 서버에서 오는 값(상대 이름, 이미 있는 날짜, 개수)은 Room에서 합친다 */
    private data class Local(
        val pickedDate: LocalDate? = null,
        val choice: ImportChoice = ImportChoice.Keep,
        val datePicker: StartDatePickerState? = null,
        val dropDialogVisible: Boolean = false,
        val requesting: Boolean = false,
        /** 요청 중에 Room이 바뀌어도(처음 만난 날·가져오기 응답 뒤 `GET /me`) 화면이 깜박이지 않게 붙들어 둔 값 */
        val frozen: Frozen? = null,
    )

    private data class Frozen(
        val hasRecords: Boolean,
        val counts: ImportCounts?,
        val serverDate: LocalDate?,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<LinkedSheetUiState> = combine(meRepository.observeMe(), local) { me, l ->
        val space = me?.space
        val frozen = l.frozen
        val serverDate = frozen?.serverDate ?: space?.startDate
        LinkedSheetUiState(
            partnerName = space?.partner?.nickname.orEmpty(),
            startDate = l.pickedDate ?: serverDate,
            serverStartDate = serverDate,
            hasRecords = frozen?.hasRecords ?: (space?.importPending == true),
            counts = frozen?.counts ?: space?.importCounts,
            choice = l.choice,
            datePicker = l.datePicker,
            dropDialogVisible = l.dropDialogVisible,
            requesting = l.requesting,
            today = today(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LinkedSheetUiState(today = today()))

    private val effectChannel = Channel<LinkedSheetSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private fun today(): LocalDate = LocalDate.now(clock)

    fun onAction(action: LinkedSheetAction) {
        when (action) {
            LinkedSheetAction.DateFieldClicked -> openDatePicker()
            LinkedSheetAction.PreviousMonth -> moveMonth(forward = false)
            LinkedSheetAction.NextMonth -> moveMonth(forward = true)
            is LinkedSheetAction.DateSelected -> selectDate(action.date)
            LinkedSheetAction.DateSaved -> saveDate()
            LinkedSheetAction.DatePickerDismissed -> local.update { it.copy(datePicker = null) }
            LinkedSheetAction.LaterClicked -> local.update { it.copy(pickedDate = null) }
            is LinkedSheetAction.ChoiceSelected -> local.update { if (it.requesting) it else it.copy(choice = action.choice) }
            LinkedSheetAction.StartClicked -> onStartClicked()
            LinkedSheetAction.DropConfirmed -> {
                local.update { it.copy(dropDialogVisible = false) }
                start()
            }
            LinkedSheetAction.DropCancelled -> local.update { it.copy(dropDialogVisible = false) }
            LinkedSheetAction.CloseClicked -> close()
            LinkedSheetAction.BackPressed -> onBack()
        }
    }

    private fun openDatePicker() {
        val state = uiState.value
        if (state.requesting) return
        val today = state.today
        local.update {
            it.copy(
                datePicker = StartDatePickerState(
                    visibleMonth = StartDateRules.initialMonth(state.startDate, today),
                    selected = state.startDate,
                ),
            )
        }
    }

    private fun moveMonth(forward: Boolean) {
        val today = today()
        local.update { current ->
            val picker = current.datePicker ?: return@update current
            val next = if (forward) {
                StartDateRules.nextMonth(picker.visibleMonth, today)
            } else {
                StartDateRules.previousMonth(picker.visibleMonth)
            }
            if (next == null) current else current.copy(datePicker = picker.copy(visibleMonth = next))
        }
    }

    private fun selectDate(date: LocalDate) {
        if (!StartDateRules.isSelectable(date, today())) return
        local.update { current ->
            val picker = current.datePicker ?: return@update current
            current.copy(datePicker = picker.copy(selected = date))
        }
    }

    /** 선택 시트의 [저장]: 칸만 채운다. 서버 저장은 [시작하기]에서 한다 (D-98) */
    private fun saveDate() {
        local.update { current ->
            val selected = current.datePicker?.selected
            current.copy(pickedDate = selected ?: current.pickedDate, datePicker = null)
        }
    }

    private fun onStartClicked() {
        val state = uiState.value
        if (state.requesting) return
        if (state.hasRecords && state.choice == ImportChoice.Drop) {
            // 되돌릴 수 없는 삭제라 먼저 묻는다 (FR-COUPLE-03 AC2)
            local.update { it.copy(dropDialogVisible = true) }
        } else {
            start()
        }
    }

    private fun start() {
        val state = uiState.value
        if (state.requesting) return
        val picked = local.value.pickedDate
        local.update {
            it.copy(requesting = true, frozen = Frozen(state.hasRecords, state.counts, state.serverStartDate))
        }
        viewModelScope.launch {
            // ① 처음 만난 날: 새로 골랐고 서버 값과 다를 때만
            if (picked != null && picked != state.serverStartDate) {
                val result = coupleRepository.updateStartDate(picked)
                if (result is DataResult.Failure) return@launch fail(result.error)
            }
            // ② 혼자 쓴 기록: 기록이 있을 때만
            var rereadMe = false
            if (state.hasRecords) {
                val result = coupleRepository.decideImport(state.choice == ImportChoice.Keep)
                if (result is DataResult.Failure) {
                    if (result.error.hasCode(ErrorCode.NO_IMPORT_PENDING)) {
                        rereadMe = true // 이미 정한 것으로 본다
                    } else {
                        return@launch fail(result.error)
                    }
                }
            }
            // ③ 이 연결을 확인했다고 기록하고 시트를 닫는다 (호스트가 `observeLinkedSheetNeeded()`로 내린다)
            meRepository.acknowledgeSpace()
            if (rereadMe) meRepository.refreshMe()
            local.value = Local()
        }
    }

    private fun close() {
        val state = uiState.value
        if (!state.canDismiss) return
        viewModelScope.launch {
            meRepository.acknowledgeSpace()
            local.value = Local()
        }
    }

    private fun onBack() {
        val current = local.value
        when {
            current.requesting -> Unit
            current.datePicker != null -> local.update { it.copy(datePicker = null) }
            current.dropDialogVisible -> local.update { it.copy(dropDialogVisible = false) }
            else -> close()
        }
    }

    private fun fail(error: DataError) {
        local.update { it.copy(requesting = false, frozen = null) }
        // 상대가 먼저 해제했으면(NOT_COUPLE) 리포지토리가 앱을 초기화했고 이동은 `feature:main`이 한다. 화면은 아무것도 하지 않는다
        if (error.hasCode(ErrorCode.NOT_COUPLE)) return
        effectChannel.trySend(LinkedSheetSideEffect.ShowMessage(error.toMessageRes()))
    }
}
