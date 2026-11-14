package com.duriseo.android.feature.notification

import com.duriseo.android.domain.notification.AppNotification
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationList
import com.duriseo.android.domain.notification.NotificationRepository
import com.duriseo.android.domain.notification.NotificationTapHandler
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

fun notification(
    id: String,
    kind: NotificationKind = NotificationKind.EVENT_REMINDER,
    targetType: NotificationTargetType? = NotificationTargetType.EVENT,
    targetId: String? = "e-$id",
    read: Boolean = false,
    createdAt: Instant = Instant.parse("2026-10-12T01:00:00Z"),
) = AppNotification(
    id = id,
    kind = kind,
    title = "제목 $id",
    body = "본문 $id",
    target = targetType?.let { NotificationTarget(it, targetId) },
    read = read,
    createdAt = createdAt,
)

class FakeNotificationRepository(items: List<AppNotification> = emptyList(), loaded: Boolean = true) : NotificationRepository {
    val list = MutableStateFlow(NotificationList(items = items, hasMore = false, isLoaded = loaded))
    var refreshFirstPageCalls = 0
    var loadMoreCalls = 0
    var refreshUnreadCountCalls = 0
    val markedRead = mutableListOf<String>()
    val unread = MutableStateFlow(0)

    override fun observeNotifications(): Flow<NotificationList> = list
    override fun observeUnreadCount(): Flow<Int> = unread.map { it }

    override suspend fun refreshFirstPage(): DataResult<Unit> {
        refreshFirstPageCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun loadMore(): DataResult<Unit> {
        loadMoreCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun refreshUnreadCount(): DataResult<Unit> {
        refreshUnreadCountCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun markRead(id: String) {
        markedRead += id
    }

    private var listOpen = false

    override val isListOpen: Boolean get() = listOpen

    override fun setListOpen(open: Boolean) {
        listOpen = open
    }
}

/** 누른 알림 처리 가짜: 정해 둔 갈 곳을 돌려주고 읽음 요청을 기록한다 */
class FakeNotificationTapHandler(var destination: NotificationDestination = NotificationDestination.None) : NotificationTapHandler {
    val opened = mutableListOf<String>()

    override suspend fun open(notificationId: String, kind: NotificationKind, target: NotificationTarget?): NotificationDestination {
        opened += notificationId
        return destination
    }
}
