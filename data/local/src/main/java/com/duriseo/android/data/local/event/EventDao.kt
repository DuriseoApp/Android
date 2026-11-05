package com.duriseo.android.data.local.event

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 범위에 걸친 일정 (서버 `GET /events`와 같은 규칙, API.md 3.3 "걸치는 일정"):
 * 종일은 `start_date <= to AND end_date >= from`, 시간 일정은 `start_at < toExclusive AND end_at >= from`.
 * 날짜는 `YYYY-MM-DD`라 문자열 비교가 날짜 비교와 같다. 시간 일정의 경계(밀리초)는 부르는 쪽이 사용자 시간대로 계산한다.
 */
@Dao
abstract class EventDao {
    @Query(
        """SELECT * FROM event WHERE
            (is_all_day = 1 AND start_date <= :toDate AND end_date >= :fromDate) OR
            (is_all_day = 0 AND start_at < :toExclusiveMillis AND end_at >= :fromMillis)
            ORDER BY is_all_day DESC, start_date, start_at, id""",
    )
    abstract fun observeRange(
        fromDate: String,
        toDate: String,
        fromMillis: Long,
        toExclusiveMillis: Long,
    ): Flow<List<EventEntity>>

    @Query("SELECT * FROM event WHERE id = :id")
    abstract fun observe(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM event WHERE id = :id")
    abstract suspend fun get(id: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(entities: List<EventEntity>)

    @Query("DELETE FROM event WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Query("DELETE FROM event")
    abstract suspend fun clear()

    @Query(
        """DELETE FROM event WHERE
            (is_all_day = 1 AND start_date <= :toDate AND end_date >= :fromDate) OR
            (is_all_day = 0 AND start_at < :toExclusiveMillis AND end_at >= :fromMillis)""",
    )
    abstract suspend fun deleteRange(fromDate: String, toDate: String, fromMillis: Long, toExclusiveMillis: Long)

    /** 서버 응답으로 범위를 바꾼다: 그 범위에 걸친 행을 지우고 새로 넣는다 (한 트랜잭션, ARCHITECTURE.md 2장) */
    @Transaction
    open suspend fun replaceRange(
        fromDate: String,
        toDate: String,
        fromMillis: Long,
        toExclusiveMillis: Long,
        entities: List<EventEntity>,
    ) {
        deleteRange(fromDate, toDate, fromMillis, toExclusiveMillis)
        upsertAll(entities)
    }
}
