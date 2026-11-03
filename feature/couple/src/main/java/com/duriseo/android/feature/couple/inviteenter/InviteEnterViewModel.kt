package com.duriseo.android.feature.couple.inviteenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.InviteCodeInput
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.couple.InviteRateLimit
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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

    /** 입력 제한이 풀릴 때까지 기다리는 작업. 화면을 나가면 ViewModel과 함께 사라져 제한 상태도 남지 않는다 */
    private var rateLimitJob: Job? = null

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
        // 글자가 바뀌면 코드 오류 문구는 지운다. 입력 제한 문구만 풀릴 때까지 남는다 (SCREENS.md SCR-COUPLE-03).
        // 소문자는 대문자로 바꾸고 영문·숫자만 6자까지 받는다 (FR-COUPLE-01 AC4)
        _uiState.update {
            it.copy(
                code = InviteCodeInput.clean(raw),
                error = it.error.takeIf { error -> error is InviteEnterError.RateLimited },
            )
        }
    }

    private fun connect() {
        val state = _uiState.value
        if (!state.canConnect) return
        _uiState.update { it.copy(requesting = true, error = null) }
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

    /** 오류 코드로 문구를 고른다 (`message`는 쓰지 않는다, API.md 1.1). 표에 없는 오류는 공통 오류·오프라인 안내다 (D-95) */
    private fun onFailure(error: DataError) {
        val inviteError = when ((error as? DataError.ApiError)?.knownCode) {
            ErrorCode.INVITE_NOT_FOUND -> InviteEnterError.NotFound
            ErrorCode.INVITE_EXPIRED -> InviteEnterError.Expired
            ErrorCode.INVITE_USED -> InviteEnterError.Used
            ErrorCode.INVITE_SELF -> InviteEnterError.Self
            ErrorCode.INVITE_UNAVAILABLE -> InviteEnterError.Unavailable
            ErrorCode.INVITE_RATE_LIMITED -> rateLimited(error as DataError.ApiError)
            else -> null
        }
        if (inviteError != null) {
            _uiState.update { it.copy(error = inviteError) }
        } else {
            // VALIDATION_FAILED, ALREADY_COUPLED, GUEST_NOT_ALLOWED(리포지토리가 GET /me를 다시 읽음), 5xx, 오프라인
            effectChannel.trySend(InviteEnterSideEffect.ShowMessage(error.toMessageRes()))
        }
    }

    /**
     * 입력 제한: [N]분은 지금 한 번 정한다. `retryAfterSeconds`가 지나면 문구를 지우고 버튼을 다시 켠다.
     * 풀리는 시간을 알려 주는 `details`가 없으면 안내를 만들 수 없어 null이고 공통 오류가 된다.
     */
    private fun rateLimited(error: DataError.ApiError): InviteEnterError? {
        val retry = error.details as? ErrorDetails.RetryAfter ?: return null
        rateLimitJob?.cancel()
        rateLimitJob = viewModelScope.launch {
            delay(retry.retryAfterSeconds * MILLIS_PER_SECOND)
            _uiState.update { state ->
                if (state.error is InviteEnterError.RateLimited) state.copy(error = null) else state
            }
        }
        return InviteEnterError.RateLimited(InviteRateLimit.minutesFor(retry.retryAfterSeconds))
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
