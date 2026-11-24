package com.duriseo.android.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import java.time.YearMonth

// 연도·월 휠 (D-167, SCR-CAL-01 "연도와 월" 시트). 모양·동작은 시간 휠과 같다: 같은 폭 2줄, 가운데 띠, 줄 높이 44, 5줄, 누르기(D-165).

/** 연도·월 휠이 읽는 순수 계산 (`YearMonthWheelValuesTest`). 범위는 PRD 날짜 범위 1950-01 ~ 2050-12 (domain의 `EventRules`와 같은 값) */
object YearMonthWheelValues {
    const val MIN_YEAR = 1950
    const val MAX_YEAR = 2050

    val YEARS: List<Int> = (MIN_YEAR..MAX_YEAR).toList()
    val MONTHS: List<Int> = (1..12).toList()

    fun yearIndex(year: Int): Int = YEARS.indexOf(year.coerceIn(MIN_YEAR, MAX_YEAR))

    fun monthIndex(month: Int): Int = MONTHS.indexOf(month.coerceIn(1, 12))

    fun yearMonth(year: Int, month: Int): YearMonth = YearMonth.of(year.coerceIn(MIN_YEAR, MAX_YEAR), month.coerceIn(1, 12))
}

/**
 * 연도("N년") · 월("N월") 휠 2줄. 처음에는 [initial]이 가운데에 놓이고 이후 값은 휠이 들고 있다.
 * 값이 바뀔 때마다 [onChange]로 알린다.
 */
@Composable
fun DuriseoYearMonthWheel(
    initial: YearMonth,
    onChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    val yearTexts = YearMonthWheelValues.YEARS.map { stringResource(R.string.year_month_wheel_year, it) }
    val monthTexts = YearMonthWheelValues.MONTHS.map { stringResource(R.string.year_month_wheel_month, it) }
    DuriseoWheelFrame(modifier = modifier) {
        DuriseoWheelColumn(
            items = yearTexts,
            initialIndex = YearMonthWheelValues.yearIndex(year),
            label = stringResource(R.string.year_month_wheel_year_label),
            onSelected = {
                year = YearMonthWheelValues.YEARS[it]
                onChange(YearMonthWheelValues.yearMonth(year, month))
            },
            modifier = Modifier.weight(1f),
        )
        DuriseoWheelColumn(
            items = monthTexts,
            initialIndex = YearMonthWheelValues.monthIndex(month),
            label = stringResource(R.string.year_month_wheel_month_label),
            onSelected = {
                month = YearMonthWheelValues.MONTHS[it]
                onChange(YearMonthWheelValues.yearMonth(year, month))
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/** 2026년 11월: 연도 2024~2028, 월 9~12가 보이고 위쪽 두 줄만 비어 있지 않다 */
@Preview(showBackground = true, widthDp = 342)
@Composable
private fun DuriseoYearMonthWheelPreview() {
    DuriseoTheme { DuriseoYearMonthWheel(initial = YearMonth.of(2026, 11), onChange = {}) }
}

/** 1950년 1월: 위쪽 줄이 비어 있다 */
@Preview(showBackground = true, widthDp = 342)
@Composable
private fun DuriseoYearMonthWheelMinPreview() {
    DuriseoTheme { DuriseoYearMonthWheel(initial = YearMonth.of(1950, 1), onChange = {}) }
}
