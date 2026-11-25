package com.duriseo.android.data.remote.appops

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.SuccessResult
import com.duriseo.android.core.common.image.PopupImageLoader
import com.duriseo.android.core.common.image.popupImageRequest
import com.duriseo.android.data.remote.network.NoAuth
import com.duriseo.android.data.remote.network.ForceUpdateSignalHub
import com.duriseo.android.domain.appops.ForceUpdateSignals
import com.duriseo.android.domain.appops.PopupImagePrefetcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** 팝업 이미지를 받는 OkHttp. 토큰·공통 헤더·서버 주소 검사를 붙이지 않는다 (unit-9 결정 3) */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PopupImageClient

/** 팝업 이미지용 OkHttp: 인터셉터가 없고, https에서 http로 가는 이동은 따르지 않는다 */
internal fun createPopupImageClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .callTimeout(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .followSslRedirects(false)
    .build()

private const val IMAGE_TIMEOUT_SECONDS = 10L

/** Coil로 받아 두는 [PopupImagePrefetcher]. 받은 이미지는 같은 로더의 캐시에 남아 팝업이 바로 그린다 (Q-23) */
internal class CoilPopupImagePrefetcher(
    private val context: Context,
    private val loader: ImageLoader,
) : PopupImagePrefetcher {
    override suspend fun prefetch(url: String): Boolean = try {
        loader.execute(popupImageRequest(context, url)) is SuccessResult
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
}

/** 운영(공지·업데이트 팝업) 데이터의 Hilt 연결 (단위 9) */
@Module
@InstallIn(SingletonComponent::class)
object AppOpsModule {

    @Provides
    @Singleton
    fun provideAppOpsApi(@NoAuth retrofit: Retrofit): AppOpsApi = retrofit.create(AppOpsApi::class.java)

    @Provides
    @Singleton
    fun provideRemoteConfigGateway(@ApplicationContext context: Context): RemoteConfigGateway =
        createRemoteConfigGateway(context)

    @Provides
    @Singleton
    @PopupImageClient
    fun providePopupImageClient(): OkHttpClient = createPopupImageClient()

    @Provides
    @Singleton
    @PopupImageLoader
    fun providePopupImageLoader(
        @ApplicationContext context: Context,
        @PopupImageClient client: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
        .build()

    @Provides
    @Singleton
    fun providePopupImagePrefetcher(
        @ApplicationContext context: Context,
        @PopupImageLoader loader: ImageLoader,
    ): PopupImagePrefetcher = CoilPopupImagePrefetcher(context, loader)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ForceUpdateSignalsModule {
    /** 426 신호를 앱 전체가 듣는 곳. 인터셉터가 채우는 [ForceUpdateSignalHub]와 같은 인스턴스다 (`@Singleton`) */
    @Binds
    abstract fun bindForceUpdateSignals(impl: ForceUpdateSignalHub): ForceUpdateSignals
}
