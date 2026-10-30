package com.duriseo.android.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.SessionEndReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 앱을 켰을 때 처음 보여 줄 곳 (FR-AUTH-01 AC2·AC3) */
enum class MainStart {
    /** 토큰 저장소를 아직 읽는 중. 빈 화면(문구 없음)을 보여 준다 */
    Loading,

    /** 저장된 토큰이 있다 → 시작 화면 없이 캘린더 (FR-AUTH-01 AC2) */
    Home,

    /** 토큰이 없거나 복호화할 수 없다 → SCR-START-01 (FR-AUTH-01 AC3) */
    Start,
}

sealed interface MainEffect {
    /**
     * 서버가 세션을 끊었다 (401 `SESSION_REPLACED`·`SESSION_INVALID`). 로컬 정리는 끝났다.
     * 시작 화면으로 가고 백스택을 비운다. [replaced]면 시작 화면이 안내 스낵바를 보여 준다 (D-51)
     */
    data class SessionEnded(val replaced: Boolean) : MainEffect
}

/**
 * 앱 전체 로그인 상태를 지킨다. 시작 분기(앱 시작 때 한 번)와 세션 끊김 알림을 맡는다.
 * 로그인·로그아웃·탈퇴 뒤 이동은 각 화면의 ViewModel이 알려 준다.
 *
 * 세션 끊김 알림은 구독자가 없을 때 나오면 사라지므로(`SessionManager.events`) 이 ViewModel이 만들어지자마자 구독한다.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    /** 토큰 저장소를 읽은 첫 결과로 정한다. 그 뒤로는 바뀌지 않는다 (이후 이동은 내비게이션이 한다) */
    val start: StateFlow<MainStart> = flow {
        val state = authRepository.authState.first { it != AuthState.Unknown }
        emit(if (state == AuthState.SignedIn) MainStart.Home else MainStart.Start)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainStart.Loading)

    private val effectChannel = Channel<MainEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionEvents.collect { reason ->
                effectChannel.trySend(MainEffect.SessionEnded(replaced = reason == SessionEndReason.REPLACED))
            }
        }
    }
}
