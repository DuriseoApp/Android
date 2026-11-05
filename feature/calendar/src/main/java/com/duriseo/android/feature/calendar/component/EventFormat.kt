package com.duriseo.android.feature.calendar.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.domain.event.EventTarget
import com.duriseo.android.domain.event.ReminderOption
import com.duriseo.android.feature.calendar.R
import java.time.LocalDate
import java.time.LocalTime

// 일정 화면이 함께 쓰는 서식·라벨·색. 문구는 보드 그대로다 (unit-4.md 확인 필요 13).

private const val DAYS_PER_WEEK = 7
private const val NOON = 12

/** 요일 한 글자 ("일"~"토", 디자인 시스템 문자열) */
@Composable
@ReadOnlyComposable
fun weekdayLabel(date: LocalDate): String =
    stringArrayResource(DesignR.array.weekday_short)[date.dayOfWeek.value % DAYS_PER_WEEK]

/** "10월 12일 (월)" (연도 없음, 앞자리 0 없음, 보드 `EventDetail.dc.html`) */
@Composable
@ReadOnlyComposable
fun formatEventDate(date: LocalDate): String =
    stringResource(R.string.event_date_format, date.monthValue, date.dayOfMonth, weekdayLabel(date))

/** "오후 7:30" (상세·작성의 시각) */
@Composable
@ReadOnlyComposable
fun formatTime12(time: LocalTime): String {
    val hour = if (time.hour % NOON == 0) NOON else time.hour % NOON
    return stringResource(if (time.hour < NOON) R.string.event_time_am else R.string.event_time_pm, hour, time.minute)
}

/** "19:30" (날짜 목록의 시각, 보드 `Calendar.dc.html`) */
@Composable
@ReadOnlyComposable
fun formatTime24(time: LocalTime): String = stringResource(R.string.event_time_24h, time.hour, time.minute)

@Composable
@ReadOnlyComposable
fun formatDateTime(date: LocalDate, time: LocalTime): String =
    stringResource(R.string.event_date_time_format, formatEventDate(date), formatTime12(time))

/** 대상 라벨: 보는 사람 기준 나·상대·함께 (FR-CAL-04 AC6) */
@Composable
@ReadOnlyComposable
fun EventTarget.label(): String = stringResource(
    when (this) {
        EventTarget.ME -> R.string.event_target_me
        EventTarget.PARTNER -> R.string.event_target_partner
        EventTarget.TOGETHER -> R.string.event_target_together
    },
)

/** 대상의 사람 색 (FR-CAL-02 AC4). 현재 `DuriseoTheme`의 `PersonColors`를 읽는다 */
@Composable
@ReadOnlyComposable
fun EventTarget.color(): Color = when (this) {
    EventTarget.ME -> DuriseoTheme.personColors.me
    EventTarget.PARTNER -> DuriseoTheme.personColors.partner
    EventTarget.TOGETHER -> DuriseoTheme.personColors.together
}

/** 알림 선택지 라벨. [option]이 null이면 "없음" (FR-CAL-05 AC1·AC3) */
@Composable
@ReadOnlyComposable
fun reminderLabel(option: ReminderOption?): String = stringResource(
    when (option) {
        null -> R.string.reminder_none
        ReminderOption.AT_TIME -> R.string.reminder_at_time
        ReminderOption.MIN_10 -> R.string.reminder_min_10
        ReminderOption.HOUR_1 -> R.string.reminder_hour_1
        ReminderOption.DAY_1 -> R.string.reminder_day_1
        ReminderOption.ALLDAY_9AM -> R.string.reminder_allday_9am
        ReminderOption.ALLDAY_PREV_9AM -> R.string.reminder_allday_prev_9am
    },
)
