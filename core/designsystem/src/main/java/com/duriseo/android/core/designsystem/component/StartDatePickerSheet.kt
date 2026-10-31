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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink300
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.domain.couple.StartDateRules
import java.time.LocalDate
import java.time.YearMonth

// 처음 만난 날 선택 시트 (SCREENS.md "처음 만난 날 선택 시트", FR-COUPLE-04 AC3, D-74, D-84).
// 수치는 보드 `StartDateSheet.dc.html`. 토큰에 없는 값이라 이름을 붙인 상수로 둔다 (unit-3.md 확인 필요 14).

/** 시트 안쪽: 위 10·좌우 24·아래 28, 덩어리 사이 16, 위 모서리 24 (19행) */
private val SheetPaddingTop = 10.dp
private val SheetPaddingBottom = 28.dp
private val SheetGap = 16.dp

/** 손잡이 36x4, 모서리 2, `line-300` (20행) */
private val HandleWidth = 36.dp
private val HandleHeight = 4.dp
private val HandleRadius = 2.dp

/** 제목 위 4, 제목과 설명 사이 6 (21~22행). 설명 13/400 행간 1.6 */
private val TitleTop = 4.dp
private val TitleGap = 6.dp
private val DescriptionLineHeight = 20.8.sp

/** 달 이동 줄: 높이 44, 이전·다음 버튼 44x44, 아이콘 20 (26·28행) */
private val MonthRowHeight = 44.dp
private val MonthButtonSize = 44.dp
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

/**
 * 처음 만난 날 선택 시트의 **면**(손잡이·제목·월 달력·안내·[저장])이다. 뒤 막과 위치는 부르는 쪽이 정한다
 * (SCR-SET-01의 [처음 만난 날 수정], SCR-COUPLE-04의 필드가 같은 시트를 쓴다).
 *
 * - 고를 수 있는 날은 1950-01-01 ~ [today]다 (`StartDateRules`). 범위 밖 날짜는 비활성이고, 이전·다음 달 버튼도
 *   그 범위 끝에서 꺼진다. 지우기는 없다 (D-84).
 * - **연·월 제목을 눌러 연도·월을 고르는 동작은 디자인이 없어 만들지 않았다** (v0.9 반영 대기, unit-3.md 확인 필요 6).
 *   제목은 글자만 보인다.
 * - 상태는 호출하는 쪽이 들고 있다: 보이는 달 [visibleMonth], 고른 날 [selectedDate].
 *
 * @param saveEnabled [저장]을 누를 수 있는지. 보드는 항상 켜져 있다
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
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface0, RoundedCornerShape(topStart = SheetTopRadius, topEnd = SheetTopRadius))
            .padding(
                start = DuriseoSpacing.Space6,
                end = DuriseoSpacing.Space6,
                top = SheetPaddingTop,
                bottom = SheetPaddingBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(SheetGap),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = HandleWidth, height = HandleHeight)
                .background(Line300, RoundedCornerShape(HandleRadius))
                .clearAndSetSemantics { },
        )
        Column(
            modifier = Modifier.padding(top = TitleTop),
            verticalArrangement = Arrangement.spacedBy(TitleGap),
        ) {
            Text(
                text = stringResource(R.string.start_date_sheet_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = Ink900,
            )
            Text(
                text = stringResource(R.string.start_date_sheet_description),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Normal,
                    lineHeight = DescriptionLineHeight,
                ),
                color = Ink600,
            )
        }
        MonthHeader(
            visibleMonth = visibleMonth,
            canGoPrevious = StartDateRules.previousMonth(visibleMonth) != null,
            canGoNext = StartDateRules.nextMonth(visibleMonth, today) != null,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
        )
        Column(verticalArrangement = Arrangement.spacedBy(CalendarToNoteGap)) {
            MonthGrid(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                onDateSelected = onDateSelected,
            )
            Text(
                text = stringResource(R.string.start_date_sheet_range_note),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = NoteFontSize,
                    lineHeight = NoteLineHeight,
                    fontWeight = FontWeight.Normal,
                ),
                color = Ink600,
            )
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

/** 시트 위 모서리 24 (`radius-2xl`, 19행) */
private val SheetTopRadius = 24.dp

@Composable
private fun MonthHeader(
    visibleMonth: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MonthRowHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.start_date_sheet_month, visibleMonth.year, visibleMonth.monthValue),
            style = MaterialTheme.typography.titleMedium,
            color = Ink900,
        )
        Row {
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
    today: LocalDate,
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
                                selectable = StartDateRules.isSelectable(date, today),
                                onClick = { onDateSelected(date) },
                            )
                        }
                    }
                }
            }
        }
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
                onPreviousMonth = { StartDateRules.previousMonth(visibleMonth)?.let { visibleMonth = it } },
                onNextMonth = { StartDateRules.nextMonth(visibleMonth, today)?.let { visibleMonth = it } },
                onDateSelected = { selectedDate = it },
                onSave = {},
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
