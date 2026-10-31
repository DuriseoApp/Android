package com.duriseo.android.data.remote.couple

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

/**
 * 공간·커플 API (API.md 3.2). "인증" 클라이언트(`@Authed`)로 만든다.
 * `GET /space`는 단위 3이 부르지 않는다 (`GET /me`의 `MeSpace`로 충분, 진행 기록 결정 3).
 */
interface CoupleApi {
    /** 201 `InviteCode`. 본문 없음. 마지막 발급 후 60초 안이면 429 `INVITE_REISSUE_TOO_SOON` */
    @POST("invite-codes")
    suspend fun issueInviteCode(): Response<InviteCodeDto>

    /** 200 `InviteCode`. 유효한 코드가 없으면 404 `NOT_FOUND` */
    @GET("invite-codes/current")
    suspend fun getCurrentInviteCode(): Response<InviteCodeDto>

    /** 200 `CoupleJoinResult` */
    @POST("couple/join")
    suspend fun join(@Body request: JoinCoupleRequest): Response<CoupleJoinResultDto>

    /** 204 */
    @POST("couple/import")
    suspend fun decideImport(@Body request: ImportRequest): Response<Unit>

    /** 200 `Space` */
    @PATCH("space")
    suspend fun patchSpace(@Body request: UpdateStartDateRequest): Response<SpaceDto>

    /** 200 `Space` */
    @PATCH("space/colors")
    suspend fun patchColors(@Body request: UpdateColorsRequest): Response<SpaceDto>

    /** 204. 본문 없음 */
    @DELETE("couple")
    suspend fun unlink(): Response<Unit>
}
