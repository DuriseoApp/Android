package com.duriseo.android.data.remote.notification

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** 알림 목록 API (API.md 3.5) 3개 경로. "인증" 클라이언트(`@Authed`)로 만든다 */
interface NotificationApi {
    /** 200 `{items, nextCursor}`. 최근 30일, 최신순. `cursor`는 앞 응답의 `nextCursor` 그대로 */
    @GET("notifications")
    suspend fun getNotifications(
        @Query("cursor") cursor: String?,
        @Query("size") size: Int?,
    ): Response<NotificationPageResponse>

    /** 200 `{count}` */
    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    /** 204, 멱등. 본문 없음 */
    @PUT("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): Response<Unit>
}
