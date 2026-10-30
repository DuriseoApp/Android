package com.duriseo.android.data.repository.auth

import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.me.MeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 시작 때 한 번 하는 일 (`MyApp`이 부른다):
 * 토큰 저장소를 읽어 로그인 상태를 정하고(FR-AUTH-01 AC2·AC3), 로그인 상태면 뒤에서 `GET /me`를 새로 받고
 * 기기 시간대가 다르면 `PATCH /me`로 맞춘다 (NFR-TIME-01). 로그인 상태가 아니면 요청을 보내지 않는다.
 * 실패는 조용히 넘긴다 (사용자 동작이 아님).
 */
@Singleton
class SessionStartup @Inject constructor(
    private val authRepository: AuthRepository,
    private val meRepository: MeRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch { run() }
    }

    internal suspend fun run() {
        authRepository.restoreSession()
        if (authRepository.authState.first() != AuthState.SignedIn) return
        meRepository.refreshMe()
        meRepository.syncTimeZone(clock.zone.id)
    }
}
