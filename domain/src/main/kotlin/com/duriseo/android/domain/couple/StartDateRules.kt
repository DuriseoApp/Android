package com.duriseo.android.domain.couple

import java.time.LocalDate
import java.time.YearMonth

/**
 * 처음 만난 날로 고를 수 있는 범위: 1950-01-01 ~ 오늘 (FR-COUPLE-04.AC3, D-74, API.md 3.2 "공간 변경 규칙").
 * 서버도 같은 범위로 거부하지만(`VALIDATION_FAILED`·`DATE_OUT_OF_RANGE`) 앱은 범위 밖 날짜를 고를 수 없게 한다.
 */
object StartDateRules {
    val MIN_DATE: LocalDate = LocalDate.of(1950, 1, 1)

    /** [date]가 [today]를 기준으로 고를 수 있는 날인지 */
    fun isSelectable(date: LocalDate, today: LocalDate): Boolean =
        !date.isBefore(MIN_DATE) && !date.isAfter(today)

    /** 달력이 처음 보여 줄 달: 이미 고른 날이 있으면 그 달, 없으면 오늘이 든 달 */
    fun initialMonth(selected: LocalDate?, today: LocalDate): YearMonth = YearMonth.from(selected ?: today)

    /** [month] 이전 달. 1950년 1월보다 앞이면 null (이전 달 버튼이 비활성) */
    fun previousMonth(month: YearMonth): YearMonth? =
        month.minusMonths(1).takeIf { !it.isBefore(YearMonth.from(MIN_DATE)) }

    /** [month] 다음 달. 오늘이 든 달보다 뒤면 null (다음 달 버튼이 비활성) */
    fun nextMonth(month: YearMonth, today: LocalDate): YearMonth? =
        month.plusMonths(1).takeIf { !it.isAfter(YearMonth.from(today)) }
}
