package com.duriseo.android.data.repository.device

import com.duriseo.android.data.remote.me.MeRemoteDataSource
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.auth.TokenState
import com.duriseo.android.domain.auth.TokenStorage
import com.duriseo.android.domain.device.DeviceRegistrar
import com.duriseo.android.domain.device.PushTokenProvider
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기기 등록 (`PUT /me/devices`, API.md 3.1): 로그인 상태(토큰 저장소에 토큰이 있음)에서만 보낸다.
 * 토큰 저장소를 아직 읽는 중이면 읽을 때까지 기다린다 (앱이 막 시작했을 때). 오프라인이면 요청 없이 `Offline`이고
 * 호출한 쪽이 조용히 넘긴다 (다음 시작 때 다시 등록한다). FCM 토큰은 로그에도 예외 메시지에도 남기지 않는다 (NFR-SEC-01).
 */
@Singleton
class DeviceRegistrarImpl @Inject constructor(
    private val remote: MeRemoteDataSource,
    private val tokenStorage: TokenStorage,
    private val networkMonitor: NetworkMonitor,
    private val pushTokens: PushTokenProvider,
) : DeviceRegistrar {

    override suspend fun registerCurrentToken(): DataResult<Unit> {
        if (!isSignedIn()) return DataResult.Success(Unit)
        // 토큰이 없으면 FCM이 꺼진 빌드나 아직 못 받은 상태다: 보낼 것이 없다
        val token = pushTokens.currentToken() ?: return DataResult.Success(Unit)
        return send(token)
    }

    override suspend fun registerToken(token: String): DataResult<Unit> {
        if (!isSignedIn()) return DataResult.Success(Unit)
        return send(token)
    }

    private suspend fun isSignedIn(): Boolean =
        tokenStorage.state.first { it != TokenState.Loading } is TokenState.Present

    private suspend fun send(token: String): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.putDevice(token) }
}
