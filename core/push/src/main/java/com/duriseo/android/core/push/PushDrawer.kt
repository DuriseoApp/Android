package com.duriseo.android.core.push

import android.content.Context
import com.duriseo.android.domain.auth.TokenState
import com.duriseo.android.domain.auth.TokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인 세션이 있을 때만 `PUSH`를 알림으로 그린다 (NFR-SEC-02). 로그아웃한 기기로 늦게 온 푸시를 그리면
 * 이전 사용자의 알림이 남는다. 앱 시작 직후라 토큰 저장소를 아직 읽는 중이면 잠시(5초) 기다리고, 그래도 모르면 그리지 않는다.
 */
@Singleton
class PushDrawer @Inject constructor(
    private val notifier: PushNotifier,
    private val tokenStorage: TokenStorage,
) {
    /** 그렸으면 true. 세션이 없거나 알림 권한이 없으면 false */
    suspend fun draw(context: Context, message: PushMessage.Visible): Boolean {
        val state = withTimeoutOrNull(STATE_WAIT_MILLIS) {
            tokenStorage.state.first { it != TokenState.Loading }
        }
        if (state !is TokenState.Present) return false
        return notifier.show(context, message)
    }

    private companion object {
        const val STATE_WAIT_MILLIS = 5_000L
    }
}
