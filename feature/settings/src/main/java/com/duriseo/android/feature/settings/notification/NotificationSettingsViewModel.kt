package com.duriseo.android.feature.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.message.toMessageResOrNull
import com.duriseo.android.domain.notification.NotificationSettingKind
import com.duriseo.android.domain.notification.NotificationSettingsRepository
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
import javax.inject.Inject

/**
 * SCR-SET-02 알림 설정 (FR-NOTI-02, D-145). 화면은 기기에 저장된 마지막 서버 값만 구독한다.
 *
 * - 들어올 때마다 서버 값을 읽는다. 한 번도 받지 못했으면(저장된 값이 없고 읽기에 실패) 스위치를 비활성으로 두고 공통 오류를 보인다.
 *   저장된 값이 있으면 읽기 실패는 조용히 넘기고 그 값을 보인다.
 * - 스위치를 누르면 바뀐 종류 하나만 `PATCH`한다. 저장 중에는 로딩 덮개(D-115), 실패는 공통 오류, 오프라인은 "인터넷 연결이 필요해요".
 *   낙관적 갱신이 없어서 실패하면 스위치는 원래 값 그대로다.
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repository: NotificationSettingsRepository,
) : ViewModel() {

    private val saving = MutableStateFlow(false)

    val uiState: StateFlow<NotificationSettingsUiState> =
        combine(repository.observe(), saving) { settings, isSaving ->
            NotificationSettingsUiState(settings = settings, saving = isSaving)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, NotificationSettingsUiState())

    private val effectChannel = Channel<NotificationSettingsSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        // 화면에 들어올 때마다 서버 값을 읽는다 (D-145)
        viewModelScope.launch {
            val result = repository.refresh()
            if (result is DataResult.Failure && repository.observe().first() == null) {
                // 한 번도 받지 못했다: 오프라인도 공통 오류로 보인다 (SCREENS.md SCR-SET-02 "불러오기")
                effectChannel.trySend(NotificationSettingsSideEffect.ShowMessage(DesignR.string.error_common))
            }
        }
    }

    fun onAction(action: NotificationSettingsAction) {
        when (action) {
            NotificationSettingsAction.BackClicked -> effectChannel.trySend(NotificationSettingsSideEffect.Back)
            is NotificationSettingsAction.Toggled -> toggle(action.kind)
        }
    }

    private fun toggle(kind: NotificationSettingKind) {
        val state = uiState.value
        val current = state.settings ?: return
        if (state.saving) return
        saving.value = true
        viewModelScope.launch {
            val result = repository.update(kind, !current[kind])
            saving.update { false }
            // 결과 안내는 덮개가 걷힌 뒤에 보인다
            if (result is DataResult.Failure) {
                result.error.toMessageResOrNull()?.let { effectChannel.trySend(NotificationSettingsSideEffect.ShowMessage(it)) }
            }
        }
    }
}
