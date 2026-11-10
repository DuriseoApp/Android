package com.duriseo.android.data.remote.anniversary

import com.duriseo.android.data.remote.response.CountedPageResponse
import com.duriseo.android.data.remote.response.ListResponse
import com.duriseo.android.data.remote.response.PageResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 기념일 API (API.md 3.3 "기념일"). "인증" 클라이언트(`@Authed`)로 만든다 */
interface AnniversaryApi {
    /** 200 `{items: AnniversaryOccurrence[]}`. 캘린더용. `from`·`to`는 `YYYY-MM-DD`, `to` 포함, 최대 3개월 */
    @GET("anniversaries")
    suspend fun getRange(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ListResponse<AnniversaryOccurrenceDto>>

    /** 200 `{items, nextCursor}`. 기념일 목록용. `cursor`는 앞 응답의 `nextCursor` 그대로, `size`를 생략하면 서버 기본(20) */
    @GET("anniversaries/upcoming")
    suspend fun getUpcoming(
        @Query("cursor") cursor: String?,
        @Query("size") size: Int?,
    ): Response<PageResponse<AnniversaryOccurrenceDto>>

    /** 200 `{items, nextCursor, totalCount}`. "지난 기념일"용 */
    @GET("anniversaries/past")
    suspend fun getPast(
        @Query("cursor") cursor: String?,
        @Query("size") size: Int?,
    ): Response<CountedPageResponse<PastAnniversaryDto>>

    /** 201 `Anniversary` */
    @POST("anniversaries")
    suspend fun create(@Body request: CreateAnniversaryRequest): Response<AnniversaryDto>

    /** 200 `Anniversary`. 부분 수정이라 본문은 [JsonObject]다: 없는 필드는 바꾸지 않는다 (null은 `REQUIRED` 오류라 보내지 않는다) */
    @PATCH("anniversaries/{id}")
    suspend fun update(@Path("id") id: String, @Body request: JsonObject): Response<AnniversaryDto>

    /** 204 */
    @DELETE("anniversaries/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
