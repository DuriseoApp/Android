package com.duriseo.android.data.remote.event

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

/** 일정 API (API.md 3.3 "일정"). "인증" 클라이언트(`@Authed`)로 만든다 */
interface EventApi {
    /** 200 `{items: Event[]}`. `from`·`to`는 `YYYY-MM-DD`, `to` 포함, 최대 3개월 */
    @GET("events")
    suspend fun getEvents(@Query("from") from: String, @Query("to") to: String): Response<ListResponse<EventDto>>

    /** 201 `Event` (`reminder`는 항상 null) */
    @POST("events")
    suspend fun create(@Body request: CreateEventRequest): Response<EventDto>

    /** 200 `Event` */
    @GET("events/{id}")
    suspend fun get(@Path("id") id: String): Response<EventDto>

    /**
     * 200 `Event`. 부분 수정이라 본문은 [JsonObject]다: 없는 필드는 바꾸지 않고, `note: null`은 메모를 지운다.
     * (DTO로는 "생략"과 "null"을 구분할 수 없다)
     */
    @PATCH("events/{id}")
    suspend fun update(@Path("id") id: String, @Body request: JsonObject): Response<EventDto>

    /** 204 */
    @DELETE("events/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    /** 200 `Event` (바뀐 `reminder` 포함). 같은 요청을 다시 보내도 같은 결과 */
    @PUT("events/{id}/reminder")
    suspend fun putReminder(@Path("id") id: String, @Body request: ReminderRequest): Response<EventDto>

    /** 204. 알림이 없어도 204 (멱등) */
    @DELETE("events/{id}/reminder")
    suspend fun deleteReminder(@Path("id") id: String): Response<Unit>
}
