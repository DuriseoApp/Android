package com.duriseo.android.data.remote.notification

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.domain.notification.AppNotification
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.result.map
import javax.inject.Inject
import javax.inject.Singleton

/** `GET /notifications`의 한 페이지. [nextCursor]는 불투명 문자열이고 앱은 해석하지 않는다 (API.md 1.1). null이면 마지막 페이지 */
data class NotificationPage(
    val items: List<AppNotification>,
    val nextCursor: String?,
)

/** 알림 API 호출을 [DataResult]와 도메인 모델로 돌려준다 (API.md 3.5). 응답 배열의 순서를 바꾸지 않는다 */
@Singleton
class NotificationRemoteDataSource @Inject constructor(
    private val api: NotificationApi,
    private val apiCaller: ApiCaller,
) {
    /** `GET /notifications?cursor=&size=`. 모르는 종류의 알림은 건너뛴다 */
    suspend fun list(cursor: String? = null, size: Int? = null): DataResult<NotificationPage> =
        apiCaller.call { api.getNotifications(cursor, size) }
            .map { r -> NotificationPage(r.items.mapNotNull { it.toDomainOrNull() }, r.nextCursor) }

    /** `GET /notifications/unread-count` -> `count` */
    suspend fun unreadCount(): DataResult<Int> = apiCaller.call { api.getUnreadCount() }.map { it.count }

    /** `PUT /notifications/{id}/read` (204, 멱등) */
    suspend fun markRead(id: String): DataResult<Unit> = apiCaller.callUnit { api.markRead(id) }
}
