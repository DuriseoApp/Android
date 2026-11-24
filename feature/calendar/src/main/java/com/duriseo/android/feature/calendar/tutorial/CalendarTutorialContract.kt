package com.duriseo.android.feature.calendar.tutorial

import androidx.annotation.StringRes
import com.duriseo.android.feature.calendar.R

/**
 * 캘린더 튜토리얼 5단계 (FR-CAL-06 AC2, D-168). 단계는 구멍 대상의 키도 겸한다 (`coachMarkTarget`).
 * 3단계만 커플과 개인 공간의 문구가 다르다 ([messageRes]의 `solo`).
 */
enum class CalendarTutorialStep(
    @StringRes private val messageRes: Int,
    @StringRes private val soloMessageRes: Int = messageRes,
) {
    /** 요일 행 + 월간 달력 */
    Calendar(R.string.calendar_tutorial_calendar),

    /** 추가 버튼(FAB) */
    Fab(R.string.calendar_tutorial_fab),

    /** 앱바의 "함께한 지 D+N ›"(처음 만난 날이 없으면 "처음 만난 날 입력하기 ›") / 개인 공간의 "기념일 ›" */
    Anniversary(R.string.calendar_tutorial_anniversary_couple, R.string.calendar_tutorial_anniversary_solo),

    /** 하단 탭의 메모 */
    MemoTab(R.string.calendar_tutorial_memo_tab),

    /** 앱바의 전체 메뉴 버튼 */
    MenuButton(R.string.calendar_tutorial_menu);

    /** 진행 표시의 "N/5"에서 N */
    val number: Int get() = ordinal + 1

    /** 다음 단계. 마지막 단계면 null (닫힌다) */
    fun next(): CalendarTutorialStep? = entries.getOrNull(ordinal + 1)

    @StringRes
    fun messageRes(solo: Boolean): Int = if (solo) soloMessageRes else messageRes

    companion object {
        val first: CalendarTutorialStep = entries.first()
        val count: Int = entries.size
    }
}

/**
 * 튜토리얼 상태.
 *
 * @property step 지금 단계. 안 보여 주는 중이면 null
 * @property blocked 연결 완료 시트(SCR-COUPLE-04) 같은 다른 시트·다이얼로그가 떠 있다. 그동안은 가리고, 닫히면 같은 단계로 이어서 보여 준다 (FR-CAL-06 AC1)
 * @property solo 개인 공간이다 (3단계 문구)
 */
data class CalendarTutorialUiState(
    val step: CalendarTutorialStep? = null,
    val blocked: Boolean = false,
    val solo: Boolean = false,
) {
    /** 화면 위에 그릴지 */
    val visible: Boolean get() = step != null && !blocked
}

sealed interface CalendarTutorialAction {
    /**
     * 튜토리얼을 띄울 수 있는 조건이 바뀌었다 (화면이 알려 준다).
     *
     * @param onCalendar 캘린더 탭(SCR-CAL-01)이 보이는 중이다. 벗어나면 튜토리얼을 닫는다
     * @param targetsReady 5단계의 구멍 대상 위치를 모두 쟀다 (내 정보가 늦게 와서 앱바 링크가 아직 없을 수 있다)
     * @param blocked 다른 시트·다이얼로그가 떠 있다
     */
    data class EnvironmentChanged(
        val onCalendar: Boolean,
        val targetsReady: Boolean,
        val blocked: Boolean,
    ) : CalendarTutorialAction

    /** 화면 아무 곳이나 눌렀다: 다음 단계, 마지막이면 닫는다 (AC3) */
    data object NextClicked : CalendarTutorialAction

    /** [건너뛰기]와 뒤로 가기: 바로 닫는다 (AC3) */
    data object SkipClicked : CalendarTutorialAction
}
