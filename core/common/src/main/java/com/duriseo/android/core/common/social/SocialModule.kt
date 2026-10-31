package com.duriseo.android.core.common.social

import com.duriseo.android.domain.auth.SocialSessionCleaner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SocialModule {
    @Binds
    @Singleton
    abstract fun bindSocialTokenProvider(impl: DefaultSocialTokenProvider): SocialTokenProvider

    @Binds
    @Singleton
    abstract fun bindSocialSessionCleaner(impl: DefaultSocialSessionCleaner): SocialSessionCleaner
}
