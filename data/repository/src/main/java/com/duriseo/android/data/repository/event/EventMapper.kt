package com.duriseo.android.data.repository.event

import com.duriseo.android.data.local.event.EventEntity
import com.duriseo.android.domain.event.Event
import com.duriseo.android.domain.event.EventReminder
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.event.ReminderOption
import java.time.Instant
import java.time.LocalDate

/** 도메인 일정 -> Room 행. 종일은 날짜 두 칸, 시간 일정은 밀리초 두 칸만 채운다 */
internal fun Event.toEntity(): EventEntity {
    val allDay = time as? EventTime.AllDay
    val timed = time as? EventTime.Timed
    return EventEntity(
        id = id,
        title = title,
        note = note,
        isAllDay = time.isAllDay,
        startDate = allDay?.start?.toString(),
        endDate = allDay?.end?.toString(),
        startAt = timed?.start?.toEpochMilli(),
        endAt = timed?.end?.toEpochMilli(),
        isTogether = isTogether,
        authorId = authorId,
        updatedBy = updatedBy,
        reminderOption = reminder?.option?.name,
        reminderFireAt = reminder?.fireAt?.toEpochMilli(),
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )
}

/** Room 행 -> 도메인 일정. 일시 칸이 비어 있는 깨진 행이면 null (화면에 내보내지 않는다) */
internal fun EventEntity.toDomainOrNull(): Event? {
    val time: EventTime = if (isAllDay) {
        EventTime.AllDay(LocalDate.parse(startDate ?: return null), LocalDate.parse(endDate ?: return null))
    } else {
        EventTime.Timed(Instant.ofEpochMilli(startAt ?: return null), Instant.ofEpochMilli(endAt ?: return null))
    }
    val reminder = reminderOption?.let { code ->
        val option = ReminderOption.fromCode(code)
        val fireAt = reminderFireAt
        if (option != null && fireAt != null) EventReminder(option, Instant.ofEpochMilli(fireAt)) else null
    }
    return Event(
        id = id,
        title = title,
        note = note,
        time = time,
        isTogether = isTogether,
        authorId = authorId,
        updatedBy = updatedBy,
        reminder = reminder,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
}
