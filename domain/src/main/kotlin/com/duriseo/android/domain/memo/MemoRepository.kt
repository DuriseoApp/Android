package com.duriseo.android.domain.memo

import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 메모 (API.md 3.4). 화면은 Room의 `observe…`만 구독한다 (NFR-OFF-01, ARCHITECTURE.md 2장).
 *
 * - 응답 배열의 **순서를 그대로** 쓴다. 보기별·고정·날짜 목록마다 위치를 함께 저장하고 앱이 다시 정렬하지 않는다 (API.md 3.4 "앱 동작").
 * - `refresh…`·`loadMore…`는 읽기라서 오프라인이면 요청 없이 `DataError.Offline`이다. 부른 쪽이 조용히 넘긴다.
 * - 쓰기([create]·[update]·[delete]·[setPinned]·[setFavorite])는 온라인에서만 한다. 오프라인이면 요청 없이 `Offline` (NFR-OFF-02).
 *   성공하면 지금까지 받은 보기들의 첫 페이지·고정 목록·[setVisibleRange]로 알린 캘린더 범위(와 이미 저장된 그 메모의 상세)를 다시 불러온다 (낙관적 갱신 없음).
 * - 404 `NOT_FOUND`면 Room에서도 그 메모를 지운다. 403 `NOT_AUTHOR`면 [refreshAll]과 그 메모 상세를 다시 불러온다 (D-121).
 */
interface MemoRepository {
    /** [view]의 보기 목록. 고정된 메모는 없다 (D-130). 위치 순서 그대로 */
    fun observeList(view: MemoView): Flow<MemoList>

    /** "★ 즐겨찾기 N": 내 즐겨찾기 중 고정되지 않은 것의 개수 (`GET /memos`의 `favoriteCount`). 받은 적이 없으면 0 */
    fun observeFavoriteCount(): Flow<Int>

    /** 고정 메모 전체, 최근 고정순 (서버 순서). 핀 N = 길이. 4개 이상이면 앞의 3개만 보이고 더보기 (FR-MEMO-05) */
    fun observePinned(): Flow<List<Memo>>

    /** [from]~[to](포함)에 메모 날짜가 있는 메모. 캘린더용이고 메모 날짜 → 위치 순서다 */
    fun observeDated(from: LocalDate, to: LocalDate): Flow<List<Memo>>

    /** 메모 하나 (SCR-MEMO-03). 저장된 것 중 가장 최근에 받은 값이고 없으면 null. 없으면 [refreshMemo]로 받는다 */
    fun observeMemo(id: String): Flow<Memo?>

    /**
     * 캘린더가 지금 보이는 범위를 알린다. 쓰기 뒤·[refreshAll]이 이 범위를 다시 불러온다 (달이 바뀔 때마다 부른다).
     * 캐시를 지우면(해제·로그아웃·공간 변경) 이 값도 비워지므로 캘린더가 다시 알려야 한다.
     */
    fun setVisibleRange(from: LocalDate, to: LocalDate)

    /** `GET /memos?view=` 첫 페이지: 그 보기의 목록·`totalCount`와 `favoriteCount`를 교체한다 (화면에 들어갈 때, 보기를 바꿀 때, 당겨서 새로고침) */
    suspend fun refreshList(view: MemoView): DataResult<Unit>

    /** `GET /memos?view=&cursor=` 다음 페이지를 이어 붙인다. 다음 페이지가 없으면 요청 없이 성공 */
    suspend fun loadMoreList(view: MemoView): DataResult<Unit>

    /** `GET /memos/pinned`: 고정 목록을 교체한다 */
    suspend fun refreshPinned(): DataResult<Unit>

    /**
     * `GET /memos/dated?from=&to=`. 범위를 1950-01-01 ~ 2050-12-31 안으로 잘라 요청하고(밖이면 요청 없이 성공),
     * 응답을 받으면 그 범위의 메모를 Room에서 지우고 새로 넣는다. 요청 뒤 캐시가 지워졌거나 겹치는 더 새 응답이 이미 저장됐으면 저장하지 않는다.
     * 월 조회의 앞뒤 하루 여유(Q-19 ②)는 부르는 쪽이 정한다 ([com.duriseo.android.domain.anniversary.calendarMonthRange]).
     */
    suspend fun refreshDated(from: LocalDate, to: LocalDate): DataResult<Unit>

    /** `GET /memos/{id}`: 상세를 저장한다. 404면 기기에서 그 메모를 지운다 (D-121) */
    suspend fun refreshMemo(id: String): DataResult<Unit>

    /**
     * 지금까지 받은 보기들의 첫 페이지와 고정 목록, 보이는 캘린더 범위를 다시 불러온다 (당겨서 새로고침, 403 뒤).
     * 하나라도 실패하면 첫 실패를 돌려준다.
     */
    suspend fun refreshAll(): DataResult<Unit>

    /** `POST /memos` (201). 응답을 상세로 저장하고 목록·고정·달을 다시 불러온다 */
    suspend fun create(memo: NewMemo): DataResult<Memo>

    /** `PATCH /memos/{id}`. 바뀐 것이 없으면([MemoPatch.isEmpty]) 요청 없이 성공으로 끝낸다 (D-123) */
    suspend fun update(id: String, patch: MemoPatch): DataResult<Unit>

    /** `DELETE /memos/{id}` (204). 성공하면 기기에서도 지우고 목록·고정·달을 다시 불러온다 */
    suspend fun delete(id: String): DataResult<Unit>

    /** `PUT`(고정) 또는 `DELETE`(해제) `/memos/{id}/pin` (204, 멱등, D-134). 누구든 할 수 있다 */
    suspend fun setPinned(id: String, pinned: Boolean): DataResult<Unit>

    /** `PUT`(즐겨찾기) 또는 `DELETE`(해제) `/memos/{id}/favorite` (204, 멱등, D-134). 나에게만 보인다 */
    suspend fun setFavorite(id: String, favorite: Boolean): DataResult<Unit>
}
