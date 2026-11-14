package com.duriseo.android.core.push

import com.duriseo.android.domain.device.PushTokenProvider
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
}
