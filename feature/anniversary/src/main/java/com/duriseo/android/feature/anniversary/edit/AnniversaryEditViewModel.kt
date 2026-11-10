package com.duriseo.android.feature.anniversary.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duriseo.android.core.common.navigation.AnniversaryRouteModel
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.anniversary.AnniversaryPatch
import com.duriseo.android.domain.anniversary.AnniversaryRepository
import com.duriseo.android.domain.anniversary.AnniversaryRules
import com.duriseo.android.domain.anniversary.NewAnniversary
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.event.EventRules
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
import java.time.YearMonth
import javax.inject.Inject
import com.duriseo.android.core.designsystem.R as DesignR

/**
 * SCR-ANNIV-02 직접 기념일 작성·수정·삭제. 입력은 화면 안에서만 들고 있고, [저장]·삭제는 온라인에서만 한다 (NFR-OFF-02).
 * 서버가 성공해야 Room이 바뀐다 (낙관적 갱신 없음).
 *
 * - 새 기념일의 기본값 (D-122): 날짜는 오늘(기기 기준), "매년 반복"은 켜짐.
 * - 수정은 **바꾼 필드만** `PATCH`한다. 날짜 칸은 누른 발생일을 보여 주고, 사용자가 바꾸지 않으면 `date`를 보내지 않아
 *   등록한 날짜가 그대로다 (unit-5.md 확인 필요 1). 바꾼 것이 없으면 요청 없이 닫는다 (FR-ANNIV-03 AC6).
 * - 서버가 404(없음)·403(권한 없음)이라 하면 화면을 닫고 공통 오류를 보인다. 404는 저장소가 기기에서 지우고, 403은 저장소가
 *   목록·달을 다시 불러온다 (FR-ANNIV-03 AC5, D-121, 확인 필요 4).
 */
@HiltViewModel
class AnniversaryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val anniversaryRepository: AnniversaryRepository,
    private val meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AnniversaryRouteModel.Edit>()
    private val anniversaryId: String? = route.anniversaryId

    private val _uiState = MutableStateFlow(
        AnniversaryEditUiState(isEdit = anniversaryId != null, today = LocalDate.now(clock)),
    )
    val uiState: StateFlow<AnniversaryEditUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<AnniversaryEditSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    /** 수정 화면이 열릴 때의 값. 바뀐 필드만 보내려고 들고 있다 */
    private data class Original(val name: String, val date: LocalDate, val repeatsYearly: Boolean)

    private var original: Original? = null

    init {
        viewModelScope.launch { initialize() }
    }

    private suspend fun initialize() {
        val me = meRepository.observeMe().first()
        if (me == null) {
            effectChannel.trySend(AnniversaryEditSideEffect.Close)
            return
        }
        val isCouple = me.space.type == SpaceType.COUPLE && me.space.partner != null
        val today = LocalDate.now(clock)
        if (anniversaryId == null) {
            _uiState.update {
                it.copy(loaded = true, isCouple = isCouple, date = today, repeatsYearly = true).recomputed()
            }
            return
        }
        // 누른 항목을 Room에서 찾는다. 다가오는 목록에서는 누른 발생일까지 맞는 것, 지난 기념일에서는 id가 같은 것
        val routeDate = route.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val upcoming = anniversaryRepository.observeUpcoming().first().items
            .firstOrNull { it.anniversaryId == anniversaryId && (routeDate == null || it.date == routeDate) }
        val past = if (upcoming == null) {
            anniversaryRepository.observePast().first().items.firstOrNull { it.anniversaryId == anniversaryId }
        } else {
            null
        }
        val name = upcoming?.name ?: past?.name
        val date = upcoming?.date ?: past?.date
        val authorId = upcoming?.authorId ?: past?.authorId
        if (name == null || date == null || authorId != me.id) {
            // 사라졌거나 내가 만든 기념일이 아니다 (직접 열 수 없는 경로)
            effectChannel.trySend(AnniversaryEditSideEffect.ShowMessage(DesignR.string.error_common))
            effectChannel.trySend(AnniversaryEditSideEffect.Close)
            return
        }
        val repeatsYearly = upcoming?.repeatsYearly ?: false
        original = Original(name, date, repeatsYearly)
        _uiState.update {
            it.copy(
                loaded = true,
                isCouple = isCouple,
                name = name,
                originalName = name,
                date = date,
                repeatsYearly = repeatsYearly,
            ).recomputed()
        }
    }

    fun onAction(action: AnniversaryEditAction) {
        val s = _uiState.value
        if (!s.loaded || s.saving) {
            if (action == AnniversaryEditAction.CloseClicked && !s.saving) {
                effectChannel.trySend(AnniversaryEditSideEffect.Close)
            }
            return
        }
        when (action) {
            is AnniversaryEditAction.NameChanged ->
                _uiState.update { it.copy(name = AnniversaryRules.limitName(action.text)).recomputed() }
            is AnniversaryEditAction.YearlyChanged -> _uiState.update { it.copy(repeatsYearly = action.on) }
            AnniversaryEditAction.DateRowClicked ->
                _uiState.update { it.copy(dateSheet = DateSheet(YearMonth.from(it.date), it.date)) }

            AnniversaryEditAction.DatePreviousMonth -> moveSheetMonth(forward = false)
            AnniversaryEditAction.DateNextMonth -> moveSheetMonth(forward = true)
            is AnniversaryEditAction.DateMonthSelected ->
                updateDateSheet { it.copy(visibleMonth = EventRules.clampMonth(action.month)) }
            is AnniversaryEditAction.DatePicked ->
                if (AnniversaryRules.isSelectable(action.date)) updateDateSheet { it.copy(selected = action.date) }
            AnniversaryEditAction.DateConfirmed -> _uiState.update { state ->
                val sheet = state.dateSheet ?: return@update state
                state.copy(date = sheet.selected, dateSheet = null).recomputed()
            }
            AnniversaryEditAction.DateSheetDismissed -> _uiState.update { it.copy(dateSheet = null) }

            AnniversaryEditAction.SaveClicked -> save()
            AnniversaryEditAction.CloseClicked -> effectChannel.trySend(AnniversaryEditSideEffect.Close)

            AnniversaryEditAction.DeleteClicked -> if (s.isEdit) _uiState.update { it.copy(deleteDialogVisible = true) }
            AnniversaryEditAction.DeleteDismissed -> _uiState.update { it.copy(deleteDialogVisible = false) }
            AnniversaryEditAction.DeleteConfirmed -> if (s.deleteDialogVisible) delete()
        }
    }

    private fun updateDateSheet(block: (DateSheet) -> DateSheet) {
        _uiState.update { state ->
            val sheet = state.dateSheet ?: return@update state
            state.copy(dateSheet = block(sheet))
        }
    }

    private fun moveSheetMonth(forward: Boolean) = updateDateSheet { sheet ->
        val next = if (forward) EventRules.nextMonth(sheet.visibleMonth) else EventRules.previousMonth(sheet.visibleMonth)
        if (next == null) sheet else sheet.copy(visibleMonth = next)
    }

    /** [저장] 활성: 이름이 앞뒤 공백을 지운 뒤 비지 않아야 한다 (D-122) */
    private fun AnniversaryEditUiState.recomputed(): AnniversaryEditUiState =
        copy(saveEnabled = AnniversaryRules.isNameValid(name))

    // --- 저장·삭제 ---

    private fun save() {
        val s = _uiState.value
        if (!s.saveEnabled) return
        val name = AnniversaryRules.normalizeName(s.name)
        val before = original
        if (before == null) {
            _uiState.update { it.copy(saving = true) }
            viewModelScope.launch { create(name, s) }
            return
        }
        // 바꾼 필드만 보낸다. 날짜를 안 바꾸면 보내지 않아 등록한 날짜가 그대로다
        val patch = AnniversaryPatch(
            name = name.takeIf { it != before.name },
            date = s.date.takeIf { it != before.date },
            repeatsYearly = s.repeatsYearly.takeIf { it != before.repeatsYearly },
        )
        if (patch.isEmpty) {
            // 같은 값이면 변경으로 보지 않는다 (FR-ANNIV-03 AC6): 요청 없이 닫는다
            effectChannel.trySend(AnniversaryEditSideEffect.Close)
            return
        }
        _uiState.update { it.copy(saving = true) }
        val id = anniversaryId ?: return
        viewModelScope.launch {
            when (val result = anniversaryRepository.update(id, patch)) {
                is DataResult.Success -> effectChannel.trySend(AnniversaryEditSideEffect.Close)
                is DataResult.Failure -> fail(result.error)
            }
        }
    }

    private suspend fun create(name: String, s: AnniversaryEditUiState) {
        val result = anniversaryRepository.create(NewAnniversary(name = name, date = s.date, repeatsYearly = s.repeatsYearly))
        when (result) {
            is DataResult.Success -> effectChannel.trySend(AnniversaryEditSideEffect.Close)
            is DataResult.Failure -> fail(result.error)
        }
    }

    private fun delete() {
        val id = anniversaryId ?: return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = anniversaryRepository.delete(id)) {
                is DataResult.Success -> effectChannel.trySend(AnniversaryEditSideEffect.Close)
                is DataResult.Failure -> fail(result.error)
            }
        }
    }

    /**
     * 요청 실패 (D-121): 어떤 코드든 공통 오류 안내(오프라인은 "인터넷 연결이 필요해요"). 서버가 없다고 하면(404 `NOT_FOUND`)
     * 저장소가 기기에서 지웠으므로, 권한이 없다고 하면(403 `NOT_AUTHOR`) 저장소가 다시 불러왔으므로 화면을 닫는다.
     * 그 밖의 오류(`VALIDATION_FAILED`·`DATE_OUT_OF_RANGE`는 앱이 미리 막는다)는 화면을 그대로 둔다. 새 문구는 없다.
     */
    private fun fail(error: DataError) {
        _uiState.update { it.copy(saving = false, deleteDialogVisible = false) }
        effectChannel.trySend(AnniversaryEditSideEffect.ShowMessage(error.toMessageRes()))
        if (error.hasCode(ErrorCode.NOT_FOUND) || error.hasCode(ErrorCode.NOT_AUTHOR)) {
            effectChannel.trySend(AnniversaryEditSideEffect.Close)
        }
    }
}
