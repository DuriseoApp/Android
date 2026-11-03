package com.duriseo.android.feature.settings.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.core.designsystem.message.toMessageResOrNull
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.StartDateRules
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * SCR-SET-01 전체 메뉴 (게스트 배너, 프로필 카드, 우리 색상 행, 연결 정보 행, 계정 행, 로그아웃).
 * 화면은 Room의 내 정보만 구독한다. 쓰기(처음 만난 날 저장)는 온라인에서만 한다 (NFR-OFF-02).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val meRepository: MeRepository,
    private val coupleRepository: CoupleRepository,
    private val clock: Clock,
) : ViewModel() {

    private data class Local(
        val logoutDialogVisible: Boolean = false,
        val requesting: Boolean = false,
        val startDateSheet: StartDateSheetState? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<SettingsUiState> = combine(meRepository.observeMe(), local) { me, localState ->
        val today = LocalDate.now(clock)
        SettingsUiState(
            account = me?.toAccount(),
            space = me?.toSettingsSpace(today, clock.zone),
            startDateSheet = localState.startDateSheet,
            today = today,
            colorKeys = me?.space?.let { listOfNotNull(it.myColor, it.partner?.color, it.togetherColor) }.orEmpty(),
            logoutDialogVisible = localState.logoutDialogVisible,
            requesting = localState.requesting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    private val effectChannel = Channel<SettingsSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        // 화면에 들어올 때 내 정보를 새로 받는다. 오프라인이면 조용히 실패하고 Room의 값을 그대로 보여 준다 (ARCHITECTURE.md 2장, NFR-OFF-01)
        viewModelScope.launch {
            val result = meRepository.refreshMe()
            // 내 정보를 한 번도 받지 못했으면(Room도 비어 있음) 공통 오류 스낵바를 보여 준다. 계정 영역은 비워 둔다 (Q-14)
            if (result is DataResult.Failure && meRepository.observeMe().first() == null) {
                effectChannel.trySend(SettingsSideEffect.ShowMessage(result.error.toMessageRes()))
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.LogoutClicked -> {
                if (local.value.requesting) return
                local.update { it.copy(logoutDialogVisible = true) }
            }
            SettingsAction.DialogDismissed -> {
                if (local.value.requesting) return
                local.update { it.copy(logoutDialogVisible = false) }
            }
            SettingsAction.LogoutConfirmed -> logout()
            SettingsAction.StartDateEditClicked -> openStartDateSheet()
            SettingsAction.StartDatePreviousMonth -> moveStartDateMonth(forward = false)
            SettingsAction.StartDateNextMonth -> moveStartDateMonth(forward = true)
            is SettingsAction.StartDateSelected -> selectStartDate(action.date)
            is SettingsAction.StartDateMonthSelected -> selectStartMonth(action.month)
            SettingsAction.StartDateSaved -> saveStartDate()
            SettingsAction.StartDateSheetDismissed -> {
                if (local.value.startDateSheet?.saving != true) local.update { it.copy(startDateSheet = null) }
            }
        }
    }

    private fun openStartDateSheet() {
        viewModelScope.launch {
            val me = meRepository.observeMe().first() ?: return@launch
            val today = LocalDate.now(clock)
            val couple = me.toSettingsSpace(today, clock.zone) as? SettingsSpace.Couple ?: return@launch
            local.update {
                it.copy(
                    startDateSheet = StartDateSheetState(
                        visibleMonth = StartDateRules.initialMonth(couple.startDate, today),
                        selected = couple.startDate,
                    ),
                )
            }
        }
    }

    private fun moveStartDateMonth(forward: Boolean) {
        val today = LocalDate.now(clock)
        local.update { current ->
            val sheet = current.startDateSheet ?: return@update current
            val next = if (forward) {
                StartDateRules.nextMonth(sheet.visibleMonth, today)
            } else {
                StartDateRules.previousMonth(sheet.visibleMonth)
            }
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

    /** 선택 시트의 [저장]: `PATCH /space`. 성공하면 시트를 닫고 Room이 바뀐 처음 만난 날을 보여 준다 (FR-COUPLE-04 AC1) */
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
                        effectChannel.trySend(SettingsSideEffect.ShowMessage(result.error.toMessageRes()))
                    }
                }
            }
        }
    }

    private fun logout() {
        val state = local.value
        if (state.requesting || !state.logoutDialogVisible) return
        local.update { it.copy(requesting = true) }
        viewModelScope.launch {
            when (val result = authRepository.logout()) {
                is DataResult.Success -> {
                    local.value = Local()
                    effectChannel.trySend(SettingsSideEffect.NavigateToStart)
                }
                is DataResult.Failure -> {
                    // 실패하면 다이얼로그를 닫고 로그인 상태 그대로 둔다 (D-86). 세션이 이미 끊겼다는 응답이면 안내 없이
                    // 세션 종료 흐름이 시작 화면으로 보낸다
                    local.value = Local()
                    result.error.toMessageResOrNull()?.let {
                        effectChannel.trySend(SettingsSideEffect.ShowMessage(it))
                    }
                }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun Me.toAccount(): SettingsAccount = when (accountType) {
    AccountType.GUEST -> SettingsAccount.Guest
    AccountType.MEMBER -> SettingsAccount.Member(providers.firstOrNull())
}
