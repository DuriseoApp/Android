package com.duriseo.android.feature.settings.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageResOrNull
import com.duriseo.android.domain.auth.AuthRepository
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** SCR-SET-01 전체 메뉴 (단위 2: 게스트 배너, 계정 행, 로그아웃). 화면은 Room의 내 정보만 구독한다. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val meRepository: MeRepository,
) : ViewModel() {

    private data class Local(val logoutDialogVisible: Boolean = false, val requesting: Boolean = false)

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<SettingsUiState> = combine(meRepository.observeMe(), local) { me, localState ->
        SettingsUiState(
            account = me?.toAccount(),
            colorKeys = me?.space?.let { listOfNotNull(it.myColor, it.partner?.color, it.togetherColor) }.orEmpty(),
            logoutDialogVisible = localState.logoutDialogVisible,
            requesting = localState.requesting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    private val effectChannel = Channel<SettingsSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        // 화면에 들어올 때 내 정보를 새로 받는다. 오프라인이면 조용히 실패하고 Room의 값을 그대로 보여 준다 (ARCHITECTURE.md 2장, NFR-OFF-01)
        viewModelScope.launch { meRepository.refreshMe() }
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
