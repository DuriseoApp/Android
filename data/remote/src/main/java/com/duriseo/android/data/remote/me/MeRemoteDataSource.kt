package com.duriseo.android.data.remote.me

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.result.map
import javax.inject.Inject
import javax.inject.Singleton

/** 내 정보 API 호출을 [DataResult]와 도메인 모델로 돌려준다. `PUT /me/devices`는 단위 7이 더한다 */
@Singleton
class MeRemoteDataSource @Inject constructor(
    private val api: MeApi,
    private val apiCaller: ApiCaller,
) {
    suspend fun getMe(): DataResult<Me> = apiCaller.call { api.getMe() }.map { it.toDomain() }

    /** `PATCH /me {timeZone}` (NFR-TIME-01). `nickname`은 보내지 않는다 */
    suspend fun patchTimeZone(timeZone: String): DataResult<Me> =
        apiCaller.call { api.patchMe(UpdateMeRequest(timeZone = timeZone)) }.map { it.toDomain() }

    suspend fun deleteMe(): DataResult<Unit> = apiCaller.callUnit { api.deleteMe() }
}
