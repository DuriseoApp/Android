package com.duriseo.android.data.remote.me

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT

/** 내 정보 API (API.md 3.1). "인증" 클라이언트(`@Authed`)로 만든다 */
interface MeApi {
    /** 200 `Me` */
    @GET("me")
    suspend fun getMe(): Response<MeDto>

    /** 200 `Me`. 없는 필드는 바꾸지 않는다. 단위 2는 `timeZone`만 보낸다 */
    @PATCH("me")
    suspend fun patchMe(@Body request: UpdateMeRequest): Response<MeDto>

    /** 204. 탈퇴 (4.6) */
    @DELETE("me")
    suspend fun deleteMe(): Response<Unit>

    /** 204. FCM 토큰 등록·갱신. 호출하는 쪽은 단위 7이다 */
    @PUT("me/devices")
    suspend fun putDevice(@Body request: RegisterDeviceRequest): Response<Unit>
}
