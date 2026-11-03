package com.duriseo.android.feature.settings.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.settings.menu.SettingsSpace
import com.duriseo.android.feature.settings.menu.toSettingsSpace
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
 * SCR-SET-01 연결 정보 (FR-SET-01, FR-COUPLE-05). Room의 내 정보만 구독하고, 들어올 때 `GET /me`를 새로 받는다.
 * 상대가 먼저 해제했다면 이 `GET /me`가 해제를 알아채 앱 초기화를 시작한다 (API.md 4.5의 6단계, FR-COUPLE-05 AC3).
 *
 * [연결 해제]는 온라인에서만 한다 (NFR-OFF-02). 성공하면 저장소가 앱 초기화를 끝내고 `feature:main`이
 * SCR-COUPLE-01로 보내므로(D-37, D-103) 이 화면은 이동하지 않는다.
 */
@HiltViewModel
class ConnectionInfoViewModel @Inject constructor(
    private val meRepository: MeRepository,
    private val coupleRepository: CoupleRepository,
    private val clock: Clock,
) : ViewModel() {

    private data class Local(
        val unlinkDialogVisible: Boolean = false,
        val requesting: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<ConnectionInfoUiState> = combine(meRepository.observeMe(), local) { me, localState ->
        val space = me?.toSettingsSpace(LocalDate.now(clock), clock.zone)
        ConnectionInfoUiState(
            space = space,
            // 상대가 먼저 해제해 혼자가 되면 다이얼로그를 보여 주지 않는다
            unlinkDialogVisible = localState.unlinkDialogVisible && space is SettingsSpace.Couple,
            requesting = localState.requesting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ConnectionInfoUiState())

    private val effectChannel = Channel<ConnectionInfoSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        // 오프라인이면 조용히 실패하고 Room의 값을 그대로 보여 준다 (NFR-OFF-01)
        viewModelScope.launch { meRepository.refreshMe() }
    }

    fun onAction(action: ConnectionInfoAction) {
        when (action) {
            ConnectionInfoAction.UnlinkClicked -> {
                // 연결 해제는 커플일 때만 있다. 혼자인 화면에서는 받지 않는다
                if (!local.value.requesting && uiState.value.space is SettingsSpace.Couple) {
                    local.update { it.copy(unlinkDialogVisible = true) }
                }
            }
            ConnectionInfoAction.UnlinkDismissed -> {
                if (!local.value.requesting) local.update { it.copy(unlinkDialogVisible = false) }
            }
            ConnectionInfoAction.UnlinkConfirmed -> unlink()
        }
    }

    private fun unlink() {
        val state = local.value
        if (state.requesting || !state.unlinkDialogVisible) return
        local.update { it.copy(requesting = true) }
        viewModelScope.launch {
            when (val result = coupleRepository.unlink()) {
                // 이동은 `MeRepository.spaceChanges`를 받는 `feature:main`이 한다 (이 화면의 스택은 비워진다)
                is DataResult.Success -> local.value = Local()
                is DataResult.Failure -> {
                    // 실패·오프라인이면 다이얼로그를 닫고 안내만 한다. 연결은 그대로다
                    local.value = Local()
                    effectChannel.trySend(ConnectionInfoSideEffect.ShowMessage(result.error.toMessageRes()))
                }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
