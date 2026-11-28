package com.duriseo.android.core.ads.di

import com.duriseo.android.core.ads.GoogleMobileAdsSdk
import com.duriseo.android.core.ads.MobileAdsSdk
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {
    @Binds
    @Singleton
    abstract fun bindMobileAdsSdk(impl: GoogleMobileAdsSdk): MobileAdsSdk
}
