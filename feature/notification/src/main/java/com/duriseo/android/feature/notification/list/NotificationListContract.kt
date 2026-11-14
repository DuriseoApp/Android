package com.duriseo.android.feature.notification.list

import androidx.annotation.StringRes
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.domain.notification.NotificationDestination

/**
 * 항목의 종류 표시 (종류 아이콘·라벨). 서버 종류 `PARTNER_CREATED`는 대상이 메모면 "상대 메모 추가", 그 밖에는 "상대 일정 추가"다
 * (보드 `Notifications.dc.html`의 TYPE 표).
 */
enum class NotificationRowType {
    EVENT_REMINDER,
    PARTNER_EVENT,
    PARTNER_MEMO,
    MEMO_PINNED,
    ANNIVERSARY,
    COUPLE_LINKED,
    COUPLE_UNLINKED,
}

/**
 * 알림 한 줄 (SCR-NOTI-01). 제목·본문은 서버가 만든 문구 그대로다 (D-46).
 *
 * @property writtenAt 받은 시각 표기 계산 결과 (D-129). 문구는 화면이 붙인다
 * @property unread 안 읽음 점과 강조 배경이 있다
 */
data class NotificationItemUi(
    val id: String,
    val type: NotificationRowType,
    val title: String,
    val body: String,
    val writtenAt: WrittenAt,
    val unread: Boolean,
)

/**
 * SCR-NOTI-01 상태.
 *
 * @property loaded 첫 페이지를 한 번이라도 받아 저장했다. false인 빈 목록은 "알림 없음"이 아니라 "아직 못 받음"이라 빈 화면 문구를 보이지 않는다
 */
data class NotificationListUiState(
    val loaded: Boolean = false,
    val items: List<NotificationItemUi> = emptyList(),
    val hasMore: Boolean = false,
) {
    /** 받아 본 적이 있는데 30일 안의 알림이 없다 */
    val isEmpty: Boolean get() = loaded && items.isEmpty()
}

sealed interface NotificationListAction {
    /** 화면이 보이기 시작했다 (들어올 때, 하위 화면에서 돌아올 때): 목록을 열린 것으로 알리고 첫 페이지를 다시 불러온다 */
    data object Shown : NotificationListAction

    /** 화면이 가려졌다 (다른 화면이 위에 쌓임, 닫힘) */
    data object Hidden : NotificationListAction

    data object BackClicked : NotificationListAction

    /** 스크롤이 끝 가까이에 왔다 */
    data object EndReached : NotificationListAction

    data class ItemClicked(val id: String) : NotificationListAction
}

sealed interface NotificationListSideEffect {
    data object Back : NotificationListSideEffect

    /** 대상 화면으로 간다. 목록 위에 쌓는다 (홈을 뒤에 두는 푸시 진입과 다르다, FR-NOTI-03 AC3) */
    data class Navigate(val destination: NotificationDestination) : NotificationListSideEffect

    /** 대상이 삭제됐다: 읽음 처리만 하고 목록에 머물며 스낵바 (FR-NOTI-05 AC5) */
    data class ShowMessage(@StringRes val messageRes: Int) : NotificationListSideEffect
}
