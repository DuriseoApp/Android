package com.duriseo.android.feature.couple.inviteenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.InviteCodeInput
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.couple.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SCR-COUPLE-03 초대코드 입력하기 (FR-COUPLE-02, API.md 4.4의 2단계).
 *
 * 연결 응답이 오면 리포지토리가 `GET /me`로 Room을 채우고, 이 화면은 캘린더로 보내 달라고 알린다.
 */
@HiltViewModel
class InviteEnterViewModel @Inject constructor(
    private val coupleRepository: CoupleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteEnterUiState())
    val uiState: StateFlow<InviteEnterUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<InviteEnterSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onAction(action: InviteEnterAction) {
        when (action) {
            is InviteEnterAction.CodeChanged -> changeCode(action.raw)
            is InviteEnterAction.Pasted -> {
                val text = action.text
                if (!text.isNullOrBlank()) changeCode(text)
            }
            InviteEnterAction.ConnectClicked -> connect()
        }
    }

    private fun changeCode(raw: String) {
        if (_uiState.value.requesting) return
        // 글자가 바뀌면 이전 오류는 지운다. 소문자는 대문자로 바꾸고 영문·숫자만 6자까지 받는다 (FR-COUPLE-01 AC4)
        _uiState.update { it.copy(code = InviteCodeInput.clean(raw), errorRes = null) }
    }

    private fun connect() {
        val state = _uiState.value
        if (!state.canConnect) return
        _uiState.update { it.copy(requesting = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = coupleRepository.join(state.code)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(requesting = false) }
                    effectChannel.trySend(InviteEnterSideEffect.NavigateToHome)
                }
                is DataResult.Failure -> {
                    _uiState.update { it.copy(requesting = false) }
                    onFailure(result.error)
                }
            }
        }
    }

    private fun onFailure(error: DataError) {
        val code = (error as? DataError.ApiError)?.knownCode
        if (code == ErrorCode.INVITE_EXPIRED) {
            // 문구가 문서에 있는 유일한 코드 오류 (SCREENS.md SCR-COUPLE-03, D-47)
            _uiState.update { it.copy(errorRes = R.string.invite_enter_error_expired) }
        } else {
            // TODO(v0.9 반영 대기): INVITE_NOT_FOUND·INVITE_USED·INVITE_SELF·INVITE_UNAVAILABLE·INVITE_RATE_LIMITED·
            // ALREADY_COUPLED·GUEST_NOT_ALLOWED는 문구와 위치가 문서에 없다. 그때까지는 공통 오류(오프라인은 안내)로 둔다
            effectChannel.trySend(InviteEnterSideEffect.ShowMessage(error.toMessageRes()))
        }
    }
}
