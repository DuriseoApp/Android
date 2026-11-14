package com.duriseo.android.core.push

import com.duriseo.android.domain.device.PushTokenProvider
import com.duriseo.android.domain.notification.DrawnNotificationCleaner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PushModule {
    /** `data:repository`의 `DeviceRegistrar`가 FCM 토큰을 얻는 곳 */
    @Binds
    abstract fun bindPushTokenProvider(impl: FirebasePushTokenProvider): PushTokenProvider

    /** `data:repository`의 `SessionManager`가 로그아웃 때 알림창의 푸시 알림을 지우는 곳 */
    @Binds
    abstract fun bindDrawnNotificationCleaner(impl: PushNotificationCleaner): DrawnNotificationCleaner
}
