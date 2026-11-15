package com.duriseo.android.data.local.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** 알림 목록 캐시. 읽기 순서는 응답 위치([NotificationEntity.position]) 그대로다 */
@Dao
abstract class NotificationDao {
    @Query("SELECT * FROM notification ORDER BY position")
    abstract fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notification_state WHERE `key` = :key")
    abstract fun observeState(key: String): Flow<NotificationStateEntity?>

    @Query("SELECT * FROM notification_state WHERE `key` = :key")
    abstract suspend fun getState(key: String): NotificationStateEntity?

    @Query("DELETE FROM notification")
    abstract suspend fun deleteAll()

    /** 만든 지 30일이 지난 알림을 지운다 ([cutoffMillis] 이하, D-141). 서버도 같은 기준으로 거르고 정리 작업이 지운다 */
    @Query("DELETE FROM notification WHERE created_at <= :cutoffMillis")
    abstract suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM notification")
    abstract suspend fun lastPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<NotificationEntity>)

    /** 다음 페이지용: 이미 있는 id는 건드리지 않는다 (새 알림이 생겨 페이지 경계가 밀려도 앞 위치와 읽음 값을 지킨다) */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnoringExisting(entities: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertState(state: NotificationStateEntity)

    /** 첫 페이지: 목록을 교체하고 이어 받기 상태를 저장한다. 위치는 0부터 */
    @Transaction
    open suspend fun replaceFirstPage(items: List<NotificationEntity>, nextCursor: String?) {
        deleteAll()
        insertAll(items.mapIndexed { i, e -> e.copy(position = i) })
        upsertState(NotificationStateEntity(NOTIFICATION_STATE_LIST, nextCursor, null))
    }

    /** 다음 페이지: 마지막 위치 뒤에 이어 붙이고 이어 받기 상태를 갱신한다 */
    @Transaction
    open suspend fun appendPage(items: List<NotificationEntity>, nextCursor: String?) {
        val start = lastPosition() + 1
        insertIgnoringExisting(items.mapIndexed { i, e -> e.copy(position = start + i) })
        upsertState(NotificationStateEntity(NOTIFICATION_STATE_LIST, nextCursor, null))
    }

    @Query("UPDATE notification SET is_read = 1 WHERE notification_id = :id AND is_read = 0")
    abstract suspend fun markRowRead(id: String): Int

    /**
     * 그 알림을 읽음으로 바꾼다. 안 읽음이던 행이 바뀌었으면 안 읽은 개수를 하나 줄이고(0 아래로 내려가지 않음) true를 돌려준다.
     * 정확한 개수는 곧 이어지는 `unread-count` 다시 읽기가 맞춘다.
     */
    @Transaction
    open suspend fun markRead(id: String): Boolean {
        if (markRowRead(id) == 0) return false
        val state = getState(NOTIFICATION_STATE_UNREAD)
        val count = state?.unreadCount
        if (state != null && count != null && count > 0) upsertState(state.copy(unreadCount = count - 1))
        return true
    }

    @Transaction
    open suspend fun setUnreadCount(count: Int) {
        upsertState(NotificationStateEntity(NOTIFICATION_STATE_UNREAD, null, count))
    }

    @Query("DELETE FROM notification_state")
    abstract suspend fun clearStates()

    @Transaction
    open suspend fun clear() {
        deleteAll()
        clearStates()
    }
}
