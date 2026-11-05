package com.duriseo.android.feature.calendar

import androidx.annotation.StringRes
import com.duriseo.android.domain.event.EventTarget
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** 날짜 목록·달력에 그릴 일정 한 건 (SCR-CAL-01). 색은 [target]에서 `PersonColors`로 정한다 (FR-CAL-02 AC4) */
data class CalendarEvent(
    val id: String,
    val title: String,
    val note: String?,
    val isAllDay: Boolean,
    /** 시간 일정의 시작 시각(기기 시간대). 종일이면 null. 여러 날 일정도 모든 날에 시작 시각 그대로 쓴다 (unit-4.md 확인 필요 13) */
    val startTime: LocalTime?,
    val target: EventTarget,
)

/** 앱바. 커플이면 "우리 둘" + D-day 줄, 개인 공간이면 "나의 캘린더" (FR-CAL-01 AC3·AC5, FR-COUPLE-04 AC2) */
sealed interface CalendarHeader {
    /** 내 정보를 아직 받지 못했다. 앱바 내용은 비운다 */
    data object Loading : CalendarHeader

    data class Solo(val myColorKey: String) : CalendarHeader

    /**
     * @property dayNumber 함께한 지 D+N의 N (처음 만난 날이 1일). 처음 만난 날이 없으면 null이고 입력 안내가 보인다
     */
    data class Couple(
        val myColorKey: String,
        val partnerColorKey: String,
        val dayNumber: Long?,
    ) : CalendarHeader
}

/** 처음 만난 날 선택 시트 (SCR-CAL-01 앱바 "처음 만난 날 입력하기", FR-COUPLE-04 AC2). 설정 화면의 시트와 같은 규칙 */
data class CalendarStartDateSheet(
    val visibleMonth: YearMonth,
    val selected: LocalDate?,
    val saving: Boolean = false,
)

data class CalendarUiState(
    val header: CalendarHeader = CalendarHeader.Loading,
    val today: LocalDate = LocalDate.of(2026, 1, 1),
    val visibleMonth: YearMonth = YearMonth.of(2026, 1),
    val selectedDate: LocalDate = LocalDate.of(2026, 1, 1),
    /** 크게 보기: 날짜 칸에 일정 막대 (SCREENS.md SCR-CAL-01 "펼친 상태") */
    val expanded: Boolean = false,
    /** 보이는 달의 날짜별 일정. 순서는 종일 일정 -> 시간 일정(시작 시각순) (FR-CAL-01 AC2). 일정 없는 날은 키가 없다 */
    val eventsByDay: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val canGoPrevious: Boolean = true,
    val canGoNext: Boolean = true,
    val startDateSheet: CalendarStartDateSheet? = null,
) {
    /** 선택한 날의 목록 */
    val selectedEvents: List<CalendarEvent> get() = eventsByDay[selectedDate].orEmpty()
}

sealed interface CalendarAction {
    /** 화면이 보이기 시작했다 (`STARTED`). 보이는 달을 다시 불러온다 (NFR-SYNC-02) */
    data object Shown : CalendarAction
    data object PreviousMonth : CalendarAction
    data object NextMonth : CalendarAction
    data class DateSelected(val date: LocalDate) : CalendarAction
    data object ToggleExpanded : CalendarAction
    data class EventClicked(val id: String) : CalendarAction
    data object AddClicked : CalendarAction
    data object StartDatePromptClicked : CalendarAction

    data object StartDatePreviousMonth : CalendarAction
    data object StartDateNextMonth : CalendarAction
    data class StartDateSelected(val date: LocalDate) : CalendarAction
    data class StartDateMonthSelected(val month: YearMonth) : CalendarAction
    data object StartDateSaved : CalendarAction
    data object StartDateSheetDismissed : CalendarAction
}

sealed interface CalendarSideEffect {
    data class OpenEvent(val id: String) : CalendarSideEffect

    /** 새 일정 작성. [date]는 선택한 날 (`YYYY-MM-DD`) */
    data class OpenCreate(val date: LocalDate) : CalendarSideEffect

    data class ShowMessage(@StringRes val messageRes: Int) : CalendarSideEffect
}
