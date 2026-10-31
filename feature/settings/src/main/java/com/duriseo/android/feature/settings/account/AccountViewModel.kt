package com.duriseo.android.feature.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.message.toMessageResOrNull
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.me.SpaceType
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
import java.time.Clock
import javax.inject.Inject

/**
 * SCR-SET-01 하위 화면 "계정": 게스트 연동, 이미 가입된 계정 로그인(switch), 로그아웃, 탈퇴
 * (FR-AUTH-03~06, FR-SET-01). 화면은 Room의 내 정보만 구독하고, 쓰기는 온라인에서만 한다 (NFR-OFF-02).
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    /** 소셜 SDK가 돌려준 토큰을 어디에 쓰는가 */
    private enum class TokenPurpose {
        /** 게스트 연동 `POST /auth/link` */
        Link,

        /** `SOCIAL_TOKEN_INVALID`로 실패한 switch를 새 토큰으로 한 번 다시 보낸다 (D-67) */
        SwitchRetry,
    }

    /** switch에 쓸 소셜 토큰. 다이얼로그 ①②가 떠 있는 동안 ViewModel 메모리에만 둔다 (저장·로그 없음, NFR-SEC-01) */
    private class PendingSocialLogin(val provider: SocialProvider, val token: String) {
        override fun toString() = "PendingSocialLogin(provider=$provider, token=***)"
    }

    private data class Local(
        val dialog: AccountDialog? = null,
        val requesting: Boolean = false,
        val justLinked: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    private var tokenPurpose = TokenPurpose.Link
    private var pendingSwitch: PendingSocialLogin? = null
    private var switchRetried = false

    val uiState: StateFlow<AccountUiState> = combine(meRepository.observeMe(), local) { me, localState ->
        AccountUiState(
            content = me?.toContent(),
            withdrawMessage = me?.toWithdrawMessage() ?: WithdrawMessage.Personal,
            dialog = localState.dialog,
            requesting = localState.requesting,
            justLinked = localState.justLinked,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AccountUiState())

    private val effectChannel = Channel<AccountSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        // 화면에 들어올 때 내 정보를 새로 받는다. 오프라인이면 조용히 실패하고 Room의 값을 그대로 보여 준다 (ARCHITECTURE.md 2장, NFR-OFF-01)
        viewModelScope.launch { meRepository.refreshMe() }
    }

    fun onAction(action: AccountAction) {
        when (action) {
            is AccountAction.LinkClicked -> startLink(action.provider)
            is AccountAction.SocialTokenReceived -> onSocialToken(action.provider, action.result)
            AccountAction.ExistingLoginClicked -> {
                val state = local.value
                if (state.dialog == AccountDialog.AlreadyUsed && !state.requesting) {
                    local.update { it.copy(dialog = AccountDialog.SwitchConfirm) }
                }
            }
            AccountAction.SwitchConfirmed -> confirmSwitch()
            AccountAction.LogoutClicked -> openDialog(AccountDialog.Logout)
            AccountAction.WithdrawClicked -> openDialog(AccountDialog.Withdraw)
            AccountAction.LogoutConfirmed -> leave(AccountDialog.Logout) { authRepository.logout() }
            AccountAction.WithdrawConfirmed -> leave(AccountDialog.Withdraw) { authRepository.withdraw() }
            AccountAction.DialogDismissed -> dismissDialog()
        }
    }

    // ---- 게스트 연동 (FR-AUTH-03 AC1·AC2) ----

    private fun startLink(provider: SocialProvider) {
        val state = local.value
        if (state.requesting || state.dialog != null) return
        tokenPurpose = TokenPurpose.Link
        local.update { it.copy(requesting = true, justLinked = false) }
        effectChannel.trySend(AccountSideEffect.RequestSocialToken(provider))
    }

    private fun onSocialToken(provider: SocialProvider, result: SocialTokenResult) {
        val retrying = tokenPurpose == TokenPurpose.SwitchRetry
        when (result) {
            // 사용자가 로그인 창을 닫았다: 아무 안내 없이 원래 화면 그대로 (D-88). switch 재시도 중이면 게스트로 계속 쓴다
            SocialTokenResult.Cancelled -> finishRequest(closeDialog = retrying)
            is SocialTokenResult.Failed -> {
                finishRequest(closeDialog = retrying)
                effectChannel.trySend(AccountSideEffect.ShowMessage(DesignR.string.error_common))
            }
            is SocialTokenResult.Success -> {
                local.update { it.copy(requesting = true) }
                if (retrying) {
                    val pending = PendingSocialLogin(provider, result.token)
                    pendingSwitch = pending
                    runSwitch(pending)
                } else {
                    link(provider, result.token)
                }
            }
        }
    }

    private fun link(provider: SocialProvider, token: String) {
        viewModelScope.launch {
            when (val result = authRepository.link(provider, token)) {
                // 토큰은 그대로, 내 정보가 Room에서 바뀌면 같은 화면이 회원으로 바뀐다
                is DataResult.Success -> local.update { it.copy(requesting = false, justLinked = true) }
                is DataResult.Failure -> {
                    if (result.error.isCode(ErrorCode.SOCIAL_ALREADY_USED)) {
                        // 연동은 하지 않는다. 취소하면 게스트로 계속 쓴다 (D-67)
                        pendingSwitch = PendingSocialLogin(provider, token)
                        local.update { it.copy(requesting = false, dialog = AccountDialog.AlreadyUsed) }
                    } else {
                        local.update { it.copy(requesting = false) }
                        showError(result.error)
                    }
                }
            }
        }
    }

    // ---- 이미 가입된 계정으로 로그인 (FR-AUTH-03 AC3, D-67) ----

    private fun confirmSwitch() {
        val state = local.value
        val pending = pendingSwitch ?: return
        if (state.requesting || state.dialog != AccountDialog.SwitchConfirm) return
        switchRetried = false
        local.update { it.copy(requesting = true) }
        runSwitch(pending)
    }

    private fun runSwitch(pending: PendingSocialLogin) {
        viewModelScope.launch {
            when (val result = authRepository.switchAccount(pending.provider, pending.token)) {
                is DataResult.Success -> {
                    pendingSwitch = null
                    local.update { it.copy(requesting = false, dialog = null) }
                    effectChannel.trySend(AccountSideEffect.NavigateToHome)
                }
                is DataResult.Failure -> {
                    if (result.error.isCode(ErrorCode.SOCIAL_TOKEN_INVALID) && !switchRetried) {
                        // 토큰이 만료됐다: 소셜 SDK에서 한 번 다시 받아 다시 보낸다 (사용자 동작 없음)
                        switchRetried = true
                        tokenPurpose = TokenPurpose.SwitchRetry
                        effectChannel.trySend(AccountSideEffect.RequestSocialToken(pending.provider))
                    } else {
                        // `SOCIAL_NOT_REGISTERED`를 포함한 그 밖의 오류: 공통 오류를 보여 주고 다이얼로그를 닫는다. 게스트 그대로다
                        pendingSwitch = null
                        local.update { it.copy(requesting = false, dialog = null) }
                        showError(result.error)
                    }
                }
            }
        }
    }

    // ---- 로그아웃·탈퇴 (FR-AUTH-05, FR-AUTH-06, D-86) ----

    private fun leave(expected: AccountDialog, call: suspend () -> DataResult<Unit>) {
        val state = local.value
        if (state.requesting || state.dialog != expected) return
        local.update { it.copy(requesting = true) }
        viewModelScope.launch {
            when (val result = call()) {
                is DataResult.Success -> {
                    local.update { it.copy(requesting = false, dialog = null) }
                    effectChannel.trySend(AccountSideEffect.NavigateToStart)
                }
                is DataResult.Failure -> {
                    // 실패하면 다이얼로그를 닫고 로그인 상태 그대로 둔다. 세션이 이미 끊겼다는 응답이면 안내 없이
                    // 세션 종료 흐름이 시작 화면으로 보낸다
                    local.update { it.copy(requesting = false, dialog = null) }
                    showError(result.error)
                }
            }
        }
    }

    // ---- 다이얼로그 ----

    private fun openDialog(dialog: AccountDialog) {
        if (local.value.requesting) return
        local.update { it.copy(dialog = dialog) }
    }

    /** 어느 다이얼로그든 [취소]면 요청을 보내지 않는다 (FR-AUTH-03 AC3) */
    private fun dismissDialog() {
        if (local.value.requesting) return
        pendingSwitch = null
        local.update { it.copy(dialog = null) }
    }

    private fun finishRequest(closeDialog: Boolean) {
        if (closeDialog) pendingSwitch = null
        local.update { it.copy(requesting = false, dialog = if (closeDialog) null else it.dialog) }
    }

    private fun showError(error: DataError) {
        error.toMessageResOrNull()?.let { effectChannel.trySend(AccountSideEffect.ShowMessage(it)) }
    }

    private fun DataError.isCode(code: ErrorCode): Boolean = this is DataError.ApiError && knownCode == code

    private fun Me.toContent(): AccountContent = when (accountType) {
        AccountType.GUEST -> AccountContent.Guest(guestId = id)
        AccountType.MEMBER -> AccountContent.Member(
            provider = providers.firstOrNull(),
            nickname = nickname,
            joinedDate = createdAt.atZone(clock.zone).toLocalDate(),
        )
    }

    private fun Me.toWithdrawMessage(): WithdrawMessage {
        val partner = space.partner
        return if (space.type == SpaceType.COUPLE && partner != null) {
            WithdrawMessage.Couple(partner.nickname)
        } else {
            WithdrawMessage.Personal
        }
    }

    override fun onCleared() {
        // 소셜 토큰을 메모리에서 버린다
        pendingSwitch = null
        super.onCleared()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
