package com.duriseo.android.data.remote.event

import com.duriseo.android.domain.event.Event
import com.duriseo.android.domain.event.EventPatch
import com.duriseo.android.domain.event.EventReminder
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.event.NewEvent
import com.duriseo.android.domain.event.NoteChange
import com.duriseo.android.domain.event.ReminderOption
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// API.md 3.3 `Event`, `EventReminder`와 요청 본문.
// `start`·`end`는 종일이면 `YYYY-MM-DD`, 아니면 UTC 일시라 문자열로 받고 `isAllDay`를 보고 해석한다.

@Serializable
data class EventReminderDto(
    val option: String,
    @Contextual val fireAt: Instant,
)

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val note: String? = null,
    val isAllDay: Boolean,
    val start: String,
    val end: String,
    val isTogether: Boolean,
    val authorId: String,
    val updatedBy: String? = null,
    val reminder: EventReminderDto? = null,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
)

/** `POST /events` 요청. null 필드는 `explicitNulls = false`라 보내지 않는다 (서버 기본값을 쓴다) */
@Serializable
data class CreateEventRequest(
    val title: String,
    val note: String? = null,
    val isAllDay: Boolean,
    val start: String,
    val end: String,
    val isTogether: Boolean? = null,
    val notifyPartner: Boolean? = null,
)

/** `PUT /events/{id}/reminder` 요청. `option`은 6개 중 하나 (API.md "일정 알림 `option`") */
@Serializable
data class ReminderRequest(val option: String)

/**
 * 일시는 `YYYY-MM-DDTHH:MM:SSZ`만 받는다 (밀리초·오프셋이면 400). 초 단위로 자르고 UTC `Z`로 쓴다.
 * 정각 초는 `Instant.toString()`이 `…:00Z`로 쓴다 (초를 생략하지 않는다).
 */
internal fun Instant.toWire(): String = truncatedTo(ChronoUnit.SECONDS).toString()

internal fun EventTime.startWire(): String = when (this) {
    is EventTime.AllDay -> start.toString()
    is EventTime.Timed -> start.toWire()
}

internal fun EventTime.endWire(): String = when (this) {
    is EventTime.AllDay -> end.toString()
    is EventTime.Timed -> end.toWire()
}

internal fun NewEvent.toRequest() = CreateEventRequest(
    title = title,
    note = note,
    isAllDay = time.isAllDay,
    start = time.startWire(),
    end = time.endWire(),
    isTogether = isTogether,
    notifyPartner = notifyPartner,
)

/** 바뀐 필드만 담은 본문. `note`를 지울 때만 `null`을 명시한다. 일시가 바뀌면 `isAllDay`·`start`·`end`를 함께 보낸다 */
internal fun EventPatch.toRequestBody(): JsonObject = buildJsonObject {
    title?.let { put("title", JsonPrimitive(it)) }
    when (val n = note) {
        NoteChange.Keep -> Unit
        NoteChange.Clear -> put("note", JsonNull)
        is NoteChange.Set -> put("note", JsonPrimitive(n.text))
    }
    time?.let {
        put("isAllDay", JsonPrimitive(it.isAllDay))
        put("start", JsonPrimitive(it.startWire()))
        put("end", JsonPrimitive(it.endWire()))
    }
    isTogether?.let { put("isTogether", JsonPrimitive(it)) }
}

/** 모양이 틀린 일시·날짜면 `IllegalArgumentException`(→ 데이터 소스가 공통 오류로 바꾼다) */
internal fun EventDto.toDomain(): Event = Event(
    id = id,
    title = title,
    note = note,
    time = if (isAllDay) {
        EventTime.AllDay(LocalDate.parse(start), LocalDate.parse(end))
    } else {
        EventTime.Timed(Instant.parse(start), Instant.parse(end))
    },
    isTogether = isTogether,
    authorId = authorId,
    updatedBy = updatedBy,
    // 모르는 option은 알림 없음으로 그린다
    reminder = reminder?.let { r -> ReminderOption.fromCode(r.option)?.let { EventReminder(it, r.fireAt) } },
    createdAt = createdAt,
    updatedAt = updatedAt,
)
