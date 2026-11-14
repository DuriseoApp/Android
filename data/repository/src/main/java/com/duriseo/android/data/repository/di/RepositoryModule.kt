package com.duriseo.android.data.repository.di

import com.duriseo.android.data.repository.anniversary.AnniversaryRepositoryImpl
import com.duriseo.android.data.repository.auth.AuthRepositoryImpl
import com.duriseo.android.data.repository.auth.SessionManager
import com.duriseo.android.data.repository.couple.CoupleRepositoryImpl
import com.duriseo.android.data.repository.device.DeviceRegistrarImpl
import com.duriseo.android.data.repository.event.EventRepositoryImpl
import com.duriseo.android.data.repository.me.MeRepositoryImpl
import com.duriseo.android.data.repository.memo.MemoRepositoryImpl
import com.duriseo.android.data.repository.notification.NotificationRepositoryImpl
import com.duriseo.android.data.repository.sync.SyncDispatcher
import com.duriseo.android.domain.anniversary.AnniversaryRepository
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.SessionEndNotifier
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.device.DeviceRegistrar
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.notification.NotificationRepository
import com.duriseo.android.domain.sync.SyncSignalReceiver
import com.duriseo.android.domain.sync.SyncSpaceSignals
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 리포지토리 구현 바인딩 (domain 인터페이스 -> 구현, `@Binds`). 기능 단위가 자기 리포지토리를 여기에 더한다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindMeRepository(impl: MeRepositoryImpl): MeRepository

    @Binds
    abstract fun bindCoupleRepository(impl: CoupleRepositoryImpl): CoupleRepository

    @Binds
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    abstract fun bindAnniversaryRepository(impl: AnniversaryRepositoryImpl): AnniversaryRepository

    @Binds
    abstract fun bindMemoRepository(impl: MemoRepositoryImpl): MemoRepository

    @Binds
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    /** 로그인 직후·토큰 갱신·앱 시작에 `PUT /me/devices` (단위 7). FCM 토큰은 `PushTokenProvider`(`core:push`)가 준다 */
    @Binds
    abstract fun bindDeviceRegistrar(impl: DeviceRegistrarImpl): DeviceRegistrar

    /** `push`가 신호를 넘기는 곳 (단위 7) */
    @Binds
    abstract fun bindSyncSignalReceiver(impl: SyncDispatcher): SyncSignalReceiver

    /** `MainViewModel`이 `LINKED`·`UNLINKED` 신호를 듣는 곳 (단위 7). 같은 인스턴스다 (`@Singleton`) */
    @Binds
    abstract fun bindSyncSpaceSignals(impl: SyncDispatcher): SyncSpaceSignals

    /** `data:remote`의 Authenticator가 세션 종료를 알릴 때 쓴다 (`data:remote`는 이 모듈을 모른다) */
    @Binds
    abstract fun bindSessionEndNotifier(impl: SessionManager): SessionEndNotifier
}
