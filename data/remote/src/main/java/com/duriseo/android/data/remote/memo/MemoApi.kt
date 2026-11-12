package com.duriseo.android.data.remote.memo

import com.duriseo.android.data.remote.response.ListResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** 메모 API (API.md 3.4 "메모") 11개 경로. "인증" 클라이언트(`@Authed`)로 만든다 */
interface MemoApi {
    /** 200 `{items, nextCursor, totalCount, favoriteCount}`. `view`는 `newest`·`oldest`·`favorites`, `cursor`는 앞 응답의 `nextCursor` 그대로 */
    @GET("memos")
    suspend fun getMemos(
        @Query("view") view: String,
        @Query("cursor") cursor: String?,
        @Query("size") size: Int?,
    ): Response<MemoPageResponse>

    /** 200 `{items}`. 고정 메모 전체 (최근 고정순) */
    @GET("memos/pinned")
    suspend fun getPinned(): Response<ListResponse<MemoDto>>

    /** 200 `{items}`. 캘린더용. `from`·`to`는 `YYYY-MM-DD`, 최대 3개월 */
    @GET("memos/dated")
    suspend fun getDated(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ListResponse<MemoDto>>

    /** 201 `Memo` */
    @POST("memos")
    suspend fun create(@Body request: CreateMemoRequest): Response<MemoDto>

    /** 200 `Memo` (요청자의 `isFavorite` 포함) */
    @GET("memos/{id}")
    suspend fun getMemo(@Path("id") id: String): Response<MemoDto>

    /** 200 `Memo`. 부분 수정이라 본문은 [JsonObject]다: 없는 필드는 바꾸지 않고, `null`은 그 필드를 지운다 */
    @PATCH("memos/{id}")
    suspend fun update(@Path("id") id: String, @Body request: JsonObject): Response<MemoDto>

    /** 204 */
    @DELETE("memos/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    /** 204, 멱등 (D-134). 본문 없음 */
    @PUT("memos/{id}/pin")
    suspend fun pin(@Path("id") id: String): Response<Unit>

    /** 204, 멱등 (D-134) */
    @DELETE("memos/{id}/pin")
    suspend fun unpin(@Path("id") id: String): Response<Unit>

    /** 204, 멱등 (D-134). 본인 값이라 동기화 신호가 없다 */
    @PUT("memos/{id}/favorite")
    suspend fun favorite(@Path("id") id: String): Response<Unit>

    /** 204, 멱등 (D-134) */
    @DELETE("memos/{id}/favorite")
    suspend fun unfavorite(@Path("id") id: String): Response<Unit>
}
