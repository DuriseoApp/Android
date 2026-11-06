package com.duriseo.android.data.local.event

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 일정 캐시 (API.md 3.3 `Event`). 화면은 이 테이블의 Flow만 구독한다 (ARCHITECTURE.md 2장).
 *
 * 종일 일정은 `start_date`·`end_date`(`YYYY-MM-DD`)만, 시간 일정은 `start_at`·`end_at`(epoch 밀리초)만 값이 있다.
 * 알림은 요청자 본인 것만 오므로 `reminder_option`(서버 문자열 그대로)·`reminder_fire_at`(epoch 밀리초) 두 칸이다.
 * 현재 활성 공간의 일정만 담는다. 공간이 바뀌면 비운다 (unit-4.md 확인 필요 12).
 * 스키마 버전 3에서 더했다.
 */
@Entity(
    tableName = "event",
    indices = [Index("start_date"), Index("start_at")],
)
data class EventEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "is_all_day") val isAllDay: Boolean,
    @ColumnInfo(name = "start_date") val startDate: String?,
    @ColumnInfo(name = "end_date") val endDate: String?,
    @ColumnInfo(name = "start_at") val startAt: Long?,
    @ColumnInfo(name = "end_at") val endAt: Long?,
    @ColumnInfo(name = "is_together") val isTogether: Boolean,
    @ColumnInfo(name = "author_id") val authorId: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String?,
    @ColumnInfo(name = "reminder_option") val reminderOption: String?,
    @ColumnInfo(name = "reminder_fire_at") val reminderFireAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
