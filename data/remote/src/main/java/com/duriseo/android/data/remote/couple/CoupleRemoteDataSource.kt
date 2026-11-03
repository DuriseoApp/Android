package com.duriseo.android.data.remote.couple

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.couple.Space
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.result.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 공간·커플 API 호출을 [DataResult]와 도메인 모델로 돌려준다 (API.md 3.2). 오류 코드는 그대로 `ApiError`로 올린다 */
@Singleton
class CoupleRemoteDataSource @Inject constructor(
    private val api: CoupleApi,
    private val apiCaller: ApiCaller,
) {
    /** `POST /invite-codes` (201) */
    suspend fun issueInviteCode(): DataResult<InviteCode> =
        apiCaller.call { api.issueInviteCode() }.map { it.toDomain() }

    /** `GET /invite-codes/current`. 404 `NOT_FOUND`는 오류가 아니라 "유효한 코드 없음"이라 `Success(null)` (API.md 4.4의 1단계) */
    suspend fun currentInviteCode(): DataResult<InviteCode?> {
        val result = apiCaller.call { api.getCurrentInviteCode() }
        if (result is DataResult.Failure) {
            val error = result.error
            if (error is DataError.ApiError && error.knownCode == ErrorCode.NOT_FOUND) return DataResult.Success(null)
        }
        return result.map { it.toDomain() }
    }

    /** `POST /couple/join {code}` (200) */
    suspend fun join(code: String): DataResult<CoupleJoinResult> =
        apiCaller.call { api.join(JoinCoupleRequest(code)) }.map { it.toDomain() }

    /** `POST /couple/import {import}` (204) */
    suspend fun decideImport(import: Boolean): DataResult<Unit> =
        apiCaller.callUnit { api.decideImport(ImportRequest(import)) }

    /** `PATCH /space {startDate}` (200) */
    suspend fun updateStartDate(date: LocalDate): DataResult<Space> =
        apiCaller.call { api.patchSpace(UpdateStartDateRequest(date)) }.map { it.toDomain() }

    /** `PATCH /space/colors` (200). 바뀐 필드만 보낸다 */
    suspend fun updateColors(change: ColorChange): DataResult<Space> =
        apiCaller.call { api.patchColors(change.toRequest()) }.map { it.toDomain() }

    /** `DELETE /couple` (204) */
    suspend fun unlink(): DataResult<Unit> = apiCaller.callUnit { api.unlink() }
}
