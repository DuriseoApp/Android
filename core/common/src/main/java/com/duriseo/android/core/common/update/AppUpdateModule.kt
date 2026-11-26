package com.duriseo.android.core.common.update

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppUpdateModule {
    @Binds
    abstract fun bindAppUpdateSource(impl: PlayAppUpdateSource): AppUpdateSource
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppForegroundModule {
    /** 같은 인스턴스다 (`@Singleton`): `MyApp`은 [AppForegroundTracker]로 알리고 화면은 [AppForegroundEvents]로 듣는다 */
    @Binds
    abstract fun bindAppForegroundEvents(impl: AppForegroundTracker): AppForegroundEvents
}
