package com.duriseo.android.core.designsystem.component

import androidx.compose.runtime.Immutable
import com.duriseo.android.domain.couple.StartDateRules
import com.duriseo.android.domain.event.EventRules
import java.time.LocalDate
import java.time.YearMonth

/**
 * 날짜 선택 시트가 고를 수 있는 날짜 범위. 처음 만난 날은 1950-01-01 ~ 오늘 (FR-COUPLE-04 AC3),
 * 일정은 1950-01-01 ~ 2050-12-31 (FR-CAL-01 AC6, D-74)이라 시트가 범위를 인자로 받는다.
 */
@Immutable
interface DatePickerRange {
    fun isSelectable(date: LocalDate): Boolean

    /** 이전 달. 범위 앞이면 null (이전 달 버튼이 꺼진다) */
    fun previousMonth(month: YearMonth): YearMonth?

    /** 다음 달. 범위 뒤면 null (다음 달 버튼이 꺼진다) */
    fun nextMonth(month: YearMonth): YearMonth?

    /** 연도·월 선택에서 이 달을 고를 수 있는지 */
    fun isMonthSelectable(month: YearMonth): Boolean

    /** 연도·월 선택에 보일 연도 (최근 순) */
    fun years(): List<Int>
}

/** 1950-01-01 ~ [today] (처음 만난 날) */
@Immutable
class UntilTodayRange(private val today: LocalDate) : DatePickerRange {
    override fun isSelectable(date: LocalDate) = StartDateRules.isSelectable(date, today)
    override fun previousMonth(month: YearMonth) = StartDateRules.previousMonth(month)
    override fun nextMonth(month: YearMonth) = StartDateRules.nextMonth(month, today)
    override fun isMonthSelectable(month: YearMonth) = StartDateRules.isMonthSelectable(month, today)
    override fun years() = StartDateRules.years(today)

    override fun equals(other: Any?) = other is UntilTodayRange && other.today == today
    override fun hashCode() = today.hashCode()
}

/** 1950-01-01 ~ 2050-12-31 (일정, FR-CAL-01 AC6) */
@Immutable
object CalendarDateRange : DatePickerRange {
    override fun isSelectable(date: LocalDate) = EventRules.isSelectable(date)
    override fun previousMonth(month: YearMonth) = EventRules.previousMonth(month)
    override fun nextMonth(month: YearMonth) = EventRules.nextMonth(month)
    override fun isMonthSelectable(month: YearMonth) = month >= EventRules.MIN_MONTH && month <= EventRules.MAX_MONTH
    override fun years() = EventRules.years().reversed()
}
