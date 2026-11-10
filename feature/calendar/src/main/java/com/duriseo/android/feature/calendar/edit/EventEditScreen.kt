package com.duriseo.android.feature.calendar.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.CalendarDateRange
import com.duriseo.android.core.designsystem.component.DuriseoBottomSheetLayer
import com.duriseo.android.core.designsystem.component.DuriseoButton
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoSheetPanel
import com.duriseo.android.core.designsystem.component.StartDatePickerSheet
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.Danger600
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.core.designsystem.theme.Surface50
import com.duriseo.android.domain.event.ReminderOption
import com.duriseo.android.feature.calendar.R
import com.duriseo.android.feature.calendar.component.formatEventDate
import com.duriseo.android.feature.calendar.component.formatTime12
import com.duriseo.android.feature.calendar.component.reminderLabel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// SCR-CAL-03 일정 작성·수정 (FR-CAL-02, FR-CAL-03, FR-NOTI-04). 수치는 보드 `AddEvent.dc.html`, design-system.md "입력".
// 날짜·시간·알림 선택 시트는 디자인이 없어 임시 모양이다: 디자인 반영 후 맞춤(이미지 대기, unit-4.md 확인 필요 1·2).
// 종류 탭 [일정 | 메모]는 메모 화면이 있는 단위 6에서 더한다 (확인 필요 5).

private val ScreenPadding = DuriseoSpacing.Space5
private val SectionGap = DuriseoSpacing.Space5

/** 날짜·시간 칩 높이 36 (보드), 행 높이 52 */
private val ChipHeight = 36.dp
private val RowHeight = 52.dp

/** 메모 입력 최소 높이 (보드 `textarea`) */
private val NoteMinHeight = 96.dp

/** 대상 선택 버튼 높이 48, 선택 테두리 2 */
private val TargetHeight = 48.dp
private val TargetSelectedBorder = 2.dp
private val ErrorBorder = 1.5.dp

@Composable
fun EventEditRoute(
    padding: PaddingValues,
    onClose: () -> Unit,
    onOpenColors: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EventEditSideEffect.Close -> onClose()
                EventEditSideEffect.OpenColors -> onOpenColors()
                is EventEditSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    EventEditScreen(state = state, onAction = viewModel::onAction, padding = padding, modifier = modifier)
}

@Composable
fun EventEditScreen(
    state: EventEditUiState,
    onAction: (EventEditAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    val sheet = state.sheet
    BackHandler(enabled = sheet != null) { onAction(EventEditAction.SheetDismissed) }
    DuriseoDialogLayer(
        dialogVisible = sheet != null,
        dialog = {
            if (sheet != null) {
                DuriseoBottomSheetLayer(onDismissRequest = { onAction(EventEditAction.SheetDismissed) }) {
                    SheetContent(sheet = sheet, state = state, onAction = onAction)
                }
            }
        },
        modifier = modifier,
        loading = state.loading,
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Surface0).padding(padding)) {
            EditHeader(state = state, onAction = onAction)
            if (state.loaded) EditBody(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun EditHeader(state: EventEditUiState, onAction: (EventEditAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(horizontal = DuriseoSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onAction(EventEditAction.CloseClicked) }, modifier = Modifier.size(DuriseoSize.ButtonMd)) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_close),
                contentDescription = stringResource(DesignR.string.nav_close),
                modifier = Modifier.size(DuriseoSize.Icon),
                tint = Ink900,
            )
        }
        Text(
            text = stringResource(if (state.isEdit) R.string.event_edit_title_edit else R.string.event_edit_title_new),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = Ink900,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .height(DuriseoSize.Touch)
                .width(DuriseoSize.ButtonMd)
                .clickable(enabled = state.saveEnabled && !state.saving, role = Role.Button) { onAction(EventEditAction.SaveClicked) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.event_edit_save),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                // 비활성 [저장]의 글자색은 문서에 없어 비활성 버튼 글자와 같은 ink-500을 쓴다
                color = if (state.saveEnabled) Brand700 else Ink500,
            )
        }
    }
}

@Composable
private fun EditBody(state: EventEditUiState, onAction: (EventEditAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(bottom = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        LabeledField(label = stringResource(R.string.event_edit_title_label)) {
            TextInput(
                value = state.title,
                onValueChange = { onAction(EventEditAction.TitleChanged(it)) },
                placeholder = stringResource(R.string.event_edit_title_placeholder),
                singleLine = true,
                enabled = !state.saving,
            )
        }

        if (state.isCouple) TargetSection(state = state, onAction = onAction)

        Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
            SwitchRow(
                label = stringResource(R.string.event_edit_all_day),
                checked = state.isAllDay,
                onCheckedChange = { onAction(EventEditAction.AllDayChanged(it)) },
            )
            DateTimeRow(
                label = stringResource(R.string.event_edit_start),
                date = state.startDate,
                time = state.startTime,
                allDay = state.isAllDay,
                error = false,
                dateDescription = R.string.event_edit_start_date_description,
                timeDescription = R.string.event_edit_start_time_description,
                onDateClick = { onAction(EventEditAction.DateRowClicked(EditField.Start)) },
                onTimeClick = { onAction(EventEditAction.TimeRowClicked(EditField.Start)) },
            )
            DateTimeRow(
                label = stringResource(R.string.event_edit_end),
                date = state.endDate,
                time = state.endTime,
                allDay = state.isAllDay,
                error = state.endError,
                dateDescription = R.string.event_edit_end_date_description,
                timeDescription = R.string.event_edit_end_time_description,
                onDateClick = { onAction(EventEditAction.DateRowClicked(EditField.End)) },
                onTimeClick = { onAction(EventEditAction.TimeRowClicked(EditField.End)) },
            )
            if (state.endError) {
                Text(
                    text = stringResource(R.string.event_edit_end_error),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                    color = Danger600,
                )
            }
        }

        ReminderRow(reminder = state, onClick = { onAction(EventEditAction.ReminderRowClicked) })

        LabeledField(label = stringResource(R.string.event_edit_note_label)) {
            TextInput(
                value = state.note,
                onValueChange = { onAction(EventEditAction.NoteChanged(it)) },
                placeholder = stringResource(R.string.event_edit_note_placeholder),
                singleLine = false,
                enabled = !state.saving,
                modifier = Modifier.heightIn(min = NoteMinHeight),
            )
        }

        // 커플 공간에서 새로 만들 때만. 수정(상대가 수정 포함)·개인 공간에는 스위치도 값도 없다 (FR-NOTI-04)
        if (state.isCouple && !state.isEdit) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface50, MaterialTheme.shapes.large)
                    .padding(DuriseoSpacing.Space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.event_edit_notify_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Ink900,
                    )
                    Text(
                        text = stringResource(R.string.event_edit_notify_description),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                        color = Ink600,
                    )
                }
                Switch(
                    checked = state.notifyPartner,
                    onCheckedChange = { onAction(EventEditAction.NotifyPartnerChanged(it)) },
                )
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Ink900)
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
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink900),
        cursorBrush = SolidColor(Ink900),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.padding(horizontal = DuriseoSpacing.Space4, vertical = DuriseoSpacing.Space3),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = MaterialTheme.typography.bodyLarge, color = Ink500)
                }
                inner()
            }
        },
    )
}

@Composable
private fun TargetSection(state: EventEditUiState, onAction: (EventEditAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
        if (state.targetReadOnly) {
            // 상대가 만든 "함께" 일정: 읽기 전용 "누구의 일정 · 함께" (FR-CAL-03 AC4, D-77)
            Text(
                text = stringResource(R.string.event_edit_who_readonly_label) + " · " + stringResource(R.string.event_target_together),
                style = MaterialTheme.typography.labelLarge,
                color = Ink900,
            )
            Text(
                text = stringResource(R.string.event_edit_who_readonly_note),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.event_edit_who_question),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = Ink900,
            )
            Box(
                modifier = Modifier
                    .height(DuriseoSize.Touch)
                    .clickable(role = Role.Button) { onAction(EventEditAction.ColorChangeClicked) }
                    .padding(horizontal = DuriseoSpacing.Space2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.event_edit_color_change),
                    style = MaterialTheme.typography.labelLarge,
                    color = Brand700,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
            TargetButton(
                text = stringResource(R.string.event_target_me),
                selected = !state.together,
                onClick = { onAction(EventEditAction.TogetherSelected(false)) },
                modifier = Modifier.weight(1f),
            )
            TargetButton(
                text = stringResource(R.string.event_target_together),
                selected = state.together,
                onClick = { onAction(EventEditAction.TogetherSelected(true)) },
                modifier = Modifier.weight(1f),
            )
        }
        if (state.together) {
            Text(
                text = stringResource(R.string.event_edit_together_note),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
        }
    }
}

@Composable
private fun TargetButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = modifier
            .height(TargetHeight)
            .clip(shape)
            .border(if (selected) TargetSelectedBorder else 1.dp, if (selected) Ink900 else Line200, shape)
            .clickable(role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Ink900,
            )
            Box(Modifier.width(DuriseoSpacing.Space1))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = Ink900,
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(RowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    date: LocalDate,
    time: LocalTime,
    allDay: Boolean,
    error: Boolean,
    dateDescription: Int,
    timeDescription: Int,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(RowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
        val dateText = formatEventDate(date)
        // 종일에서는 시간 칩이 없어 종료 날짜 칩이 오류를 대신 보여 준다
        ValueChip(
            text = dateText,
            description = stringResource(dateDescription, dateText),
            error = error && allDay,
            onClick = onDateClick,
        )
        if (!allDay) {
            val timeText = formatTime12(time)
            ValueChip(
                text = timeText,
                description = stringResource(timeDescription, timeText),
                error = error,
                onClick = onTimeClick,
            )
        }
    }
}

@Composable
private fun ValueChip(text: String, description: String, error: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.small
    // 보이는 높이는 36 그대로, 터치 영역만 44로 키운다
    Box(
        modifier = Modifier
            .heightIn(min = DuriseoSize.Touch)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(ChipHeight)
                .clip(shape)
                .background(Surface100)
                .then(if (error) Modifier.border(ErrorBorder, Danger600, shape) else Modifier)
                .padding(horizontal = DuriseoSpacing.Space3),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (error) Danger600 else Ink900,
            )
        }
    }
}

@Composable
private fun ReminderRow(reminder: EventEditUiState, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_bell),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Ink600,
        )
        Text(
            text = stringResource(R.string.event_edit_reminder),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
        Text(
            text = reminderLabel(reminder.reminder),
            style = MaterialTheme.typography.bodySmall,
            color = Ink600,
        )
        Icon(
            painter = painterResource(DesignR.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Ink500,
        )
    }
}

// --- 선택 시트 (디자인 반영 후 맞춤: 이미지 대기) ---

@Composable
private fun SheetContent(sheet: EditSheet, state: EventEditUiState, onAction: (EventEditAction) -> Unit) {
    when (sheet) {
        is EditSheet.DatePicker -> StartDatePickerSheet(
            visibleMonth = sheet.visibleMonth,
            selectedDate = sheet.selected,
            today = state.today,
            onPreviousMonth = { onAction(EventEditAction.DatePreviousMonth) },
            onNextMonth = { onAction(EventEditAction.DateNextMonth) },
            onDateSelected = { onAction(EventEditAction.DatePicked(it)) },
            onSave = { onAction(EventEditAction.DateConfirmed) },
            onMonthSelected = { onAction(EventEditAction.DateMonthSelected(it)) },
            range = CalendarDateRange,
            title = stringResource(if (sheet.field == EditField.Start) R.string.event_edit_start else R.string.event_edit_end),
            // 일정용 안내 문구는 문서에 없어 넣지 않는다 (unit-4.md 확인 필요 1-A)
            description = null,
            rangeNote = null,
        )
        is EditSheet.TimePicker -> TimeSheet(initial = sheet.initial, onConfirm = { onAction(EventEditAction.TimeConfirmed(it)) })
        EditSheet.Reminder -> ReminderSheet(
            isAllDay = state.isAllDay,
            selected = state.reminder,
            onSelect = { onAction(EventEditAction.ReminderSelected(it)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSheet(initial: LocalTime, onConfirm: (LocalTime) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    DuriseoSheetPanel {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimePicker(state = pickerState)
        }
        DuriseoButton(
            text = stringResource(R.string.event_edit_save),
            onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) },
            modifier = Modifier.fillMaxWidth(),
            style = DuriseoButtonStyle.Brand,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSheet(isAllDay: Boolean, selected: ReminderOption?, onSelect: (ReminderOption?) -> Unit) {
    DuriseoSheetPanel {
        Text(
            text = stringResource(R.string.event_edit_reminder),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = Ink900,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
            verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
        ) {
            (listOf<ReminderOption?>(null) + ReminderOption.optionsFor(isAllDay)).forEach { option ->
                val isSelected = option == selected
                // 보이는 높이 40 그대로, 터치 영역만 44로 키운다
                Box(
                    modifier = Modifier
                        .heightIn(min = DuriseoSize.Touch)
                        .clickable(role = Role.RadioButton) { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .then(if (isSelected) Modifier.background(Ink900) else Modifier.border(1.dp, Line200, CircleShape))
                            .padding(horizontal = DuriseoSpacing.Space4),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = reminderLabel(option),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (isSelected) OnBrand else Ink900,
                        )
                    }
                }
            }
        }
    }
}

// --- Preview. 견본 값은 보드와 같다 ---

private val PreviewDay = LocalDate.of(2026, 10, 12)

private fun previewState(
    isEdit: Boolean = false,
    isCouple: Boolean = true,
    targetReadOnly: Boolean = false,
    allDay: Boolean = false,
    title: String = "저녁 데이트",
    endError: Boolean = false,
    sheet: EditSheet? = null,
) = EventEditUiState(
    loaded = true,
    isEdit = isEdit,
    isCouple = isCouple,
    targetReadOnly = targetReadOnly,
    title = title,
    isAllDay = allDay,
    startDate = PreviewDay,
    startTime = LocalTime.of(19, 30),
    endDate = PreviewDay,
    endTime = if (endError) LocalTime.of(18, 0) else LocalTime.of(21, 30),
    together = true,
    endError = endError,
    saveEnabled = title.isNotBlank() && !endError,
    sheet = sheet,
    today = PreviewDay,
)

@Composable
private fun PreviewScreen(state: EventEditUiState) {
    DuriseoTheme { EventEditScreen(state = state, onAction = {}) }
}

/** `SCR-CAL-03.png`: 커플 공간, 새로 추가 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditPreview() = PreviewScreen(previewState())

/** `SCR-CAL-03--solo.png`: 개인 공간, 대상 선택과 알림 스위치 없음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditSoloPreview() = PreviewScreen(previewState(isCouple = false))

/** `SCR-CAL-03--all-day.png`: 하루 종일, 시간 칩 없음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditAllDayPreview() = PreviewScreen(previewState(allDay = true))

/** `SCR-CAL-03--edit.png`: 내 일정 수정, 알림 스위치 없음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditEditPreview() = PreviewScreen(previewState(isEdit = true))

/** `SCR-CAL-03--partner-edit.png`: 상대가 만든 함께 일정, 대상 읽기 전용 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditPartnerEditPreview() = PreviewScreen(previewState(isEdit = true, targetReadOnly = true))

/** `SCR-CAL-03--error.png`: 종료가 시작보다 이른 모습 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditErrorPreview() = PreviewScreen(previewState(endError = true))

/** 제목이 비어 [저장]이 꺼진 모습 (FR-CAL-02 AC1) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditEmptyTitlePreview() = PreviewScreen(previewState(title = ""))

/** 디자인 반영 후 맞춤(이미지 대기): 날짜 선택 시트 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditDateSheetPreview() =
    PreviewScreen(previewState(sheet = EditSheet.DatePicker(EditField.Start, YearMonth.of(2026, 10), PreviewDay)))

/** 디자인 반영 후 맞춤(이미지 대기): 시간 선택 시트 (Material3 TimePicker 임시) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditTimeSheetPreview() =
    PreviewScreen(previewState(sheet = EditSheet.TimePicker(EditField.Start, LocalTime.of(19, 30))))

/** 디자인 반영 후 맞춤(이미지 대기): 내 알림 선택 시트 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditReminderSheetPreview() = PreviewScreen(previewState(sheet = EditSheet.Reminder))
