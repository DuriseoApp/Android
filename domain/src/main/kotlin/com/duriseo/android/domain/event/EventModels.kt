package com.duriseo.android.domain.event

import java.time.Instant
import java.time.LocalDate

/**
 * 일정 알림 선택지 (API.md 3.3 "일정 알림 `option`", ERD `ck_event_reminders_option`). "없음"은 값이 아니라 알림 행이 없는 것이다.
 * 시간 일정은 [AT_TIME]·[MIN_10]·[HOUR_1]·[DAY_1], 종일 일정은 [ALLDAY_9AM]·[ALLDAY_PREV_9AM] (FR-CAL-05 AC1·AC3).
 */
enum class ReminderOption(val forAllDay: Boolean) {
    AT_TIME(false),
    MIN_10(false),
    HOUR_1(false),
    DAY_1(false),
    ALLDAY_9AM(true),
    ALLDAY_PREV_9AM(true),
    ;

    companion object {
        /** 화면에 보이는 순서대로 */
        val timed: List<ReminderOption> = listOf(AT_TIME, MIN_10, HOUR_1, DAY_1)
        val allDay: List<ReminderOption> = listOf(ALLDAY_9AM, ALLDAY_PREV_9AM)

        fun optionsFor(isAllDay: Boolean): List<ReminderOption> = if (isAllDay) allDay else timed

        /** 서버 문자열 -> 값. 모르는 값이면 null (알림 없음처럼 그린다) */
        fun fromCode(code: String): ReminderOption? = entries.firstOrNull { it.name == code }
    }
}

/** 요청자 자신의 일정 알림 (API.md `EventReminder`). [fireAt]은 이미 지난 시각일 수 있다 (D-90) */
data class EventReminder(
    val option: ReminderOption,
    val fireAt: Instant,
)

/** 일정 일시. 종일이면 날짜만, 아니면 UTC 시각 (NFR-TIME-01). 종료는 시작과 같아도 된다 */
sealed interface EventTime {
    val isAllDay: Boolean

    data class AllDay(val start: LocalDate, val end: LocalDate) : EventTime {
        override val isAllDay: Boolean get() = true
    }

    data class Timed(val start: Instant, val end: Instant) : EventTime {
        override val isAllDay: Boolean get() = false
    }
}

/** 일정 (API.md 3.3 `Event`). 닉네임은 응답에 없다. [authorId]로 `Me`에서 찾는다 (FR-CAL-03 AC3) */
data class Event(
    val id: String,
    val title: String,
    val note: String?,
    val time: EventTime,
    val isTogether: Boolean,
    val authorId: String,
    val updatedBy: String?,
    val reminder: EventReminder?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isAllDay: Boolean get() = time.isAllDay
}

/** `POST /events` 입력. [isTogether]·[notifyPartner]가 null이면 보내지 않는다(서버 기본: 나, 알림 켬) */
data class NewEvent(
    val title: String,
    val note: String?,
    val time: EventTime,
    val isTogether: Boolean? = null,
    val notifyPartner: Boolean? = null,
)

/**
 * `PATCH /events/{id}` 입력: 바뀐 필드만 담는다. [note]는 [NoteChange]로 "안 바꿈 / 지움 / 새 값"을 구분한다.
 * [time]이 바뀌면 시작·종료를 항상 함께 보낸다 (API.md "종일·시간 짝"). 상대가 수정할 때는 [isTogether]를 null로 둔다 (R-13)
 */
data class EventPatch(
    val title: String? = null,
    val note: NoteChange = NoteChange.Keep,
    val time: EventTime? = null,
    val isTogether: Boolean? = null,
) {
    val isEmpty: Boolean get() = title == null && note == NoteChange.Keep && time == null && isTogether == null
}

sealed interface NoteChange {
    data object Keep : NoteChange
    data object Clear : NoteChange
    data class Set(val text: String) : NoteChange
}
