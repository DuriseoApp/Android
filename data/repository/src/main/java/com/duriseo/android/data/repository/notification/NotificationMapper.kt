package com.duriseo.android.data.repository.notification

import com.duriseo.android.data.local.notification.NotificationEntity
import com.duriseo.android.domain.notification.AppNotification
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import java.time.Instant

/** 위치는 저장할 때 Dao가 정한다 ([position]은 자리표시) */
internal fun AppNotification.toEntity(position: Int = 0) = NotificationEntity(
    notificationId = id,
    position = position,
    type = kind.name,
    title = title,
    body = body,
    targetType = target?.type?.name,
    targetId = target?.id,
    isRead = read,
    createdAt = createdAt.toEpochMilli(),
)

/** 읽을 수 없는 행(모르는 종류)은 null이다. 목록은 그 행을 건너뛴다 */
internal fun NotificationEntity.toDomainOrNull(): AppNotification? {
    val kind = NotificationKind.fromCode(type) ?: return null
    val created = Instant.ofEpochMilli(createdAt)
    val targetKind = NotificationTargetType.fromCode(targetType)
    return AppNotification(
        id = notificationId,
        kind = kind,
        title = title,
        body = body,
        target = targetKind?.let { NotificationTarget(it, targetId) },
        read = isRead,
        createdAt = created,
    )
}
