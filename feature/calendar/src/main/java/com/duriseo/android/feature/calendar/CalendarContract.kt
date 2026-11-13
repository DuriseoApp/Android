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

/**
 * 날짜 목록·달력에 그릴 기념일 한 건 (SCR-CAL-01, D-65·D-117).
 *
 * @property dayNumber 자동 기념일 카드의 D+N: 기념일 날짜 − 처음 만난 날 + 1 (오늘과 무관, D-117). 처음 만난 날을 모르면 null
 * @property author 직접 기념일을 만든 사람(나·상대). 자동은 null. 색은 [author]에서 `PersonColors`로 정한다
 * @property repeatsYearly 직접 기념일이 매년 반복인지 (카드 보조 줄 "· 매년")
 */
data class CalendarAnniversary(
    val name: String,
    val isAuto: Boolean,
    val dayNumber: Long?,
    val author: EventTarget?,
    val repeatsYearly: Boolean,
)

/**
 * 날짜 목록·달력에 그릴 메모 한 건 (SCR-CAL-01, 메모 날짜가 있는 메모만, D-119).
 *
 * @property title 목록 제목 (D-131): 제목이 없으면 본문 첫 줄 50자
 * @property author 작성자(나·상대). 메모의 점 색은 작성자의 사람 색이다 (함께 색이 아님, D-118)
 */
data class CalendarMemo(
    val id: String,
    val title: String,
    val author: EventTarget,
)

/** 날짜 목록의 한 줄. 순서는 기념일 -> 종일 -> 시간 -> 메모 (D-119) */
sealed interface CalendarListItem {
    /** [index]는 그날 기념일 안의 위치로 목록 키에 쓴다 */
    data class Anniversary(val index: Int, val value: CalendarAnniversary) : CalendarListItem
    data class Event(val value: CalendarEvent) : CalendarListItem
    data class Memo(val value: CalendarMemo) : CalendarListItem
}

/** 앱바. 커플이면 "우리 둘" + D-day 줄, 개인 공간이면 "나의 캘린더" (FR-CAL-01 AC3·AC5, FR-COUPLE-04 AC2) */
sealed interface CalendarHeader {
    /** 내 정보를 아직 받지 못했다. 앱바 내용은 비운다 */
    data object Loading : CalendarHeader

    data class Solo(val myColorKey: String) : CalendarHeader

    /**
     * @property dayNumber 함께한 지 D+N의 N (처음 만난 날이 1일, 최소 1: D-116). 처음 만난 날이 없으면 null이고 입력 안내가 보인다
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
    /** 보이는 달의 날짜별 기념일. 순서는 저장소(서버 응답) 순서 그대로 (D-119). 기념일 없는 날은 키가 없다 */
    val anniversariesByDay: Map<LocalDate, List<CalendarAnniversary>> = emptyMap(),
    /** 보이는 달의 날짜별 메모(메모 날짜가 있는 것). 순서는 작성 최신순, 같으면 id순 (D-119). 메모 없는 날은 키가 없다 */
    val memosByDay: Map<LocalDate, List<CalendarMemo>> = emptyMap(),
    val canGoPrevious: Boolean = true,
    val canGoNext: Boolean = true,
    val startDateSheet: CalendarStartDateSheet? = null,
    /** 당겨서 새로고침 중 (Q-16). 화면에 들어올 때·달을 넘길 때의 다시 불러오기에는 켜지 않는다 (D-112) */
    val refreshing: Boolean = false,
) {
    /** 처음 만난 날 저장 요청이 도는 동안 true. 로딩 덮개를 그린다 (D-115). 조회와 당겨서 새로고침은 덮지 않는다 */
    val loading: Boolean get() = startDateSheet?.saving == true

    /** 선택한 날의 일정 */
    val selectedEvents: List<CalendarEvent> get() = eventsByDay[selectedDate].orEmpty()

    /** 선택한 날의 기념일 */
    val selectedAnniversaries: List<CalendarAnniversary> get() = anniversariesByDay[selectedDate].orEmpty()

    /** 선택한 날의 메모 */
    val selectedMemos: List<CalendarMemo> get() = memosByDay[selectedDate].orEmpty()

    /** 선택한 날의 목록: 기념일 -> 종일 일정 -> 시간 일정 -> 메모 (D-119) */
    val selectedItems: List<CalendarListItem>
        get() = selectedAnniversaries.mapIndexed { index, value -> CalendarListItem.Anniversary(index, value) } +
            selectedEvents.map { CalendarListItem.Event(it) } +
            selectedMemos.map { CalendarListItem.Memo(it) }
}

sealed interface CalendarAction {
    /** 화면이 보이기 시작했다 (`STARTED`). 보이는 달을 다시 불러온다 (NFR-SYNC-02) */
    data object Shown : CalendarAction
    data object PreviousMonth : CalendarAction
    data object NextMonth : CalendarAction
    data class DateSelected(val date: LocalDate) : CalendarAction
    data object ToggleExpanded : CalendarAction
    data class EventClicked(val id: String) : CalendarAction

    /** 날짜 목록의 메모 카드: SCR-MEMO-03으로 간다 (FR-CAL-01 AC2) */
    data class MemoClicked(val id: String) : CalendarAction
    data object AddClicked : CalendarAction
    data object StartDatePromptClicked : CalendarAction

    /** 앱바 "함께한 지 D+N ›"·"기념일 ›"와 기념일 카드: SCR-ANNIV-01로 간다 (FR-CAL-01 AC2·AC3·AC5) */
    data object AnniversariesClicked : CalendarAction

    /** 날짜 목록을 당겼다 (Q-16, NFR-SYNC-02) */
    data object Refresh : CalendarAction

    data object StartDatePreviousMonth : CalendarAction
    data object StartDateNextMonth : CalendarAction
    data class StartDateSelected(val date: LocalDate) : CalendarAction
    data class StartDateMonthSelected(val month: YearMonth) : CalendarAction
    data object StartDateSaved : CalendarAction
    data object StartDateSheetDismissed : CalendarAction
}

sealed interface CalendarSideEffect {
    data class OpenEvent(val id: String) : CalendarSideEffect

    /** SCR-MEMO-03 */
    data class OpenMemo(val id: String) : CalendarSideEffect

    /** SCR-ANNIV-01 */
    data object OpenAnniversaries : CalendarSideEffect

    /** 새 일정 작성. [date]는 선택한 날 (`YYYY-MM-DD`) */
    data class OpenCreate(val date: LocalDate) : CalendarSideEffect

    data class ShowMessage(@StringRes val messageRes: Int) : CalendarSideEffect
}
