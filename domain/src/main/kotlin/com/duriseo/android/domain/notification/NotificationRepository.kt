package com.duriseo.android.domain.notification

import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

/**
 * 알림 목록 (API.md 3.5, FR-NOTI-05). 화면은 Room의 [observeNotifications]·[observeUnreadCount]만 구독한다 (NFR-OFF-01).
 *
 * - `refresh…`·[loadMore]는 읽기라서 오프라인이면 요청 없이 `DataError.Offline`이다. 부른 쪽이 조용히 넘긴다.
 * - [markRead]는 기기에서 먼저 읽음으로 바꾸고 서버 요청은 뒤에서 한다. 실패·오프라인은 안내하지 않는다 (D-127).
 * - 알림은 사용자 것이라 로그아웃·계정 전환·세션 끊김 때 지워진다 (`LocalDataCleaner`). 공간이 바뀔 때는 지우지 않고
 *   서버가 해제 알림을 정리하므로(D-45) 다시 불러온다.
 */
interface NotificationRepository {
    /** 최근 30일 알림, 서버 순서(최신순) 그대로 (D-141) */
    fun observeNotifications(): Flow<NotificationList>

    /** 안 읽은 알림 개수. 받은 적이 없으면 0. 캘린더 앱바 알림 버튼의 점은 0보다 클 때다 (FR-CAL-01 AC4) */
    fun observeUnreadCount(): Flow<Int>

    /** `GET /notifications` 첫 페이지: 목록을 교체한다 (알림 목록에 들어갈 때, 당겨서 새로고침, 동기화 신호) */
    suspend fun refreshFirstPage(): DataResult<Unit>

    /** `GET /notifications?cursor=` 다음 페이지를 이어 붙인다. 다음 페이지가 없으면 요청 없이 성공 */
    suspend fun loadMore(): DataResult<Unit>

    /** `GET /notifications/unread-count` -> 안 읽은 개수를 저장한다 (캘린더 진입·당겨서 새로고침·동기화 신호) */
    suspend fun refreshUnreadCount(): DataResult<Unit>

    /**
     * 그 알림을 읽음으로 한다 (D-127): 기기의 Room을 먼저 바꾸고(안 읽은 개수도 하나 줄임) 바로 돌아온다.
     * `PUT /notifications/{id}/read`는 뒤에서 보내고, 실패(404 포함)·오프라인은 안내 없이 넘긴다.
     * 이미 읽음이거나 목록에 없는 알림이어도 부를 수 있다 (푸시를 눌러 들어왔을 때, D-142).
     * 서버 요청이 성공하면 안 읽은 개수를 서버 값으로 다시 맞춘다.
     */
    suspend fun markRead(id: String)

    /**
     * 알림 목록 화면이 열려 있는지 알린다. `NOTIFICATION` 동기화 신호가 오면 열려 있을 때만 첫 페이지도 다시 부른다 (API.md 6장).
     * 화면이 보이는 동안 true, 떠날 때 false다.
     */
    fun setListOpen(open: Boolean)

    /** [setListOpen]으로 알린 값. 동기화 신호 처리가 첫 페이지를 다시 부를지 정할 때 읽는다 */
    val isListOpen: Boolean
}
