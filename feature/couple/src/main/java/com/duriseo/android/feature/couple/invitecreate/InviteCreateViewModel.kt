package com.duriseo.android.feature.couple.invitecreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.couple.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * SCR-COUPLE-02 초대코드 만들기 (FR-COUPLE-01, FR-COUPLE-02 AC5, API.md 4.4의 1·8단계).
 *
 * - 화면이 보이는 동안([InviteCreateAction.Shown]~[InviteCreateAction.Hidden])만 1초마다 남은 시간을 갱신하고
 *   5초마다 `GET /me`로 연결을 확인한다. 보이지 않는 동안에는 새 코드도 받지 않는다 (D-42).
 * - 코드는 `GET /invite-codes/current`로 먼저 읽고, 없으면(404) `POST /invite-codes`로 만든다.
 * - 연결 여부는 Room의 내 정보(공간 종류 `COUPLE`)로 안다. 연결 확인은 `refreshMe()`가 Room을 채우게 하는 일이다.
 * - 남은 시간·재요청 시각은 서버 시각이 아니라 기기 시계와 비교한다. 기기 시계가 어긋나 만료로 보여도
 *   `current`가 같은 코드를 주면 그대로 두고 몇 초 뒤 다시 본다 (진행 기록 결정 4).
 */
@HiltViewModel
class InviteCreateViewModel @Inject constructor(
    private val coupleRepository: CoupleRepository,
    private val meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteCreateUiState())
    val uiState: StateFlow<InviteCreateUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<InviteCreateSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    // 아래 필드는 메인 스레드(viewModelScope)에서만 바꾼다
    private var invite: InviteCode? = null
    private var reissueAvailableAt: Instant = Instant.EPOCH
    private var nextAttemptAt: Instant = Instant.EPOCH
    private var loadFailureShown = false
    private var navigated = false
    private var visibleJob: Job? = null
    private var loadJob: Job? = null
    private var reissueJob: Job? = null

    init {
        // 상대가 연결하면 Room의 공간 종류가 COUPLE로 바뀐다 (5초 확인, [연결 확인하기], 이후 FCM 모두 Room을 거친다)
        viewModelScope.launch {
            meRepository.observeMe().collect { me ->
                if (me?.space?.type == SpaceType.COUPLE) navigateToHome()
            }
        }
    }

    fun onAction(action: InviteCreateAction) {
        when (action) {
            InviteCreateAction.Shown -> onShown()
            InviteCreateAction.Hidden -> onHidden()
            InviteCreateAction.CopyClicked -> {
                val code = invite?.code ?: return
                _uiState.update { it.copy(copied = true) }
                effectChannel.trySend(InviteCreateSideEffect.CopyCode(code))
            }
            InviteCreateAction.ShareClicked -> {
                val code = invite?.code ?: return
                effectChannel.trySend(InviteCreateSideEffect.ShareCode(code))
            }
            InviteCreateAction.ReissueClicked -> reissue()
            InviteCreateAction.CheckClicked -> check()
        }
    }

    private fun onShown() {
        if (visibleJob?.isActive == true) return
        visibleJob = viewModelScope.launch {
            launch {
                while (isActive) {
                    onTick()
                    delay(TICK_MILLIS)
                }
            }
            launch {
                while (isActive) {
                    delay(POLL_MILLIS)
                    // 결과는 Room으로 들어온다. 실패해도 조용히 다음 5초를 기다린다 (읽기)
                    meRepository.refreshMe()
                }
            }
        }
    }

    private fun onHidden() {
        visibleJob?.cancel()
        visibleJob = null
    }

    /** 1초마다: 남은 시간을 갱신하고, 코드가 없거나 만료됐으면 받아 온다 */
    private fun onTick() {
        val now = clock.instant()
        val current = invite
        val needsCode = current == null || !now.isBefore(current.expiresAt)
        if (needsCode && loadJob?.isActive != true && reissueJob?.isActive != true && !now.isBefore(nextAttemptAt)) {
            startLoad()
        }
        publish(now)
    }

    /**
     * 처음이면 `current` -> 없으면 `POST`. 만료돼서 받는 것이면 새 코드를 받은 뒤 안내를 띄운다.
     * 실패하면 5초 뒤에 다시 한다 (처음 실패 한 번만 안내).
     */
    private fun startLoad() {
        val expired = invite != null
        loadJob = viewModelScope.launch {
            when (val current = coupleRepository.currentInviteCode()) {
                is DataResult.Success -> {
                    val found = current.value
                    val now = clock.instant()
                    when {
                        found != null && now.isBefore(found.expiresAt) ->
                            apply(found, renewedNotice = expired && found.code != invite?.code)
                        // 서버는 아직 유효하다는데 기기 시계로는 만료: 그대로 두고 잠시 뒤 다시 본다 (결정 4)
                        found != null -> nextAttemptAt = now.plus(RETRY_DELAY)
                        else -> issueNew(renewedNotice = expired)
                    }
                }
                is DataResult.Failure -> onLoadFailed(current.error)
            }
        }
    }

    private suspend fun issueNew(renewedNotice: Boolean) {
        when (val result = coupleRepository.issueInviteCode()) {
            is DataResult.Success -> apply(result.value, renewedNotice)
            is DataResult.Failure -> onLoadFailed(result.error)
        }
    }

    private fun onLoadFailed(error: DataError) {
        nextAttemptAt = clock.instant().plus(RETRY_DELAY)
        if (!loadFailureShown) {
            loadFailureShown = true
            effectChannel.trySend(InviteCreateSideEffect.ShowMessage(error.toMessageRes()))
        }
    }

    private fun apply(code: InviteCode, renewedNotice: Boolean) {
        invite = code
        reissueAvailableAt = code.reissueAvailableAt
        loadFailureShown = false
        _uiState.update { it.copy(copied = false, renewedNotice = renewedNotice) }
        publish(clock.instant())
    }

    private fun publish(now: Instant) {
        val current = invite
        _uiState.update {
            it.copy(
                code = current?.code,
                remainingSeconds = if (current == null) 0 else secondsUntil(current.expiresAt, now),
                reissueWaitSeconds = secondsUntil(reissueAvailableAt, now),
            )
        }
    }

    /** [코드 새로 만들기] (FR-COUPLE-01 AC3). 마지막 발급 후 1분 안이면 서버가 429 `INVITE_REISSUE_TOO_SOON`을 준다 */
    private fun reissue() {
        val state = _uiState.value
        if (!state.canReissue || loadJob?.isActive == true) return
        _uiState.update { it.copy(requesting = true) }
        reissueJob = viewModelScope.launch {
            when (val result = coupleRepository.issueInviteCode()) {
                is DataResult.Success -> apply(result.value, renewedNotice = false)
                is DataResult.Failure -> onReissueFailed(result.error)
            }
            _uiState.update { it.copy(requesting = false) }
        }
    }

    private fun onReissueFailed(error: DataError) {
        val tooSoon = error is DataError.ApiError && error.knownCode == ErrorCode.INVITE_REISSUE_TOO_SOON
        val retry = (error as? DataError.ApiError)?.details as? ErrorDetails.RetryAfter
        if (tooSoon && retry != null) {
            // 서버가 알려 준 만큼 기다린다. 버튼이 꺼지고 남은 초가 보인다
            val now = clock.instant()
            reissueAvailableAt = now.plusSeconds(retry.retryAfterSeconds.toLong())
            publish(now)
        } else {
            effectChannel.trySend(InviteCreateSideEffect.ShowMessage(error.toMessageRes()))
        }
    }

    /** [연결 확인하기] (FR-COUPLE-02 AC5): 바로 `GET /me`. 아직 개인 공간이면 "아직 연결되지 않았어요" */
    private fun check() {
        viewModelScope.launch {
            when (val result = meRepository.refreshMe()) {
                is DataResult.Success -> {
                    val type = meRepository.observeMe().first()?.space?.type
                    if (type == SpaceType.COUPLE) {
                        navigateToHome()
                    } else {
                        effectChannel.trySend(InviteCreateSideEffect.ShowMessage(R.string.invite_create_not_yet))
                    }
                }
                is DataResult.Failure ->
                    effectChannel.trySend(InviteCreateSideEffect.ShowMessage(result.error.toMessageRes()))
            }
        }
    }

    private fun navigateToHome() {
        if (navigated) return
        navigated = true
        effectChannel.trySend(InviteCreateSideEffect.NavigateToHome)
    }

    override fun onCleared() {
        visibleJob?.cancel()
        super.onCleared()
    }

    private fun secondsUntil(target: Instant, now: Instant): Int {
        val millis = Duration.between(now, target).toMillis()
        if (millis <= 0) return 0
        return ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }

    private companion object {
        const val TICK_MILLIS = 1_000L

        /** 연결 확인 간격 5초 (API.md 4.4의 8단계, D-48) */
        const val POLL_MILLIS = 5_000L
        const val MILLIS_PER_SECOND = 1_000L
        val RETRY_DELAY: Duration = Duration.ofSeconds(5)
    }
}
