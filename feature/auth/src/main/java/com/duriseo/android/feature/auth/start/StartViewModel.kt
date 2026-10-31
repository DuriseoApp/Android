package com.duriseo.android.feature.auth.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.message.toMessageResOrNull
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.auth.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** SCR-START-01. 게스트 시작과 Google·카카오 로그인 (FR-AUTH-01, FR-AUTH-02). */
@HiltViewModel
class StartViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val meRepository: MeRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<StartSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var sessionNoticeShown = false

    fun onAction(action: StartAction) {
        when (action) {
            StartAction.GuestClicked -> startAsGuest()
            is StartAction.SocialClicked -> {
                if (!beginRequest()) return
                effectChannel.trySend(StartSideEffect.RequestSocialToken(action.provider))
            }
            is StartAction.SocialTokenReceived -> onSocialToken(action.provider, action.result)
            StartAction.SessionReplacedArrived -> {
                if (sessionNoticeShown) return
                sessionNoticeShown = true
                effectChannel.trySend(StartSideEffect.ShowMessage(R.string.start_session_replaced))
            }
        }
    }

    private fun startAsGuest() {
        if (!beginRequest()) return
        viewModelScope.launch {
            when (val result = authRepository.startAsGuest()) {
                is DataResult.Success -> {
                    endRequest()
                    effectChannel.trySend(StartSideEffect.LoggedIn(AfterLogin.Home))
                }
                is DataResult.Failure -> failRequest(result)
            }
        }
    }

    private fun onSocialToken(provider: SocialProvider, result: SocialTokenResult) {
        when (result) {
            // 사용자가 로그인 창을 닫았다: 아무 안내 없이 원래 화면 그대로 (D-88)
            SocialTokenResult.Cancelled -> endRequest()
            // SDK 실패(기기에 Google 계정이 없는 경우 포함): 공통 오류 안내
            is SocialTokenResult.Failed -> {
                endRequest()
                effectChannel.trySend(StartSideEffect.ShowMessage(DesignR.string.error_common))
            }
            is SocialTokenResult.Success -> viewModelScope.launch {
                mutableState.update { it.copy(requesting = true) }
                when (val login = authRepository.signIn(provider, result.token)) {
                    is DataResult.Success -> {
                        val destination = destinationAfter(login.value)
                        endRequest()
                        effectChannel.trySend(StartSideEffect.LoggedIn(destination))
                    }
                    is DataResult.Failure -> failRequest(login)
                }
            }
        }
    }

    /** 신규 회원은 SCR-COUPLE-01, 가져올 기록이 남은 기존 회원은 SCR-COUPLE-04, 그 밖에는 캘린더 (FR-AUTH-02) */
    private suspend fun destinationAfter(login: LoginResult): AfterLogin = when {
        login.isNewUser -> AfterLogin.CoupleConnect
        meRepository.observeMe().first()?.space?.importPending == true -> AfterLogin.ImportPending
        else -> AfterLogin.Home
    }

    /** 이미 요청 중이면 false (버튼이 비활성이라 보통은 오지 않는다) */
    private fun beginRequest(): Boolean {
        if (mutableState.value.requesting) return false
        mutableState.update { it.copy(requesting = true) }
        return true
    }

    private fun endRequest() {
        mutableState.update { it.copy(requesting = false) }
    }

    /** 서버 실패: 화면과 상태는 그대로 두고 스낵바만 띄운다. 세션 종료 코드는 문구가 없다 (API.md 1장) */
    private fun failRequest(failure: DataResult.Failure) {
        endRequest()
        failure.error.toMessageResOrNull()?.let { effectChannel.trySend(StartSideEffect.ShowMessage(it)) }
    }
}
