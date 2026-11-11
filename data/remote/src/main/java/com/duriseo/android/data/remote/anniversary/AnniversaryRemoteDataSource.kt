package com.duriseo.android.data.remote.anniversary

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.domain.anniversary.Anniversary
import com.duriseo.android.domain.anniversary.AnniversaryOccurrence
import com.duriseo.android.domain.anniversary.AnniversaryPatch
import com.duriseo.android.domain.anniversary.NewAnniversary
import com.duriseo.android.domain.anniversary.PastAnniversary
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.result.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 한 페이지. [nextCursor]는 불투명 문자열이고 앱은 해석하지 않는다 (API.md 1.1). null이면 마지막 페이지.
 * [totalCount]는 `past`에서만 값이 있다.
 */
data class AnniversaryPage<T>(
    val items: List<T>,
    val nextCursor: String?,
    val totalCount: Int? = null,
)

/** 기념일 API 호출을 [DataResult]와 도메인 모델로 돌려준다 (API.md 3.3). 오류 코드는 그대로 `ApiError`로 올린다. 응답 배열의 순서를 바꾸지 않는다 */
@Singleton
class AnniversaryRemoteDataSource @Inject constructor(
    private val api: AnniversaryApi,
    private val apiCaller: ApiCaller,
) {
    /** `GET /anniversaries?from=&to=` */
    suspend fun list(from: LocalDate, to: LocalDate): DataResult<List<AnniversaryOccurrence>> =
        apiCaller.call { api.getRange(from.toString(), to.toString()) }
            .map { r -> r.items.mapNotNull { it.toDomainOrNull() } }

    /** `GET /anniversaries/upcoming` */
    suspend fun upcoming(cursor: String? = null, size: Int? = null): DataResult<AnniversaryPage<AnniversaryOccurrence>> =
        apiCaller.call { api.getUpcoming(cursor, size) }
            .map { r -> AnniversaryPage(r.items.mapNotNull { it.toDomainOrNull() }, r.nextCursor) }

    /** `GET /anniversaries/past` */
    suspend fun past(cursor: String? = null, size: Int? = null): DataResult<AnniversaryPage<PastAnniversary>> =
        apiCaller.call { api.getPast(cursor, size) }
            .map { r -> AnniversaryPage(r.items.map { it.toDomain() }, r.nextCursor, r.totalCount) }

    /** `POST /anniversaries` (201) */
    suspend fun create(anniversary: NewAnniversary): DataResult<Anniversary> =
        apiCaller.call { api.create(anniversary.toRequest()) }.map { it.toDomain() }

    /** `PATCH /anniversaries/{id}` */
    suspend fun update(id: String, patch: AnniversaryPatch): DataResult<Anniversary> =
        apiCaller.call { api.update(id, patch.toRequestBody()) }.map { it.toDomain() }

    /** `DELETE /anniversaries/{id}` (204) */
    suspend fun delete(id: String): DataResult<Unit> = apiCaller.callUnit { api.delete(id) }
}
