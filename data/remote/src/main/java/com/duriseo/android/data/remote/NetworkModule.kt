package com.duriseo.android.data.remote

import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.network.AndroidNetworkMonitor
import com.duriseo.android.data.remote.network.ServerConfigInterceptor
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 인증 없는 Retrofit 클라이언트 하나 (단위 1). 인증(`Authenticator`, 토큰 갱신)은 단위 2.
 * 서버 주소는 [AppConfig]의 호스트에 `/api/v1/`을 붙여 쓴다 (API.md 1장).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val APPLICATION_JSON = "application/json"

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

    private fun OkHttpClient.Builder.commonTimeouts(): OkHttpClient.Builder = apply {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        callTimeout(30, TimeUnit.SECONDS)
    }

    @Provides
    @Singleton
    fun provideNoAuthOkHttpClient(
        config: AppConfig,
        logging: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .commonTimeouts()
        .addInterceptor(ServerConfigInterceptor(config))
        .addInterceptor(logging)
        .build()

    /**
     * Retrofit은 처음 쓸 때 만들어진다(Hilt 싱글턴). 서버 주소가 비어 있어도(CI) 만들다가 죽지 않고,
     * 요청을 보낼 때 [ServerConfigInterceptor]가 그 요청만 실패시킨다.
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        config: AppConfig,
        client: OkHttpClient,
        factory: Converter.Factory,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(resolveApiBaseUrl(config)?.toString() ?: UNCONFIGURED_BASE_URL)
        .addConverterFactory(factory)
        .client(client)
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorModule {
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
}
