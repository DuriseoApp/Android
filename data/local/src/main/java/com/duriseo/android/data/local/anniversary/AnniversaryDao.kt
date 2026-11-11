package com.duriseo.android.data.local.anniversary

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 기념일 캐시. 읽기 순서는 응답 위치([AnniversaryOccurrenceEntity.position]) 그대로다.
 * 날짜는 `YYYY-MM-DD`라 문자열 비교가 날짜 비교와 같다.
 */
@Dao
abstract class AnniversaryDao {
    // --- 캘린더 범위 ---

    @Query(
        """SELECT * FROM anniversary_occurrence
            WHERE list_type = 'RANGE' AND date >= :from AND date <= :to
            ORDER BY date, position""",
    )
    abstract fun observeRange(from: String, to: String): Flow<List<AnniversaryOccurrenceEntity>>

    @Query("DELETE FROM anniversary_occurrence WHERE list_type = 'RANGE' AND date >= :from AND date <= :to")
    abstract suspend fun deleteRange(from: String, to: String)

    /** 서버 응답으로 범위를 바꾼다: 그 범위의 발생일을 지우고 응답 위치대로 새로 넣는다 (한 트랜잭션) */
    @Transaction
    open suspend fun replaceRange(from: String, to: String, entities: List<AnniversaryOccurrenceEntity>) {
        deleteRange(from, to)
        insertOccurrences(entities)
    }

    // --- 다가오는 목록 ---

    @Query("SELECT * FROM anniversary_occurrence WHERE list_type = 'UPCOMING' ORDER BY position")
    abstract fun observeUpcoming(): Flow<List<AnniversaryOccurrenceEntity>>

    @Query("DELETE FROM anniversary_occurrence WHERE list_type = 'UPCOMING'")
    abstract suspend fun deleteUpcoming()

    @Query("SELECT COALESCE(MAX(position), -1) FROM anniversary_occurrence WHERE list_type = 'UPCOMING'")
    abstract suspend fun lastUpcomingPosition(): Int

    /** 첫 페이지: 목록을 교체한다. 위치는 0부터 */
    @Transaction
    open suspend fun replaceUpcoming(items: List<AnniversaryOccurrenceEntity>, nextCursor: String?) {
        deleteUpcoming()
        insertOccurrences(items.mapIndexed { i, e -> e.copy(listType = LIST_UPCOMING, position = i) })
        upsertState(AnniversaryPageStateEntity(STATE_UPCOMING, nextCursor, null))
    }

    /** 다음 페이지: 마지막 위치 뒤에 이어 붙인다 */
    @Transaction
    open suspend fun appendUpcoming(items: List<AnniversaryOccurrenceEntity>, nextCursor: String?) {
        val start = lastUpcomingPosition() + 1
        insertOccurrences(items.mapIndexed { i, e -> e.copy(listType = LIST_UPCOMING, position = start + i) })
        upsertState(AnniversaryPageStateEntity(STATE_UPCOMING, nextCursor, null))
    }

    // --- 지난 기념일 ---

    @Query("SELECT * FROM anniversary_past ORDER BY position")
    abstract fun observePast(): Flow<List<PastAnniversaryEntity>>

    @Query("DELETE FROM anniversary_past")
    abstract suspend fun deletePast()

    @Query("SELECT COALESCE(MAX(position), -1) FROM anniversary_past")
    abstract suspend fun lastPastPosition(): Int

    /** 첫 페이지: 목록과 `totalCount`를 교체한다 */
    @Transaction
    open suspend fun replacePast(items: List<PastAnniversaryEntity>, nextCursor: String?, totalCount: Int) {
        deletePast()
        insertPast(items.mapIndexed { i, e -> e.copy(position = i) })
        upsertState(AnniversaryPageStateEntity(STATE_PAST, nextCursor, totalCount))
    }

    @Transaction
    open suspend fun appendPast(items: List<PastAnniversaryEntity>, nextCursor: String?, totalCount: Int?) {
        val start = lastPastPosition() + 1
        insertPast(items.mapIndexed { i, e -> e.copy(position = start + i) })
        // 다음 페이지에 totalCount가 없으면 저장된 값을 그대로 둔다
        upsertState(AnniversaryPageStateEntity(STATE_PAST, nextCursor, totalCount ?: getState(STATE_PAST)?.totalCount))
    }

    // --- 이어 받기 상태 ---

    @Query("SELECT * FROM anniversary_page_state WHERE `key` = :key")
    abstract fun observeState(key: String): Flow<AnniversaryPageStateEntity?>

    @Query("SELECT * FROM anniversary_page_state WHERE `key` = :key")
    abstract suspend fun getState(key: String): AnniversaryPageStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertState(state: AnniversaryPageStateEntity)

    // --- 공통 ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOccurrences(entities: List<AnniversaryOccurrenceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPast(entities: List<PastAnniversaryEntity>)

    @Query("DELETE FROM anniversary_occurrence WHERE anniversary_id = :id")
    abstract suspend fun deleteOccurrencesOf(id: String)

    @Query("DELETE FROM anniversary_past WHERE anniversary_id = :id")
    abstract suspend fun deletePastOf(id: String)

    /** 서버가 없다고 답한 기념일을 기기에서 지운다 (404, D-121). 지난 기념일 개수도 하나 줄인다 */
    @Transaction
    open suspend fun deleteAnniversary(id: String) {
        val hadPast = pastCountOf(id) > 0
        deleteOccurrencesOf(id)
        deletePastOf(id)
        if (hadPast) {
            val state = getState(STATE_PAST)
            val total = state?.totalCount
            if (state != null && total != null && total > 0) upsertState(state.copy(totalCount = total - 1))
        }
    }

    @Query("SELECT COUNT(*) FROM anniversary_past WHERE anniversary_id = :id")
    abstract suspend fun pastCountOf(id: String): Int

    @Query("DELETE FROM anniversary_occurrence")
    abstract suspend fun clearOccurrences()

    @Query("DELETE FROM anniversary_page_state")
    abstract suspend fun clearStates()

    /** 공간이 바뀔 때 모두 비운다 */
    @Transaction
    open suspend fun clear() {
        clearOccurrences()
        deletePast()
        clearStates()
    }
}
