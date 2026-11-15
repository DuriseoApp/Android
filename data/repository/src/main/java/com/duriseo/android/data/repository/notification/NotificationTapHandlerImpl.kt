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
                if (exists { events.refreshEvent(destination.eventId) }) destination else NotificationDestination.EventDeleted

            is NotificationDestination.MemoDetail ->
                if (exists { memos.refreshMemo(destination.memoId) }) destination else NotificationDestination.MemoDeleted

            else -> destination
        }
    }

    /**
     * 온라인이면 Room에 있어도 단건을 읽어 확인한다: 상대가 지운 일정·메모가 기기에 남아 있을 수 있어서다.
     * 404 `NOT_FOUND`면 삭제된 대상이고, 단건 읽기가 Room에서도 지운다 (기기에서 사라짐, R-01).
     * 오프라인이거나 그 밖의 오류면 확인 없이 이동한다: 대상 화면이 Room만 보고 자기 오류를 처리한다 (FR-NOTI-05 AC6).
     */
    private suspend fun exists(refresh: suspend () -> DataResult<Unit>): Boolean {
        if (!networkMonitor.isOnlineNow()) return true
        return when (val result = refresh()) {
            is DataResult.Success -> true
            is DataResult.Failure -> !result.error.hasCode(ErrorCode.NOT_FOUND)
        }
    }
}
