package com.duriseo.android.feature.memo.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.CalendarDateRange
import com.duriseo.android.core.designsystem.component.DuriseoBottomSheetLayer
import com.duriseo.android.core.designsystem.component.StartDatePickerSheet
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink300
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.feature.memo.R
import com.duriseo.android.feature.memo.component.dashedBorder
import java.time.LocalDate
import java.time.YearMonth

// SCR-MEMO-04 메모 입력 부분 (FR-MEMO-02, FR-NOTI-04). 수치는 보드 `AddEvent.dc.html`(메모 종류, 58~82·123~137행).
// 이 파일의 [MemoForm]·[MemoDateSheet]는 **공개 부품**이다: SCR-MEMO-04 화면(`MemoEditScreen`)과 SCR-CAL-03 [메모] 탭(묶음 C의 슬롯)이 같이 쓴다.

/** 덩어리 사이 20 (26행). 라벨과 입력 사이 8, 라벨 13/600 `ink-600` */
private val SectionGap = DuriseoSpacing.Space5
private val LabelGap = DuriseoSpacing.Space2

/** 날짜 영역: [날짜 추가] 높이 48·모서리 14·점선 1 `ink-300`, 아이콘 18과 글 사이 8 (61행) / 날짜 행 높이 56·모서리 16·테두리 1 `line-100`, 안쪽 왼 16·오른 4 (70행) */
private val DateAddHeight = DuriseoSize.ButtonMd
private val DateAddIcon = 18.dp
private val DateAddGap = DuriseoSpacing.Space2
private val DateRowHeight = 56.dp
private val DateRowPaddingStart = DuriseoSpacing.Space4
private val DateRowPaddingEnd = DuriseoSpacing.Space1
private val DateRowIcon = 20.dp
private val DateRowGap = DuriseoSpacing.Space3
private val DateRemoveIcon = 18.dp

/** 날짜 칸 위아래 안내 12/1.6 (65·79행), 날짜 영역 안 사이 8 */
private val HintLineHeight = 19.2.sp

/** 본문 입력 최소 높이: 보드 `textarea rows=2`, 안쪽 위아래 14·좌우 16, 15/1.6 (125행) */
private val BodyMinHeight = 96.dp
private val BodyPaddingVertical = 14.dp

/** 알림 카드: 안쪽 14·16, 모서리 16, 테두리 1 `line-100`, 글 사이 2, 카드 안 사이 12 (128~131행) */
private val NotifyPaddingVertical = 14.dp
private val NotifyTextGap = 2.dp

/**
 * 메모 입력 부분: 제목, 날짜 영역, 본문, 알림 스위치 (커플의 새 메모만). 앱바·[저장] 버튼·스크롤은 포함하지 않는다.
 * 바깥(SCR-MEMO-04 화면 또는 SCR-CAL-03의 [메모] 탭)이 [MemoEditUiState.saveEnabled]·[MemoEditAction.SaveClicked]로 저장을 맡는다.
 * 날짜 시트는 화면 전체를 덮어야 해서 이 부품 밖에 있다: 바깥이 [MemoDateSheet]를 화면 위층에 같이 그린다.
 */
@Composable
fun MemoForm(
    state: MemoEditUiState,
    onAction: (MemoEditAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        LabeledField(label = stringResource(R.string.memo_edit_title_label)) {
            TextInput(
                value = state.title,
                onValueChange = { onAction(MemoEditAction.TitleChanged(it)) },
                placeholder = stringResource(R.string.memo_edit_title_placeholder),
                singleLine = true,
                enabled = !state.saving,
            )
        }

        DateSection(date = state.memoDate, enabled = !state.saving, onAction = onAction)

        LabeledField(label = stringResource(R.string.memo_edit_body_label)) {
            TextInput(
                value = state.body,
                onValueChange = { onAction(MemoEditAction.BodyChanged(it)) },
                placeholder = stringResource(R.string.memo_edit_body_placeholder),
                singleLine = false,
                enabled = !state.saving,
                modifier = Modifier.heightIn(min = BodyMinHeight),
            )
        }

        // 커플 공간의 새 메모에서만. 수정·개인 공간에는 스위치도 값도 없다 (FR-NOTI-04 AC3)
        if (state.showNotify) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .border(1.dp, Line100, MaterialTheme.shapes.large)
                    .padding(horizontal = DuriseoSpacing.Space4, vertical = NotifyPaddingVertical),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NotifyTextGap)) {
                    Text(
                        text = stringResource(R.string.memo_edit_notify_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Ink900,
                    )
                    Text(
                        text = stringResource(R.string.memo_edit_notify_description),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                        color = Ink600,
                    )
                }
                Switch(
                    checked = state.notifyPartner,
                    onCheckedChange = { onAction(MemoEditAction.NotifyChanged(it)) },
                    enabled = !state.saving,
                )
            }
        }
    }
}

/**
 * 날짜 선택 시트(1950-01-01 ~ 2050-12-31, D-74). 화면 전체를 덮는 뒤 막과 아래 시트이고, 바깥이 `DuriseoDialogLayer`의 `dialog` 자리에 둔다:
 * `dialogVisible = state.dateSheet != null`, `dialog = { MemoDateSheet(state, onAction) }`. [MemoEditUiState.dateSheet]가 null이면 아무것도 그리지 않는다.
 */
@Composable
fun MemoDateSheet(
    state: MemoEditUiState,
    onAction: (MemoEditAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheet = state.dateSheet ?: return
    BackHandler { onAction(MemoEditAction.DateSheetDismissed) }
    DuriseoBottomSheetLayer(onDismissRequest = { onAction(MemoEditAction.DateSheetDismissed) }, modifier = modifier) {
        StartDatePickerSheet(
            visibleMonth = sheet.visibleMonth,
            selectedDate = sheet.selected,
            today = state.today,
            onPreviousMonth = { onAction(MemoEditAction.DatePreviousMonth) },
            onNextMonth = { onAction(MemoEditAction.DateNextMonth) },
            onDateSelected = { onAction(MemoEditAction.DatePicked(it)) },
            onSave = { onAction(MemoEditAction.DateConfirmed) },
            onMonthSelected = { onAction(MemoEditAction.DateMonthSelected(it)) },
            range = CalendarDateRange,
            // 제목은 날짜 행의 라벨 "날짜"를 쓴다. 설명·안내 줄은 없다 (SCR-CAL-03 날짜 시트와 같다)
            title = stringResource(R.string.memo_edit_date_sheet_title),
            description = null,
            rangeNote = null,
            confirmText = stringResource(DesignR.string.action_done),
        )
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LabelGap)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Ink600)
        content()
    }
}

@Composable
private fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DuriseoSize.Input)
            .background(Surface0, shape)
            .border(1.dp, Line300, shape),
        enabled = enabled,
        singleLine = singleLine,
        // 제목 17, 본문 15/1.6 (35·125행)
        textStyle = (if (singleLine) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal) else MaterialTheme.typography.bodyMedium)
            .copy(color = Ink900),
        cursorBrush = SolidColor(Ink900),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.padding(
                    horizontal = DuriseoSpacing.Space4,
                    vertical = if (singleLine) DuriseoSpacing.Space3 else BodyPaddingVertical,
                ),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = if (singleLine) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal) else MaterialTheme.typography.bodyMedium,
                        color = Ink500,
                    )
                }
                inner()
            }
        },
    )
}

/**
 * 날짜 영역 (FR-MEMO-02 AC3): 기본은 날짜가 없고 [날짜 추가] 버튼만 있다. 날짜를 고르면 버튼이 날짜 행으로 바뀌고,
 * 행 안의 제거 버튼("날짜 빼기")으로 뺄 수 있다. 날짜 행을 누르면 날짜를 바꾼다.
 */
@Composable
private fun DateSection(date: LocalDate?, enabled: Boolean, onAction: (MemoEditAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
        if (date == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DateAddHeight)
                    .dashedBorder(Ink300, radius = 14.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(enabled = enabled, role = Role.Button) { onAction(MemoEditAction.DateAddClicked) },
                horizontalArrangement = Arrangement.spacedBy(DateAddGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_calendar_plus),
                    contentDescription = null,
                    modifier = Modifier.size(DateAddIcon),
                    tint = Ink900,
                )
                Text(
                    text = stringResource(R.string.memo_edit_date_add),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink900,
                )
            }
            Hint(stringResource(R.string.memo_edit_date_add_hint))
        } else {
            val dateText = fullDateText(date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DateRowHeight)
                    .clip(MaterialTheme.shapes.large)
                    .border(1.dp, Line100, MaterialTheme.shapes.large)
                    .padding(start = DateRowPaddingStart, end = DateRowPaddingEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DateRowGap),
                ) {
                    Icon(
                        painter = painterResource(DesignR.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(DateRowIcon),
                        tint = Ink600,
                    )
                    Text(
                        text = stringResource(R.string.memo_edit_date_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink600,
                    )
                }
                val valueDescription = stringResource(R.string.memo_edit_date_value_description, dateText)
                Box(
                    modifier = Modifier
                        .heightIn(min = DuriseoSize.Touch)
                        .clickable(enabled = enabled, role = Role.Button) { onAction(MemoEditAction.DateRowClicked) }
                        .padding(horizontal = DuriseoSpacing.Space1)
                        .semantics { contentDescription = valueDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Ink900,
                    )
                }
                val removeDescription = stringResource(R.string.memo_edit_date_remove)
                Box(
                    modifier = Modifier
                        .size(DuriseoSize.Touch)
                        .clickable(enabled = enabled, role = Role.Button) { onAction(MemoEditAction.DateRemoved) }
                        .semantics { contentDescription = removeDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(DesignR.drawable.ic_close),
                        contentDescription = null,
                        modifier = Modifier.size(DateRemoveIcon),
                        tint = Ink500,
                    )
                }
            }
            Hint(stringResource(R.string.memo_edit_date_hint, date.monthValue, date.dayOfMonth))
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = HintLineHeight),
        color = Ink600,
    )
}

/** 날짜 행의 값: "2026년 10월 17일 (토)". 연도는 항상 붙는다 (보드 73행) */
@Composable
private fun fullDateText(date: LocalDate): String {
    val weekday = stringArrayResource(DesignR.array.weekday_short)[date.dayOfWeek.value % 7]
    return stringResource(DesignR.string.date_year_month_day, date.year, date.monthValue, date.dayOfMonth) + " ($weekday)"
}

// --- Preview. 견본은 보드 `AddEvent.dc.html`(커플, 메모 종류)와 같다 ---

private val PreviewBase = MemoEditUiState(loaded = true, isCouple = true, today = LocalDate.of(2026, 10, 12))

@Composable
private fun PreviewForm(state: MemoEditUiState) {
    DuriseoTheme {
        Column(modifier = Modifier.background(Surface0).padding(horizontal = DuriseoSpacing.Space5, vertical = DuriseoSpacing.Space2)) {
            MemoForm(state = state, onAction = {})
        }
    }
}

/** 기본: 날짜 없음 + [날짜 추가], 알림 스위치 켜짐 (`SCR-MEMO-04` 입력 부분) */
@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun MemoFormPreview_Default() = PreviewForm(PreviewBase)

/** 날짜 추가됨: 날짜 행 + "날짜 빼기" + 안내 */
@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun MemoFormPreview_Date() = PreviewForm(PreviewBase.copy(memoDate = LocalDate.of(2026, 10, 17)))

/** 개인 공간 또는 수정: 알림 스위치 없음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun MemoFormPreview_NoNotify() = PreviewForm(
    PreviewBase.copy(isCouple = false, title = "팀 회식", body = "늦게 끝날 수도 있어요", memoDate = LocalDate.of(2026, 10, 17)),
)

/** 날짜 시트가 열린 모양 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoDateSheetPreview() {
    DuriseoTheme {
        MemoDateSheet(
            state = PreviewBase.copy(dateSheet = MemoDateSheetState(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 12))),
            onAction = {},
        )
    }
}
