package com.duriseo.android.data.remote

import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.data.remote.auth.AuthApi
import com.duriseo.android.data.remote.auth.AuthSessionApi
import com.duriseo.android.data.remote.couple.CoupleApi
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.me.MeApi
import com.duriseo.android.data.remote.network.AndroidNetworkMonitor
import com.duriseo.android.data.remote.network.AuthInterceptor
import com.duriseo.android.data.remote.network.Authed
import com.duriseo.android.data.remote.network.NoAuth
import com.duriseo.android.data.remote.network.ServerConfigInterceptor
import com.duriseo.android.data.remote.network.TokenAuthenticator
import com.duriseo.android.data.remote.network.UNCONFIGURED_BASE_URL
import com.duriseo.android.data.remote.network.createLoggingInterceptor
import com.duriseo.android.data.remote.network.resolveApiBaseUrl
import com.duriseo.android.domain.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val APPLICATION_JSON = "application/json"

private fun OkHttpClient.Builder.commonTimeouts(): OkHttpClient.Builder = apply {
    connectTimeout(30, TimeUnit.SECONDS)
    readTimeout(30, TimeUnit.SECONDS)
    writeTimeout(30, TimeUnit.SECONDS)
    callTimeout(30, TimeUnit.SECONDS)
}

/** "인증 없음" 클라이언트: `Authorization`을 붙이지 않는다 (`/auth/guest`, `/auth/social`, `/auth/refresh`). */
internal fun createNoAuthClient(config: AppConfig, logging: Interceptor): OkHttpClient =
    OkHttpClient.Builder()
        .commonTimeouts()
        .addInterceptor(ServerConfigInterceptor(config))
        .addInterceptor(logging)
        .build()

/**
 * "인증" 클라이언트: `Authorization: Bearer <access>`를 붙이고, 401 `TOKEN_EXPIRED`면 [TokenAuthenticator]가
 * refresh를 한 번만 부른 뒤 다시 보낸다. 로깅은 맨 끝에 둬서 붙은 헤더(값은 가려짐)까지 본다.
 */
internal fun createAuthedClient(
    config: AppConfig,
    logging: Interceptor,
    authInterceptor: Interceptor,
    authenticator: okhttp3.Authenticator,
): OkHttpClient = OkHttpClient.Builder()
    .commonTimeouts()
    .addInterceptor(ServerConfigInterceptor(config))
    .addInterceptor(authInterceptor)
    .addInterceptor(logging)
    .authenticator(authenticator)
    .build()

/**
 * Retrofit 클라이언트 둘: 인증 없음(`@NoAuth`)과 인증(`@Authed`). 서버 주소는 [AppConfig]의 호스트에
 * `/api/v1/`을 붙여 쓴다 (API.md 1장).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = createApiJson()

    @Provides
    @Singleton
    fun provideJsonConverter(json: Json): Converter.Factory =
        json.asConverterFactory(APPLICATION_JSON.toMediaType())

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        createLoggingInterceptor(enabled = BuildConfig.DEBUG) { message -> Logger.i("okhttp", message) }

    @Provides
    @Singleton
    @NoAuth
    fun provideNoAuthOkHttpClient(
        config: AppConfig,
        logging: HttpLoggingInterceptor,
    ): OkHttpClient = createNoAuthClient(config, logging)

    @Provides
    @Singleton
    @Authed
    fun provideAuthedOkHttpClient(
        config: AppConfig,
        logging: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient = createAuthedClient(config, logging, authInterceptor, authenticator)

    /**
     * Retrofit은 처음 쓸 때 만들어진다(Hilt 싱글턴). 서버 주소가 비어 있어도(CI) 만들다가 죽지 않고,
     * 요청을 보낼 때 [ServerConfigInterceptor]가 그 요청만 실패시킨다.
     */
    @Provides
    @Singleton
    @NoAuth
    fun provideNoAuthRetrofit(
        config: AppConfig,
        @NoAuth client: OkHttpClient,
        factory: Converter.Factory,
    ): Retrofit = buildRetrofit(config, client, factory)

    @Provides
    @Singleton
    @Authed
    fun provideAuthedRetrofit(
        config: AppConfig,
        @Authed client: OkHttpClient,
        factory: Converter.Factory,
    ): Retrofit = buildRetrofit(config, client, factory)

    private fun buildRetrofit(config: AppConfig, client: OkHttpClient, factory: Converter.Factory): Retrofit =
        Retrofit.Builder()
            .baseUrl(resolveApiBaseUrl(config)?.toString() ?: UNCONFIGURED_BASE_URL)
            .addConverterFactory(factory)
            .client(client)
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(@NoAuth retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAuthSessionApi(@Authed retrofit: Retrofit): AuthSessionApi =
        retrofit.create(AuthSessionApi::class.java)

    @Provides
    @Singleton
    fun provideMeApi(@Authed retrofit: Retrofit): MeApi = retrofit.create(MeApi::class.java)

    @Provides
    @Singleton
    fun provideCoupleApi(@Authed retrofit: Retrofit): CoupleApi = retrofit.create(CoupleApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorModule {
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
}
