package com.duriseo.android.data.remote.memo

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.domain.memo.Memo
import com.duriseo.android.domain.memo.MemoPatch
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.domain.memo.NewMemo
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.result.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `GET /memos`의 한 페이지. [nextCursor]는 불투명 문자열이고 앱은 해석하지 않는다 (API.md 1.1). null이면 마지막 페이지.
 *
 * @property totalCount 요청한 `view`의 전체 개수(고정 제외)
 * @property favoriteCount 내 즐겨찾기 중 고정되지 않은 것의 개수
 */
data class MemoPage(
    val items: List<Memo>,
    val nextCursor: String?,
    val totalCount: Int,
    val favoriteCount: Int,
)

/** 메모 API 호출을 [DataResult]와 도메인 모델로 돌려준다 (API.md 3.4). 오류 코드는 그대로 `ApiError`로 올린다. 응답 배열의 순서를 바꾸지 않는다 */
@Singleton
class MemoRemoteDataSource @Inject constructor(
    private val api: MemoApi,
    private val apiCaller: ApiCaller,
) {
    /** `GET /memos?view=&cursor=&size=` */
    suspend fun list(view: MemoView, cursor: String? = null, size: Int? = null): DataResult<MemoPage> =
        apiCaller.call { api.getMemos(view.code, cursor, size) }
            .map { r -> MemoPage(r.items.map { it.toDomain() }, r.nextCursor, r.totalCount, r.favoriteCount) }

    /** `GET /memos/pinned` */
    suspend fun pinned(): DataResult<List<Memo>> =
        apiCaller.call { api.getPinned() }.map { r -> r.items.map { it.toDomain() } }

    /** `GET /memos/dated?from=&to=` */
    suspend fun dated(from: LocalDate, to: LocalDate): DataResult<List<Memo>> =
        apiCaller.call { api.getDated(from.toString(), to.toString()) }.map { r -> r.items.map { it.toDomain() } }

    /** `POST /memos` (201) */
    suspend fun create(memo: NewMemo): DataResult<Memo> =
        apiCaller.call { api.create(memo.toRequest()) }.map { it.toDomain() }

    /** `GET /memos/{id}` */
    suspend fun get(id: String): DataResult<Memo> = apiCaller.call { api.getMemo(id) }.map { it.toDomain() }

    /** `PATCH /memos/{id}` */
    suspend fun update(id: String, patch: MemoPatch): DataResult<Memo> =
        apiCaller.call { api.update(id, patch.toRequestBody()) }.map { it.toDomain() }

    /** `DELETE /memos/{id}` (204) */
    suspend fun delete(id: String): DataResult<Unit> = apiCaller.callUnit { api.delete(id) }

    /** `PUT`(고정) · `DELETE`(해제) `/memos/{id}/pin` (204) */
    suspend fun setPinned(id: String, pinned: Boolean): DataResult<Unit> =
        apiCaller.callUnit { if (pinned) api.pin(id) else api.unpin(id) }

    /** `PUT`(즐겨찾기) · `DELETE`(해제) `/memos/{id}/favorite` (204) */
    suspend fun setFavorite(id: String, favorite: Boolean): DataResult<Unit> =
        apiCaller.callUnit { if (favorite) api.favorite(id) else api.unfavorite(id) }
}
