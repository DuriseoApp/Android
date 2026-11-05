package com.duriseo.android.feature.calendar.edit

import androidx.annotation.StringRes
import com.duriseo.android.domain.event.ReminderOption
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** 시작·종료 중 어느 행인가 */
enum class EditField { Start, End }

/** 작성 화면 위에 뜨는 선택 시트 (날짜·시간·알림). 디자인 반영 후 맞춤(이미지 대기, unit-4.md 확인 필요 1·2) */
sealed interface EditSheet {
    data class DatePicker(val field: EditField, val visibleMonth: YearMonth, val selected: LocalDate) : EditSheet
    data class TimePicker(val field: EditField, val initial: LocalTime) : EditSheet
    data object Reminder : EditSheet
}

/**
 * SCR-CAL-03 일정 작성·수정 (FR-CAL-02, FR-CAL-03, FR-NOTI-04).
 *
 * @property isEdit 수정 화면이면 true (앱바 "일정 수정", 알림 보내기 스위치 없음)
 * @property isCouple 커플 공간이면 대상 선택과 알림 보내기(작성)가 있다. 개인 공간은 둘 다 없다
 * @property targetReadOnly 상대가 만든 "함께" 일정을 수정하는 중: 대상은 읽기 전용 "누구의 일정 · 함께" (FR-CAL-03 AC4, D-77)
 */
data class EventEditUiState(
    val loaded: Boolean = false,
    val isEdit: Boolean = false,
    val isCouple: Boolean = false,
    val targetReadOnly: Boolean = false,
    val title: String = "",
    val note: String = "",
    val isAllDay: Boolean = false,
    val startDate: LocalDate = LocalDate.of(2026, 1, 1),
    val startTime: LocalTime = LocalTime.of(0, 0),
    val endDate: LocalDate = LocalDate.of(2026, 1, 1),
    val endTime: LocalTime = LocalTime.of(0, 0),
    val together: Boolean = false,
    val notifyPartner: Boolean = true,
    val reminder: ReminderOption? = null,
    val endError: Boolean = false,
    val saveEnabled: Boolean = false,
    val saving: Boolean = false,
    val sheet: EditSheet? = null,
    val today: LocalDate = LocalDate.of(2026, 1, 1),
) {
    /** 색상 변경 링크는 대상을 고를 수 있을 때만 (커플 공간, 읽기 전용이 아닐 때) */
    val showTargetSelector: Boolean get() = isCouple && !targetReadOnly
}

sealed interface EventEditAction {
    data class TitleChanged(val text: String) : EventEditAction
    data class NoteChanged(val text: String) : EventEditAction
    data class AllDayChanged(val allDay: Boolean) : EventEditAction
    data class TogetherSelected(val together: Boolean) : EventEditAction
    data class NotifyPartnerChanged(val on: Boolean) : EventEditAction
    data object ColorChangeClicked : EventEditAction

    data class DateRowClicked(val field: EditField) : EventEditAction
    data class TimeRowClicked(val field: EditField) : EventEditAction
    data object ReminderRowClicked : EventEditAction
    data class ReminderSelected(val option: ReminderOption?) : EventEditAction

    data object DatePreviousMonth : EventEditAction
    data object DateNextMonth : EventEditAction
    data class DateMonthSelected(val month: YearMonth) : EventEditAction
    data class DatePicked(val date: LocalDate) : EventEditAction
    data object DateConfirmed : EventEditAction
    data class TimeConfirmed(val time: LocalTime) : EventEditAction
    data object SheetDismissed : EventEditAction

    data object SaveClicked : EventEditAction
    data object CloseClicked : EventEditAction
}

sealed interface EventEditSideEffect {
    /** 작성·수정을 마쳤거나 닫는다 */
    data object Close : EventEditSideEffect
    data object OpenColors : EventEditSideEffect
    data class ShowMessage(@StringRes val messageRes: Int) : EventEditSideEffect
}
