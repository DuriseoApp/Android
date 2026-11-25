package com.duriseo.android.data.remote.appops

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

/**
 * 운영 공개 조회 (API.md 3.7). "인증 없음" 클라이언트(`@NoAuth`)로 만든다: access 토큰 없이 공통 요청 헤더만 붙는다.
 * Remote Config 값과 같은 코드로 읽으려고 본문을 글자 그대로 받는다 ([AppOpsJsonParser]).
 */
interface AppOpsApi {
    /** 200 `{"items": [AppNotice]}`: 지금 유효한 것만, 모든 플랫폼 */
    @GET("app-notices")
    suspend fun getAppNotices(): Response<ResponseBody>

    /** 200 `{"items": [AppUpdateRule]}`: 모든 규칙, 모든 플랫폼 */
    @GET("app-update-rules")
    suspend fun getAppUpdateRules(): Response<ResponseBody>
}
