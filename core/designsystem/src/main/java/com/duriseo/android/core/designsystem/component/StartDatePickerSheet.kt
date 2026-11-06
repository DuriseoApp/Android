package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.Danger600
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink300
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.OnBrand
import java.time.LocalDate
import java.time.YearMonth

// 처음 만난 날 선택 시트 (SCREENS.md "처음 만난 날 선택 시트", FR-COUPLE-04 AC3, D-74, D-84).
// 수치는 보드 `StartDateSheet.dc.html`. 토큰에 없는 값이라 이름을 붙인 상수로 둔다 (unit-3.md 확인 필요 14).

/** 덩어리 사이 16 (19행). 면(위 모서리 24, 손잡이, 안쪽 여백)은 [DuriseoSheetPanel] */
private val SheetGap = DuriseoSpacing.Space4

/** 제목 위 4, 제목과 설명 사이 6 (21~22행). 설명 13/400 행간 1.6 */
private val TitleTop = DuriseoSpacing.Space1
private val TitleGap = 6.dp
private val DescriptionLineHeight = 20.8.sp

/** 달 이동 줄: 높이 44, 이전·다음 버튼 44x44, 아이콘 20 (26·28행) */
private val MonthRowHeight = DuriseoSize.Touch
private val MonthButtonSize = DuriseoSize.Touch
private val MonthIconSize = 20.dp

/** 요일 줄 높이 28 (12/600), 날짜 줄 높이 40, 줄 사이 2, 날짜 원 36 (33·34행) */
private val WeekdayHeight = 28.dp
private val DayRowHeight = 40.dp
private val DayRowGap = 2.dp
private val DayCircleSize = 36.dp

/** 요일 글자 12/600 (`caption`), 날짜 글자 15/500·선택 700 */
private val DayFontSize = 15.sp
private val DayLineHeight = 22.sp

/** 달력과 안내 문구 사이: 덩어리 사이 16에서 6을 당겨 10 (보드 `margin: -6px 0 0`) */
private val CalendarToNoteGap = 10.dp

/** 안내 문구 12/400 행간 16 (37행) */
private val NoteFontSize = 12.sp
private val NoteLineHeight = 16.sp

private const val DAYS_PER_WEEK = 7

/** 연도·월 선택의 격자: 연도는 4열, 월은 4열 3줄. 칸 높이는 날짜 줄과 같고 칸 사이는 날짜 줄 사이와 같다 */
private const val PICKER_COLUMNS = 4
private const val MONTHS_PER_YEAR = 12

/** 제목 옆 ⌄: 오른쪽 화살표 아이콘을 아래쪽으로 돌린 것 (보드 `StartDateSheet.dc.html` 27행의 ⌄ 버튼 자리) */
private const val CHEVRON_DOWN_ROTATION = 90f

/**
 * 처음 만난 날 선택 시트의 **면**(손잡이·제목·월 달력·안내·[저장])이다. 뒤 막과 위치는 부르는 쪽이 정한다
 * (SCR-SET-01의 [처음 만난 날 수정], SCR-COUPLE-04의 필드가 같은 시트를 쓴다).
 *
 * - 고를 수 있는 날은 1950-01-01 ~ [today]다 (`StartDateRules`). 범위 밖 날짜는 비활성이고, 이전·다음 달 버튼도
 *   그 범위 끝에서 꺼진다. 지우기는 없다 (D-84).
 * - 연·월 제목을 누르면 연도 목록 → 월 목록으로 달을 고른다 (1950-01 ~ 오늘이 든 달, 미래 월은 비활성).
 *   **디자인 반영 후 맞춤(이미지 대기)**: SCREENS.md "연도·월 선택"은 이미지 예정(`SCR-SET-01--start-date-sheet-year-month.png`)이고
 *   그때까지는 Android가 정한다. 지금 모양은 기존 컴포넌트·토큰만 쓴 단순한 격자다 (unit-3.md 확인 필요 6).
 * - 상태는 호출하는 쪽이 들고 있다: 보이는 달 [visibleMonth], 고른 날 [selectedDate].
 *
 * @param saveEnabled [저장]을 누를 수 있는지. 보드는 항상 켜져 있다
 * @param range 고를 수 있는 날짜 범위. 기본은 1950-01-01 ~ [today]. 일정은 [CalendarDateRange] (FR-CAL-01 AC6)
 * @param title 시트 제목. 기본은 "처음 만난 날". 일정은 "시작"·"종료" (새 문구를 만들지 않는다)
 * @param description 제목 아래 설명. null이면 줄이 없다
 * @param rangeNote 달력 아래 안내. null이면 줄이 없다 (일정은 안내 문구가 문서에 없다, unit-4.md 확인 필요 1)
 */
@Composable
fun StartDatePickerSheet(
    visibleMonth: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = true,
    onMonthSelected: ((YearMonth) -> Unit)? = null,
    range: DatePickerRange = UntilTodayRange(today),
    title: String = stringResource(R.string.start_date_sheet_title),
    description: String? = stringResource(R.string.start_date_sheet_description),
    rangeNote: String? = stringResource(R.string.start_date_sheet_range_note),
) {
    // 연도·월 선택이 열려 있는지, 열려 있다면 어느 해의 월을 고르는 중인지(null이면 연도 목록)
    var pickerOpen by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableStateOf<Int?>(null) }
    DuriseoSheetPanel(modifier = modifier, gap = SheetGap) {
        Column(
            modifier = Modifier.padding(top = TitleTop),
            verticalArrangement = Arrangement.spacedBy(TitleGap),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = Ink900,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = DescriptionLineHeight,
                    ),
                    color = Ink600,
                )
            }
        }
        MonthHeader(
            visibleMonth = visibleMonth,
            canGoPrevious = range.previousMonth(visibleMonth) != null,
            canGoNext = range.nextMonth(visibleMonth) != null,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            pickerOpen = pickerOpen,
            onTitleClick = onMonthSelected?.let {
                {
                    pickerYear = null
                    pickerOpen = !pickerOpen
                }
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(CalendarToNoteGap)) {
            if (pickerOpen && onMonthSelected != null) {
                YearMonthPicker(
                    visibleMonth = visibleMonth,
                    range = range,
                    year = pickerYear,
                    onYearSelected = { pickerYear = it },
                    onBackToYears = { pickerYear = null },
                    onMonthSelected = {
                        onMonthSelected(it)
                        pickerOpen = false
                        pickerYear = null
                    },
                )
            } else {
                MonthGrid(
                    visibleMonth = visibleMonth,
                    selectedDate = selectedDate,
                    range = range,
                    onDateSelected = onDateSelected,
                )
            }
            if (rangeNote != null) {
                Text(
                    text = rangeNote,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = NoteFontSize,
                        lineHeight = NoteLineHeight,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = Ink600,
                )
            }
        }
        DuriseoLargeButton(
            text = stringResource(R.string.start_date_sheet_save),
            onClick = onSave,
            container = Brand500,
            contentColor = OnBrand,
            enabled = saveEnabled,
        )
    }
}

@Composable
private fun MonthHeader(
    visibleMonth: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    pickerOpen: Boolean,
    onTitleClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MonthRowHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val pickLabel = stringResource(R.string.start_date_sheet_pick_year_month)
        Row(
            modifier = Modifier
                .height(MonthRowHeight)
                .then(
                    if (onTitleClick != null) {
                        Modifier
                            .clickable(role = Role.Button, onClick = onTitleClick)
                            .semantics { contentDescription = pickLabel }
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.start_date_sheet_month, visibleMonth.year, visibleMonth.monthValue),
                modifier = if (onTitleClick != null) Modifier.clearAndSetSemantics { } else Modifier,
                style = MaterialTheme.typography.titleMedium,
                color = Ink900,
            )
            if (onTitleClick != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(MonthIconSize)
                        .rotate(if (pickerOpen) -CHEVRON_DOWN_ROTATION else CHEVRON_DOWN_ROTATION),
                    tint = Ink600,
                )
            }
        }
        // 연도·월 선택이 열려 있는 동안은 달 이동 버튼을 숨긴다
        if (!pickerOpen) Row {
            MonthButton(
                iconRes = R.drawable.ic_back,
                description = stringResource(R.string.start_date_sheet_previous_month),
                enabled = canGoPrevious,
                onClick = onPreviousMonth,
            )
            MonthButton(
                iconRes = R.drawable.ic_chevron_right,
                description = stringResource(R.string.start_date_sheet_next_month),
                enabled = canGoNext,
                onClick = onNextMonth,
            )
        }
    }
}

@Composable
private fun MonthButton(iconRes: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(MonthButtonSize)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier.size(MonthIconSize),
            // 끝에서 꺼진 버튼의 모양은 보드에 없어 `ink-300`(비활성 글자 자리)로 둔다
            tint = if (enabled) Ink600 else Ink300,
        )
    }
}

@Composable
private fun MonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate?,
    range: DatePickerRange,
    onDateSelected: (LocalDate) -> Unit,
) {
    val weekdays = stringArrayResource(R.array.weekday_short)
    // 일요일 시작: 달의 1일 앞 빈 칸 수 = 요일 값(월 1 ... 일 7)을 7로 나눈 나머지
    val leadingBlanks = visibleMonth.atDay(1).dayOfWeek.value % DAYS_PER_WEEK
    val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..visibleMonth.lengthOfMonth()).map { visibleMonth.atDay(it) }
    val rows = cells.chunked(DAYS_PER_WEEK)

    Column(verticalArrangement = Arrangement.spacedBy(DayRowGap)) {
        Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            weekdays.forEachIndexed { index, weekday ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(WeekdayHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = weekday,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (index == 0) Danger600 else Ink500,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // 마지막 줄은 빈 칸으로 채워 칸 폭을 맞춘다
                repeat(DAYS_PER_WEEK) { column ->
                    val date = row.getOrNull(column)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(DayRowHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                column = column,
                                selected = date == selectedDate,
                                selectable = range.isSelectable(date),
                                onClick = { onDateSelected(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 연도·월 선택 (이미지 대기). [year]가 null이면 연도 목록(최근 순), 있으면 그 해의 12개 월.
 * 높이는 월 달력(요일 줄 + 6줄)과 같게 해서 열고 닫을 때 시트가 출렁이지 않게 한다.
 */
@Composable
private fun YearMonthPicker(
    visibleMonth: YearMonth,
    range: DatePickerRange,
    year: Int?,
    onYearSelected: (Int) -> Unit,
    onBackToYears: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
) {
    val height = WeekdayHeight + DayRowHeight * 6 + DayRowGap * 5
    if (year == null) {
        val years = range.years()
        // 열면 지금 고른 연도가 보이는 줄로 스크롤해 둔다
        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = years.indexOf(visibleMonth.year).coerceAtLeast(0),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(PICKER_COLUMNS),
            modifier = Modifier.fillMaxWidth().height(height),
            state = gridState,
            verticalArrangement = Arrangement.spacedBy(DayRowGap),
        ) {
            items(years) { y ->
                val text = stringResource(R.string.start_date_sheet_year, y)
                PickerCell(
                    text = text,
                    selected = y == visibleMonth.year,
                    enabled = true,
                    onClick = { onYearSelected(y) },
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().height(height),
            verticalArrangement = Arrangement.spacedBy(DayRowGap),
        ) {
            // 연도 표시를 누르면 연도 목록으로 돌아간다
            val yearText = stringResource(R.string.start_date_sheet_year, year)
            Row(
                modifier = Modifier
                    .height(DuriseoSize.Touch)
                    .clickable(role = Role.Button, onClick = onBackToYears)
                    .semantics { contentDescription = yearText },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = yearText,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink900,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(MonthIconSize).rotate(CHEVRON_DOWN_ROTATION),
                    tint = Ink600,
                )
            }
            (1..MONTHS_PER_YEAR).chunked(PICKER_COLUMNS).forEach { months ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    months.forEach { m ->
                        val month = YearMonth.of(year, m)
                        Box(modifier = Modifier.weight(1f)) {
                            PickerCell(
                                text = stringResource(R.string.start_date_sheet_month_name, m),
                                selected = month == visibleMonth,
                                enabled = range.isMonthSelectable(month),
                                onClick = { onMonthSelected(month) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerCell(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val label = if (selected) stringResource(R.string.start_date_sheet_selected, text) else text
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Touch)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) Brand500 else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.clearAndSetSemantics { },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = DayFontSize,
                lineHeight = DayLineHeight,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = when {
                selected -> OnBrand
                !enabled -> Ink300
                else -> Ink900
            },
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    column: Int,
    selected: Boolean,
    selectable: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(
        if (selected) R.string.start_date_sheet_day_selected else R.string.start_date_sheet_day,
        date.monthValue,
        date.dayOfMonth,
    )
    val textColor: Color = when {
        selected -> OnBrand
        !selectable -> Ink300
        column == 0 -> Danger600
        else -> Ink900
    }
    Box(
        modifier = Modifier
            .size(DayCircleSize)
            .background(if (selected) Brand500 else Color.Transparent, DuriseoShapes.Full)
            .clickable(enabled = selectable, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            modifier = Modifier.clearAndSetSemantics { },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = DayFontSize,
                lineHeight = DayLineHeight,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = textColor,
        )
    }
}

// Preview의 날짜는 보드(2026년 4월 7일 선택)와 같은 견본이다.

@Composable
private fun PreviewSheet(
    month: YearMonth,
    selected: LocalDate?,
    today: LocalDate = LocalDate.of(2026, 4, 30),
) {
    val range = UntilTodayRange(today)
    DuriseoTheme {
        var visibleMonth by remember { mutableStateOf(month) }
        var selectedDate by remember { mutableStateOf(selected) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink900.copy(alpha = 0.48f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            StartDatePickerSheet(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                onPreviousMonth = { range.previousMonth(visibleMonth)?.let { visibleMonth = it } },
                onNextMonth = { range.nextMonth(visibleMonth)?.let { visibleMonth = it } },
                onDateSelected = { selectedDate = it },
                onSave = {},
                onMonthSelected = { visibleMonth = it },
            )
        }
    }
}

/** `SCR-SET-01--start-date-sheet.png`: 2026년 4월, 7일 선택 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartDatePickerSheetPreview() = PreviewSheet(YearMonth.of(2026, 4), LocalDate.of(2026, 4, 7))

/** 날짜를 아직 안 골랐을 때 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartDatePickerSheetNoSelectionPreview() = PreviewSheet(YearMonth.of(2026, 4), null)

/** 오늘이 든 달: 오늘 이후 날짜는 비활성이고 다음 달 버튼도 꺼진다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartDatePickerSheetFutureDisabledPreview() = PreviewSheet(YearMonth.of(2026, 4), LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 15))

/** 1950년 1월: 이전 달 버튼이 꺼진다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartDatePickerSheetEarliestMonthPreview() = PreviewSheet(YearMonth.of(1950, 1), null)

/** 디자인 반영 후 맞춤(이미지 대기): 연도·월 선택의 연도 목록 */
@Preview(showBackground = true, widthDp = 390)
@Composable
private fun YearMonthPickerYearsPreview() {
    DuriseoTheme {
        YearMonthPicker(YearMonth.of(2026, 4), UntilTodayRange(LocalDate.of(2026, 4, 30)), null, {}, {}, {})
    }
}

/** 디자인 반영 후 맞춤(이미지 대기): 연도·월 선택의 월 목록 (오늘이 든 해라 5월 이후는 비활성) */
@Preview(showBackground = true, widthDp = 390)
@Composable
private fun YearMonthPickerMonthsPreview() {
    DuriseoTheme {
        YearMonthPicker(YearMonth.of(2026, 4), UntilTodayRange(LocalDate.of(2026, 4, 30)), 2026, {}, {}, {})
    }
}
