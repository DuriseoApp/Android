package com.duriseo.android.data.local.memo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 메모 캐시. 읽기 순서는 응답 위치([MemoEntity.position]) 그대로다.
 * 날짜는 `YYYY-MM-DD`라 문자열 비교가 날짜 비교와 같다.
 */
@Dao
abstract class MemoDao {
    // --- 보기별 목록·고정 목록 (위치 순서) ---

    @Query("SELECT * FROM memo WHERE list_type = :listType ORDER BY position")
    abstract fun observeList(listType: String): Flow<List<MemoEntity>>

    @Query("DELETE FROM memo WHERE list_type = :listType")
    abstract suspend fun deleteList(listType: String)

    @Query("SELECT COALESCE(MAX(position), -1) FROM memo WHERE list_type = :listType")
    abstract suspend fun lastPosition(listType: String): Int

    /** 보기별 목록의 첫 페이지: 목록을 교체하고 이어 받기 상태와 `favoriteCount`를 저장한다. 위치는 0부터 */
    @Transaction
    open suspend fun replaceList(
        listType: String,
        items: List<MemoEntity>,
        nextCursor: String?,
        totalCount: Int,
        favoriteCount: Int,
    ) {
        deleteList(listType)
        insertMemos(items.mapIndexed { i, e -> e.copy(listType = listType, position = i) })
        upsertState(MemoPageStateEntity(listType, nextCursor, totalCount, null))
        upsertState(MemoPageStateEntity(MEMO_STATE_COUNTS, null, null, favoriteCount))
    }

    /** 보기별 목록의 다음 페이지: 마지막 위치 뒤에 이어 붙이고 상태를 갱신한다 */
    @Transaction
    open suspend fun appendList(
        listType: String,
        items: List<MemoEntity>,
        nextCursor: String?,
        totalCount: Int,
        favoriteCount: Int,
    ) {
        val start = lastPosition(listType) + 1
        insertMemos(items.mapIndexed { i, e -> e.copy(listType = listType, position = start + i) })
        upsertState(MemoPageStateEntity(listType, nextCursor, totalCount, null))
        upsertState(MemoPageStateEntity(MEMO_STATE_COUNTS, null, null, favoriteCount))
    }

    /** 고정 목록: 응답 위치대로 교체한다 */
    @Transaction
    open suspend fun replacePinned(items: List<MemoEntity>) {
        deleteList(MEMO_LIST_PINNED)
        insertMemos(items.mapIndexed { i, e -> e.copy(listType = MEMO_LIST_PINNED, position = i) })
        // 받은 적이 있다는 표시. 빈 목록도 "고정 없음"으로 확정된다
        upsertState(MemoPageStateEntity(MEMO_STATE_PINNED, null, null, null))
    }

    // --- 날짜 메모 범위 ---

    @Query(
        """SELECT * FROM memo
            WHERE list_type = 'DATED' AND memo_date >= :from AND memo_date <= :to
            ORDER BY memo_date, position""",
    )
    abstract fun observeDated(from: String, to: String): Flow<List<MemoEntity>>

    @Query("DELETE FROM memo WHERE list_type = 'DATED' AND memo_date >= :from AND memo_date <= :to")
    abstract suspend fun deleteDated(from: String, to: String)

    /** 서버 응답으로 범위를 바꾼다: 그 범위의 날짜 메모를 지우고 응답 위치대로 새로 넣는다 (한 트랜잭션) */
    @Transaction
    open suspend fun replaceDated(from: String, to: String, entities: List<MemoEntity>) {
        deleteDated(from, to)
        insertMemos(entities.map { it.copy(listType = MEMO_LIST_DATED) })
    }

    // --- 상세 ---

    /** 그 메모를 담은 행 중 가장 나중에 저장한 것. 목록에서 받았든 상세로 받았든 최신 값이다 */
    @Query("SELECT * FROM memo WHERE memo_id = :memoId ORDER BY id DESC LIMIT 1")
    abstract fun observeMemo(memoId: String): Flow<MemoEntity?>

    @Query("DELETE FROM memo WHERE list_type = 'DETAIL' AND memo_id = :memoId")
    abstract suspend fun deleteDetail(memoId: String)

    @Query("SELECT COUNT(*) FROM memo WHERE list_type = 'DETAIL' AND memo_id = :memoId")
    abstract suspend fun detailCount(memoId: String): Int

    @Transaction
    open suspend fun replaceDetail(entity: MemoEntity) {
        deleteDetail(entity.memoId)
        insertMemos(listOf(entity.copy(listType = MEMO_LIST_DETAIL, position = 0)))
    }

    // --- 이어 받기 상태 ---

    @Query("SELECT * FROM memo_page_state WHERE `key` = :key")
    abstract fun observeState(key: String): Flow<MemoPageStateEntity?>

    @Query("SELECT * FROM memo_page_state WHERE `key` = :key")
    abstract suspend fun getState(key: String): MemoPageStateEntity?

    /** 받은 적이 있는 보기들의 키 (쓰기 뒤 첫 페이지를 다시 받을 보기) */
    @Query("SELECT `key` FROM memo_page_state WHERE `key` IN ('NEWEST', 'OLDEST', 'FAVORITES')")
    abstract suspend fun loadedViewKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertState(state: MemoPageStateEntity)

    // --- 공통 ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMemos(entities: List<MemoEntity>)

    @Query("SELECT COUNT(*) FROM memo WHERE list_type = :listType AND memo_id = :memoId")
    abstract suspend fun countIn(listType: String, memoId: String): Int

    @Query("DELETE FROM memo WHERE memo_id = :memoId")
    abstract suspend fun deleteRows(memoId: String)

    /**
     * 그 메모를 기기에서 지운다 (404 또는 삭제 성공, D-121). 보기별 목록에 있었으면 그 보기의 `totalCount`를 하나 줄이고,
     * 즐겨찾기 보기에 있었으면 `favoriteCount`도 줄인다. 정확한 값은 곧 이어지는 다시 불러오기가 맞춘다.
     */
    @Transaction
    open suspend fun deleteMemo(memoId: String) {
        for (listType in MEMO_VIEW_LISTS) {
            if (countIn(listType, memoId) == 0) continue
            val state = getState(listType)
            val total = state?.totalCount
            if (state != null && total != null && total > 0) upsertState(state.copy(totalCount = total - 1))
            if (listType == MEMO_LIST_FAVORITES) {
                val counts = getState(MEMO_STATE_COUNTS)
                val fav = counts?.favoriteCount
                if (counts != null && fav != null && fav > 0) upsertState(counts.copy(favoriteCount = fav - 1))
            }
        }
        deleteRows(memoId)
    }

    @Query("DELETE FROM memo")
    abstract suspend fun clearMemos()

    @Query("DELETE FROM memo_page_state")
    abstract suspend fun clearStates()

    /** 공간이 바뀔 때 모두 비운다 */
    @Transaction
    open suspend fun clear() {
        clearMemos()
        clearStates()
    }
}
