package com.duriseo.android.data.repository.auth

import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.me.MeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 시작 때 한 번 하는 일 (`MyApp`이 부른다):
 * 토큰 저장소를 읽어 로그인 상태를 정하고(FR-AUTH-01 AC2·AC3), 로그인 상태면 뒤에서 `GET /me`를 새로 받고
 * 기기 시간대가 다르면 `PATCH /me`로 맞춘다 (NFR-TIME-01). 로그인 상태가 아니면 요청을 보내지 않는다.
 * 로그인 상태가 아니면 남아 있을 수 있는 이전 사용자 데이터를 지운다. 실패는 조용히 넘긴다 (사용자 동작이 아님).
 * 앱으로 돌아올 때도 시간대를 다시 살핀다 ([onForeground], Q-19 ①).
 */
@Singleton
class SessionStartup @Inject constructor(
    private val authRepository: AuthRepository,
    private val meRepository: MeRepository,
    private val sessionManager: SessionManager,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch { run() }
    }

    internal suspend fun run() {
        authRepository.restoreSession()
        if (authRepository.authState.first() != AuthState.SignedIn) {
            // 로그아웃 상태인데 이전 사용자의 Room·설정이 남아 있으면 지운다 (로그아웃·탈퇴·세션 끊김 정리가 중간에 끊긴 경우)
            sessionManager.clearUserData()
            return
        }
        meRepository.refreshMe()
        meRepository.syncTimeZone(clock.zone.id)
    }

    /**
     * 앱으로 돌아왔다 (`MyApp`이 앱 단위로 화면이 하나라도 보이기 시작할 때 부른다, Q-19 ①).
     * 로그인 상태에서 기기 시간대가 저장된 값과 다르면 `PATCH /me`를 다시 보낸다 (같으면 보내지 않는다). 실패는 조용히 넘긴다.
     */
    fun onForeground() {
        scope.launch { syncTimeZoneIfSignedIn() }
    }

    /**
     * [zone]은 지금 기기 시간대다. 주입된 [clock]은 시간대를 부를 때마다 기기에서 읽으므로(`DeviceZoneClock`)
     * 앱이 떠 있는 동안 바뀐 시간대도 담긴다 (Q-19 ①).
     */
    internal suspend fun syncTimeZoneIfSignedIn(zone: ZoneId = clock.zone) {
        if (authRepository.authState.first { it != AuthState.Unknown } != AuthState.SignedIn) return
        meRepository.syncTimeZone(zone.id)
    }
}
