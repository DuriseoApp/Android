package com.duriseo.android.data.repository.notification

import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationRepository
import com.duriseo.android.domain.notification.NotificationTapHandler
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.destinationOf
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** 알림 목록과 푸시 진입이 함께 쓰는 "누른 알림 처리" (FR-NOTI-03 AC2, FR-NOTI-05 AC5·AC6) */
@Singleton
class NotificationTapHandlerImpl @Inject constructor(
    private val notifications: NotificationRepository,
    private val events: EventRepository,
    private val memos: MemoRepository,
    private val networkMonitor: NetworkMonitor,
) : NotificationTapHandler {

    override suspend fun open(
        notificationId: String,
        kind: NotificationKind,
        target: NotificationTarget?,
    ): NotificationDestination {
        // 읽음은 기기에서 먼저, 서버 요청은 뒤에서 (실패·오프라인도 안내 없음, D-127)
        notifications.markRead(notificationId)
        return when (val destination = destinationOf(kind, target)) {
            is NotificationDestination.EventDetail ->
                if (exists(
                        inRoom = { events.observeEvent(destination.eventId).first() != null },
                        refresh = { events.refreshEvent(destination.eventId) },
                    )
                ) destination else NotificationDestination.EventDeleted

            is NotificationDestination.MemoDetail ->
                if (exists(
                        inRoom = { memos.observeMemo(destination.memoId).first() != null },
                        refresh = { memos.refreshMemo(destination.memoId) },
                    )
                ) destination else NotificationDestination.MemoDeleted

            else -> destination
        }
    }

    /**
     * 기기(Room)에 있으면 있는 것이다. 없으면 온라인일 때 단건을 읽어 보고 404 `NOT_FOUND`면 삭제된 대상이다.
     * 오프라인이거나 그 밖의 오류면 그냥 이동한다(대상 화면이 자기 오류를 처리한다).
     */
    private suspend fun exists(inRoom: suspend () -> Boolean, refresh: suspend () -> DataResult<Unit>): Boolean {
        if (inRoom()) return true
        if (!networkMonitor.isOnlineNow()) return true
        return when (val result = refresh()) {
            is DataResult.Success -> true
            is DataResult.Failure -> !result.error.hasCode(ErrorCode.NOT_FOUND)
        }
    }
}
