package com.duriseo.android.data.local.notification

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 캐시(Room)를 `data:repository`에 보여 주는 얇은 창구. Room 타입은 이 모듈 밖으로 나가지 않는다.
 * `open`인 것은 저장소 테스트가 Room 쓰기 실패를 만들려고 상속하기 때문이다.
 */
@Singleton
open class NotificationLocalDataSource @Inject constructor(
    private val dao: NotificationDao,
) {
    open fun observeAll(): Flow<List<NotificationEntity>> = dao.observeAll()

    /** 이어 받기 상태. null이면 첫 페이지를 받은 적이 없다 */
    open fun observeListState(): Flow<NotificationStateEntity?> = dao.observeState(NOTIFICATION_STATE_LIST)

    /** 안 읽은 개수 상태. null이면 받은 적이 없다 */
    open fun observeUnreadState(): Flow<NotificationStateEntity?> = dao.observeState(NOTIFICATION_STATE_UNREAD)

    open suspend fun nextCursor(): String? = dao.getState(NOTIFICATION_STATE_LIST)?.nextCursor

    open suspend fun replaceFirstPage(items: List<NotificationEntity>, nextCursor: String?) =
        dao.replaceFirstPage(items, nextCursor)

    open suspend fun appendPage(items: List<NotificationEntity>, nextCursor: String?) =
        dao.appendPage(items, nextCursor)

    open suspend fun setUnreadCount(count: Int) = dao.setUnreadCount(count)

    /** 기기에서 읽음으로 바꾼다. 안 읽음이던 행이 있었으면 true */
    open suspend fun markRead(id: String): Boolean = dao.markRead(id)

    /** 만든 지 30일이 지난 알림을 지운다 ([cutoffMillis] 이하, D-141) */
    open suspend fun deleteOlderThan(cutoffMillis: Long) = dao.deleteOlderThan(cutoffMillis)

    open suspend fun clear() = dao.clear()
}
