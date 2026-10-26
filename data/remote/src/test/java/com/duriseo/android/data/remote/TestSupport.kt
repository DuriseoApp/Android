package com.duriseo.android.data.remote

import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.response.CountedPageResponse
import com.duriseo.android.data.remote.response.ListResponse
import com.duriseo.android.data.remote.response.PageResponse
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** 테스트용 DTO. 일시·날짜는 API.md 1장 형식 */
@Serializable
data class SampleDto(
    val id: String,
    val note: String?,
    @Contextual val createdAt: Instant,
    @Contextual val day: LocalDate? = null,
)

/** 테스트용 Retrofit 서비스. 서비스 메서드는 `Response<T>`를 돌려준다 (ApiCaller 규약) */
interface SampleApi {
    @GET("sample")
    suspend fun getSample(): Response<SampleDto>

    @GET("items")
    suspend fun getItems(): Response<ListResponse<SampleDto>>

    @GET("page")
    suspend fun getPage(): Response<PageResponse<SampleDto>>

    @GET("counted")
    suspend fun getCounted(): Response<CountedPageResponse<SampleDto>>

    @DELETE("sample")
    suspend fun delete(): Response<Unit>
}

/** 테스트에서 쓰는 JSON·Retrofit 만들기 */
fun sampleApi(server: MockWebServer, client: OkHttpClient = OkHttpClient()): SampleApi =
    Retrofit.Builder()
        .baseUrl(server.url("/api/v1/"))
        .addConverterFactory(createApiJson().asConverterFactory("application/json".toMediaType()))
        .client(client)
        .build()
        .create(SampleApi::class.java)

/** 시간이 오래 걸리지 않게 짧은 제한을 둔 클라이언트. 연결 실패 때 다시 시도하지 않는다. */
fun fastFailClient(): OkHttpClient = OkHttpClient.Builder()
    .retryOnConnectionFailure(false)
    .connectTimeout(2, TimeUnit.SECONDS)
    .readTimeout(500, TimeUnit.MILLISECONDS)
    .build()

fun jsonResponse(code: Int, body: String): MockResponse = MockResponse()
    .setResponseCode(code)
    .setHeader("Content-Type", "application/json")
    .setBody(body)
