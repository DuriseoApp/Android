package com.duriseo.android.core.push

import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.domain.device.DeviceRegistrar
import com.duriseo.android.domain.sync.SyncSignalReceiver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * FCM 수신 (API.md 6장·7장). 서버는 모든 메시지를 data 메시지로 보낸다.
 *
 * - [onNewToken]: 로그인 상태면 `PUT /me/devices`로 새 토큰을 등록한다 (실패는 다음 앱 시작 때 다시).
 * - [onMessageReceived]: `type`으로 나눈다. `PUSH`는 프로세스가 없었어도 이 서비스가 알림으로 그린다.
 *   `SYNC`는 프로세스가 살아 있을 때만 처리하고 아니면 무시한다 ([ProcessUiState]).
 *
 * 토큰과 data 내용(알림 문구·ID)을 로그에 남기지 않는다 (NFR-SEC-01, D-46).
 */
@AndroidEntryPoint
class DuriseoMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var deviceRegistrar: DeviceRegistrar

    @Inject
    lateinit var syncReceiver: SyncSignalReceiver

    @Inject
    lateinit var notifier: PushNotifier

    @Inject
    lateinit var processUiState: ProcessUiState

    @Inject
    @field:ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onNewToken(token: String) {
        scope.launch {
            try {
                deviceRegistrar.registerToken(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(TAG, "기기 등록에 실패했어요: ${e.javaClass.simpleName}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val parsed = PushMessageParser.parse(message.data)
        when (parsed) {
            is PushMessage.Visible -> try {
                notifier.show(this, parsed)
            } catch (e: Exception) {
                Logger.w(TAG, "알림을 그리지 못했어요: ${e.javaClass.simpleName}")
            }

            is PushMessage.Sync -> if (processUiState.uiStarted) syncReceiver.receive(parsed.signal)

            null -> Logger.d(TAG, "알 수 없는 푸시 메시지를 버렸어요")
        }
    }

    private companion object {
        const val TAG = "push"
    }
}
