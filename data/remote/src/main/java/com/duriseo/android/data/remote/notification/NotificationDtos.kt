package com.duriseo.android.data.remote.notification

import com.duriseo.android.domain.notification.AppNotification
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

// API.md 3.5 알림 목록.

/**
 * 알림 한 건 `{id, type, title, body, targetType, targetId, read, createdAt}`. `type`·`targetType`은 문자열로 받아
 * 모르는 값이 와도 파싱이 깨지지 않게 한다 (알 수 없는 종류는 [toDomainOrNull]이 건너뛴다).
 */
@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val targetType: String? = null,
    val targetId: String? = null,
    val read: Boolean,
    @Contextual val createdAt: Instant,
)

/** `GET /notifications` 응답 `{items, nextCursor}`. 마지막 페이지면 `nextCursor`는 null */
@Serializable
data class NotificationPageResponse(
    val items: List<NotificationDto>,
    val nextCursor: String? = null,
)

/** `GET /notifications/unread-count` 응답 `{count}` */
@Serializable
data class UnreadCountResponse(val count: Int)

/** 모르는 `type`이면 null이다. 모르는 `targetType`은 이동 대상 없음으로 본다 */
fun NotificationDto.toDomainOrNull(): AppNotification? {
    val kind = NotificationKind.fromCode(type) ?: return null
    val targetKind = NotificationTargetType.fromCode(targetType)
    return AppNotification(
        id = id,
        kind = kind,
        title = title,
        body = body,
        target = targetKind?.let { NotificationTarget(it, targetId) },
        read = read,
        createdAt = createdAt,
    )
}
