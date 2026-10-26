package com.duriseo.android.data.repository.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 리포지토리 구현 바인딩 자리 (domain 인터페이스 -> 구현, `@Binds`).
 * 단위 1에는 리포지토리가 없다. 기능 단위가 자기 리포지토리를 여기에 추가한다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule
