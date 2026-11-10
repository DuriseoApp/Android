package com.duriseo.android.core.common.di

import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.core.common.coroutines.IoDispatcher
import com.duriseo.android.core.common.time.DeviceZoneClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    /**
     * 현재 시각은 이 `Clock`으로 얻는다. 테스트에서 고정한다 (ARCHITECTURE.md 7장).
     * 시간대는 부를 때마다 지금 기기 시간대를 읽는다 (`Clock.systemDefaultZone()`은 만들 때의 값에 묶인다, Q-19).
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = DeviceZoneClock()

    /** 앱 프로세스 수명의 범위. 한 작업이 실패해도 다른 작업은 계속된다. */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
