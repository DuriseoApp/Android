package com.duriseo.android.feature.settings.di

import com.duriseo.android.feature.settings.licenses.AndroidLicenseSource
import com.duriseo.android.feature.settings.licenses.LicenseSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** SCR-SET-05의 라이선스 목록 출처. ViewModel은 인터페이스만 안다 (테스트에서 가짜로 바꾼다) */
@Module
@InstallIn(SingletonComponent::class)
abstract class LicensesModule {
    @Binds
    abstract fun bindLicenseSource(impl: AndroidLicenseSource): LicenseSource
}
