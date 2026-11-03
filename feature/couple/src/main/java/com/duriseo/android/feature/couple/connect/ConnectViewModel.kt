package com.duriseo.android.feature.couple.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageResOrNull
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.me.AccountType
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

/**
 * SCR-COUPLE-01 커플 연결 (연결 방법 선택). 게스트인지는 Room의 내 정보로 정한다 (화면은 Room만 구독한다).
 * 아직 내 정보가 없으면 회원 모양이다 (게스트가 이 화면에 오는 길은 내 정보를 이미 받은 뒤다).
 */
@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val meRepository: MeRepository,
) : ViewModel() {

    private data class Local(val logoutDialogVisible: Boolean = false, val requesting: Boolean = false)

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<ConnectUiState> = combine(meRepository.observeMe(), local) { me, localState ->
        ConnectUiState(
            isGuest = me?.accountType == AccountType.GUEST,
            logoutDialogVisible = localState.logoutDialogVisible,
            requesting = localState.requesting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ConnectUiState())

    private val effectChannel = Channel<ConnectSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onAction(action: ConnectAction) {
        when (action) {
            ConnectAction.MakeCodeClicked -> effectChannel.trySend(ConnectSideEffect.NavigateToInviteCreate)
            ConnectAction.EnterCodeClicked -> effectChannel.trySend(ConnectSideEffect.NavigateToInviteEnter)
            ConnectAction.SoloClicked -> effectChannel.trySend(ConnectSideEffect.NavigateToHome)
            ConnectAction.GuestLoginClicked -> effectChannel.trySend(ConnectSideEffect.NavigateToAccount)
            ConnectAction.OtherAccountClicked -> local.update { it.copy(logoutDialogVisible = true) }
            ConnectAction.LogoutDismissed -> {
                if (local.value.requesting) return
                local.update { it.copy(logoutDialogVisible = false) }
            }
            ConnectAction.LogoutConfirmed -> logout()
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
                    effectChannel.trySend(ConnectSideEffect.NavigateToStart)
                }
                is DataResult.Failure -> {
                    // 실패하면 다이얼로그를 닫고 로그인 상태 그대로 둔다 (D-86). 세션이 이미 끊겼다는 응답이면
                    // 안내 없이 세션 종료 흐름이 시작 화면으로 보낸다
                    local.value = Local()
                    result.error.toMessageResOrNull()?.let {
                        effectChannel.trySend(ConnectSideEffect.ShowMessage(it))
                    }
                }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
