package com.duriseo.android.data.remote.image

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.core.common.coroutines.IoDispatcher
import com.duriseo.android.data.remote.network.Authed
import com.duriseo.android.domain.profile.ProfileImageCacheCleaner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 프로필 사진을 받는 Coil `ImageLoader` (API.md `GET /users/{id}/profile-image`, 단위 8).
 *
 * 앱의 "인증" OkHttp(`@Authed`: Bearer 헤더, 401 `TOKEN_EXPIRED`면 토큰 갱신)로 받아서 사진도 API와 같은 인증·갱신을 쓴다.
 * 캐시 키는 요청 주소(사용자 ID + 버전)라서 사진이 바뀌면 새로 받는다. 404·실패는 Coil의 오류 상태가 되고 `DuriseoAvatar`가
 * 사람 색 + 글자로 그린다 (FR-AUTH-07 AC4). 앱은 이 로더를 `SingletonImageLoader.Factory`(`MyApp`)로 쓴다.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProfileImageModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        config: AppConfig,
        @Authed client: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(ProfileImageMapper(config))
            add(OkHttpNetworkFetcherFactory(callFactory = { client }))
        }
        .build()

    /** 로그아웃·탈퇴·세션 끊김·계정 전환 때 받아 둔 사진을 지운다. 로더는 `Provider`로 늦게 받아 의존 순환을 피한다 */
    @Provides
    @Singleton
    fun provideProfileImageCacheCleaner(
        loader: Provider<ImageLoader>,
        @IoDispatcher io: CoroutineDispatcher,
    ): ProfileImageCacheCleaner = CoilProfileImageCacheCleaner(loader, io)
}
