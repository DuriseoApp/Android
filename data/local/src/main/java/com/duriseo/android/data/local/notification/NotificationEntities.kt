package com.duriseo.android.data.local.notification

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** [NotificationStateEntity.key]: 알림 목록의 이어 받기 상태 (행이 있으면 첫 페이지를 받은 적이 있다) */
const val NOTIFICATION_STATE_LIST = "LIST"

/** [NotificationStateEntity.key]: 안 읽은 알림 개수 (행이 있으면 받은 적이 있다) */
const val NOTIFICATION_STATE_UNREAD = "UNREAD"

/**
 * 알림 목록 캐시 (API.md 3.5 `{id, type, title, body, targetType, targetId, read, createdAt}`).
 * [position]은 응답 배열의 위치다 (여러 페이지면 페이지를 이어 붙인 순서). 앱은 다시 정렬하지 않는다.
 * [createdAt]은 UTC epoch 밀리초다 (30일 경계 비교용, API.md의 일시는 초 단위). 사용자 것이라 공간이 바뀌어도 지우지 않고 로그아웃·계정 전환 때 `clearAllTables`가 지운다.
 * 스키마 버전 6에서 더했다.
 */
@Entity(tableName = "notification", indices = [Index("position")])
data class NotificationEntity(
    @PrimaryKey @ColumnInfo(name = "notification_id") val notificationId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "target_type") val targetType: String?,
    @ColumnInfo(name = "target_id") val targetId: String?,
    @ColumnInfo(name = "is_read") val isRead: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/**
 * 알림의 상태 행. [NOTIFICATION_STATE_LIST] 행은 [nextCursor](서버가 준 불투명 문자열 그대로, null이면 마지막 페이지)를,
 * [NOTIFICATION_STATE_UNREAD] 행은 [unreadCount]를 담는다. 스키마 버전 6.
 */
@Entity(tableName = "notification_state")
data class NotificationStateEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "next_cursor") val nextCursor: String?,
    @ColumnInfo(name = "unread_count") val unreadCount: Int?,
)
