package com.duriseo.android.feature.notification.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.format.DateTexts
import com.duriseo.android.domain.notification.AppNotification
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationRepository
import com.duriseo.android.domain.notification.NotificationTapHandler
import com.duriseo.android.domain.notification.NotificationTargetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * SCR-NOTI-01 알림 목록 (FR-NOTI-05). 화면은 Room만 구독한다 (NFR-OFF-01): 보일 때마다 첫 페이지를 다시 받고(실패·오프라인이면
 * 저장된 목록 그대로, 조용히 넘김), 스크롤 끝에서 다음 페이지를 이어 붙인다. 목록이 열려 있는 동안 새 알림 신호가 오면
 * 데이터 쪽이 첫 페이지를 다시 부르도록 [NotificationRepository.setListOpen]으로 알린다.
 *
 * 항목을 누르면 [NotificationTapHandler]가 읽음 처리(기기 먼저, 덮개 없음 D-127)와 갈 곳 확인을 한다.
 */
@HiltViewModel
class NotificationListViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val tapHandler: NotificationTapHandler,
    private val clock: Clock,
) : ViewModel() {

    @Volatile
    private var items: Map<String, AppNotification> = emptyMap()

    val uiState: StateFlow<NotificationListUiState> = notificationRepository.observeNotifications()
        .map { list ->
            items = list.items.associateBy { it.id }
            NotificationListUiState(
                loaded = list.isLoaded,
                items = list.items.map { it.toUi() },
                hasMore = list.hasMore,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NotificationListUiState())

    private val effectChannel = Channel<NotificationListSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var loadingMore = false
    private var tapping = false

    fun onAction(action: NotificationListAction) {
        when (action) {
            NotificationListAction.Shown -> {
                notificationRepository.setListOpen(true)
                // 실패·오프라인은 저장된 목록을 그대로 보인다 (안내 없음)
                viewModelScope.launch { notificationRepository.refreshFirstPage() }
            }

            NotificationListAction.Hidden -> notificationRepository.setListOpen(false)
            NotificationListAction.BackClicked -> effectChannel.trySend(NotificationListSideEffect.Back)
            NotificationListAction.EndReached -> loadMore()
            is NotificationListAction.ItemClicked -> onItemClicked(action.id)
        }
    }

    private fun loadMore() {
        if (loadingMore || !uiState.value.hasMore) return
        loadingMore = true
        viewModelScope.launch {
            try {
                notificationRepository.loadMore()
            } finally {
                loadingMore = false
            }
        }
    }

    /**
     * 읽음 처리 후 바로 이동한다 (덮는 화면 없음, D-127). 대상이 삭제됐으면 읽음만 하고 목록에 머물며 안내한다 (FR-NOTI-05 AC5).
     * 같은 항목을 연달아 눌러 화면이 두 번 열리지 않게 처리 중에는 다른 누름을 무시한다.
     */
    private fun onItemClicked(id: String) {
        val notification = items[id] ?: return
        if (tapping) return
        tapping = true
        viewModelScope.launch {
            try {
                val destination = tapHandler.open(notification.id, notification.kind, notification.target)
                when (destination) {
                    NotificationDestination.EventDeleted ->
                        effectChannel.trySend(NotificationListSideEffect.ShowMessage(DesignR.string.notification_event_deleted))

                    NotificationDestination.MemoDeleted ->
                        effectChannel.trySend(NotificationListSideEffect.ShowMessage(DesignR.string.notification_memo_deleted))

                    NotificationDestination.None -> Unit
                    else -> effectChannel.trySend(NotificationListSideEffect.Navigate(destination))
                }
            } finally {
                tapping = false
            }
        }
    }

    override fun onCleared() {
        notificationRepository.setListOpen(false)
    }

    private fun AppNotification.toUi() = NotificationItemUi(
        id = id,
        type = rowTypeOf(kind, target?.type),
        title = title,
        body = body,
        writtenAt = DateTexts.writtenAt(createdAt, clock),
        unread = !read,
    )
}

/** 서버 종류와 대상 종류로 아이콘·라벨을 정한다 */
internal fun rowTypeOf(kind: NotificationKind, targetType: NotificationTargetType?): NotificationRowType = when (kind) {
    NotificationKind.EVENT_REMINDER -> NotificationRowType.EVENT_REMINDER
    NotificationKind.PARTNER_CREATED ->
        if (targetType == NotificationTargetType.MEMO) NotificationRowType.PARTNER_MEMO else NotificationRowType.PARTNER_EVENT

    NotificationKind.MEMO_PINNED -> NotificationRowType.MEMO_PINNED
    NotificationKind.ANNIVERSARY -> NotificationRowType.ANNIVERSARY
    NotificationKind.COUPLE_LINKED -> NotificationRowType.COUPLE_LINKED
    NotificationKind.COUPLE_UNLINKED -> NotificationRowType.COUPLE_UNLINKED
}
