package com.duriseo.android.core.push

import android.content.Context
import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.domain.device.DeviceRegistrar
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 앱 시작 때 `MyApp`이 부른다: 알림 채널 5개를 만들고(D-136), 로그인 상태면 뒤에서 FCM 토큰을 `PUT /me/devices`로 등록한다.
 * 실패(오프라인 등)는 조용히 넘기고 다음 앱 시작 때 다시 한다. FCM이 꺼진 빌드는 등록을 건너뛴다 ([FcmAvailability]).
 * 로그인 상태는 [DeviceRegistrar]가 토큰 저장소를 읽을 때까지 기다려 확인한다.
 */
@Singleton
class PushInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRegistrar: DeviceRegistrar,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        NotificationChannels.createAll(context)
        if (!FcmAvailability.isAvailable(context)) return
        scope.launch {
            try {
                deviceRegistrar.registerCurrentToken()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(TAG, "기기 등록에 실패했어요: ${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "push"
    }
}
