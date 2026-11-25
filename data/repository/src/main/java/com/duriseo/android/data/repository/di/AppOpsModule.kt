package com.duriseo.android.data.repository.di

import com.duriseo.android.core.common.config.OsVersion
import com.duriseo.android.core.common.update.AppVersion
import com.duriseo.android.data.local.appops.PopupHiddenTodayLocalDataSource
import com.duriseo.android.data.repository.appops.AppOpsRepositoryImpl
import com.duriseo.android.domain.appops.AppEnvironment
import com.duriseo.android.domain.appops.AppOpsRepository
import com.duriseo.android.domain.appops.AppPlatform
import com.duriseo.android.domain.appops.AppPopupCoordinator
import com.duriseo.android.domain.appops.ForceUpdateSignals
import com.duriseo.android.domain.appops.PopupHiddenTodayStore
import com.duriseo.android.domain.appops.PopupImagePrefetcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** 운영 팝업(단위 9)의 domain 인터페이스 → 구현 연결 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppOpsBindingsModule {
    @Binds
    abstract fun bindAppOpsRepository(impl: AppOpsRepositoryImpl): AppOpsRepository

    /** "오늘 하루 안 보기": 기기 단위 DataStore 키 (D-179) */
    @Binds
    abstract fun bindPopupHiddenTodayStore(impl: PopupHiddenTodayLocalDataSource): PopupHiddenTodayStore
}

/** `domain`은 Hilt를 몰라서 [AppPopupCoordinator]는 여기서 만든다. 이 앱의 플랫폼·빌드·OS 버전을 규칙 판단에 넣는다 */
@Module
@InstallIn(SingletonComponent::class)
object AppOpsProvidesModule {
    @Provides
    @Singleton
    fun provideAppPopupCoordinator(
        repository: AppOpsRepository,
        hiddenStore: PopupHiddenTodayStore,
        forceSignals: ForceUpdateSignals,
        prefetcher: PopupImagePrefetcher,
        version: AppVersion,
        os: OsVersion,
        clock: Clock,
    ): AppPopupCoordinator = AppPopupCoordinator(
        repository = repository,
        hiddenStore = hiddenStore,
        forceSignals = forceSignals,
        prefetcher = prefetcher,
        environment = AppEnvironment(platform = AppPlatform.ANDROID, build = version.code, osVersion = os.apiLevel),
        clock = clock,
    )
}
