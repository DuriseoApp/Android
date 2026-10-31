package com.duriseo.android.data.repository.di

import com.duriseo.android.data.repository.auth.AuthRepositoryImpl
import com.duriseo.android.data.repository.auth.SessionManager
import com.duriseo.android.data.repository.me.MeRepositoryImpl
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.SessionEndNotifier
import com.duriseo.android.domain.me.MeRepository
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

    /** `data:remote`의 Authenticator가 세션 종료를 알릴 때 쓴다 (`data:remote`는 이 모듈을 모른다) */
    @Binds
    abstract fun bindSessionEndNotifier(impl: SessionManager): SessionEndNotifier
}
