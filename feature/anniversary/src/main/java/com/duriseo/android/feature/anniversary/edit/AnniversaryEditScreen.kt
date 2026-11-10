package com.duriseo.android.feature.anniversary.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.CalendarDateRange
import com.duriseo.android.core.designsystem.component.DuriseoBottomSheetLayer
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDialog
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.StartDatePickerSheet
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.Danger700
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.domain.anniversary.AnniversaryRules
import com.duriseo.android.feature.anniversary.R
import java.time.LocalDate
import java.time.YearMonth

// SCR-ANNIV-02 직접 기념일 작성·수정 (FR-ANNIV-03, D-122). 수치는 보드 `AnniversaryEdit.dc.html`.
// 날짜 시트는 일정과 같은 부품(1950~2050)을 쓴다. 날짜 시트 디자인 교체는 단위 5 묶음 D.

private val ScreenPadding = DuriseoSpacing.Space5
private val ContentTop = DuriseoSpacing.Space2
private val BlockGap = DuriseoSpacing.Space6
private val LabelGap = DuriseoSpacing.Space2

/** 저장 버튼 높이 48, 좌우 14 */
private val SaveWidth = 60.dp

/** 날짜·반복 카드 모서리 16, 줄 최소 높이 56, 아이콘 20 */
private val CardRadius = 16.dp
private val RowMinHeight = 56.dp
private val RowIconSize = 20.dp
private val RowIconGap = DuriseoSpacing.Space3

/** 안내 12/1.6 */
private val GuideLineHeight = 19.2.sp

/** 삭제 버튼 높이 52, 모서리 16 */
private val DeleteHeight = 52.dp

@Composable
fun AnniversaryEditRoute(
    padding: PaddingValues,
    onClose: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnniversaryEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AnniversaryEditSideEffect.Close -> onClose()
                is AnniversaryEditSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    AnniversaryEditScreen(state = state, onAction = viewModel::onAction, padding = padding, modifier = modifier)
}

@Composable
fun AnniversaryEditScreen(
    state: AnniversaryEditUiState,
    onAction: (AnniversaryEditAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    val sheet = state.dateSheet
    BackHandler(enabled = sheet != null) { onAction(AnniversaryEditAction.DateSheetDismissed) }
    DuriseoDialogLayer(
        dialogVisible = sheet != null || state.deleteDialogVisible,
        dialog = {
            if (sheet != null) {
                DuriseoBottomSheetLayer(onDismissRequest = { onAction(AnniversaryEditAction.DateSheetDismissed) }) {
                    StartDatePickerSheet(
                        visibleMonth = sheet.visibleMonth,
                        selectedDate = sheet.selected,
                        today = state.today,
                        onPreviousMonth = { onAction(AnniversaryEditAction.DatePreviousMonth) },
                        onNextMonth = { onAction(AnniversaryEditAction.DateNextMonth) },
                        onDateSelected = { onAction(AnniversaryEditAction.DatePicked(it)) },
                        onSave = { onAction(AnniversaryEditAction.DateConfirmed) },
                        onMonthSelected = { onAction(AnniversaryEditAction.DateMonthSelected(it)) },
                        range = CalendarDateRange,
                        // 새 문구를 만들지 않는다: 날짜 칸의 이름 "날짜"를 제목으로 쓰고 안내 줄은 없다
                        title = stringResource(R.string.anniversary_edit_date_label),
                        description = null,
                        rangeNote = null,
                    )
                }
            } else if (state.deleteDialogVisible) {
                DuriseoDialog(
                    title = stringResource(R.string.anniversary_delete_dialog_title),
                    body = stringResource(R.string.anniversary_delete_dialog_body, state.originalName),
                    cancelText = stringResource(DesignR.string.action_cancel),
                    confirmText = stringResource(R.string.anniversary_delete_dialog_confirm),
                    onCancel = { onAction(AnniversaryEditAction.DeleteDismissed) },
                    onConfirm = { onAction(AnniversaryEditAction.DeleteConfirmed) },
                    confirmStyle = DuriseoButtonStyle.Destructive,
                    buttonsEnabled = !state.saving,
                )
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
private fun EditHeader(state: AnniversaryEditUiState, onAction: (AnniversaryEditAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(horizontal = DuriseoSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onAction(AnniversaryEditAction.CloseClicked) }, modifier = Modifier.size(DuriseoSize.ButtonMd)) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_close),
                contentDescription = stringResource(DesignR.string.nav_close),
                modifier = Modifier.size(DuriseoSize.Icon),
                tint = Ink900,
            )
        }
        Text(
            text = stringResource(if (state.isEdit) R.string.anniversary_edit_title_edit else R.string.anniversary_edit_title_new),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = Ink900,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .height(DuriseoSize.ButtonMd)
                .width(SaveWidth)
                .clickable(enabled = state.saveEnabled && !state.saving, role = Role.Button) {
                    onAction(AnniversaryEditAction.SaveClicked)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.anniversary_edit_save),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                // 비활성 [저장]의 글자색은 문서에 없어 일정 작성과 같이 ink-500을 쓴다 (묶음 D에서 대조)
                color = if (state.saveEnabled) Brand700 else Ink500,
            )
        }
    }
}

@Composable
private fun EditBody(state: AnniversaryEditUiState, onAction: (AnniversaryEditAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = ScreenPadding, end = ScreenPadding, top = ContentTop, bottom = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BlockGap),
    ) {
        NameField(state = state, onAction = onAction)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Line100, RoundedCornerShapeCard),
        ) {
            val dateText = dateFull(state.date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = RowMinHeight)
                    .clickable(enabled = !state.saving, role = Role.Button) { onAction(AnniversaryEditAction.DateRowClicked) }
                    .semantics { contentDescription = dateText }
                    .padding(horizontal = DuriseoSpacing.Space4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RowLabel(iconRes = DesignR.drawable.ic_calendar, text = stringResource(R.string.anniversary_edit_date_label))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink900,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Line100))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = RowMinHeight)
                    .padding(horizontal = DuriseoSpacing.Space4, vertical = DuriseoSpacing.Space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RowLabel(iconRes = DesignR.drawable.ic_repeat, text = stringResource(R.string.anniversary_edit_yearly_label))
                Switch(
                    checked = state.repeatsYearly,
                    onCheckedChange = { onAction(AnniversaryEditAction.YearlyChanged(it)) },
                    enabled = !state.saving,
                )
            }
        }
        Text(
            text = stringResource(if (state.isCouple) R.string.anniversary_edit_guide_couple else R.string.anniversary_edit_guide_solo),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = GuideLineHeight),
            color = Ink600,
        )
        if (state.isEdit) {
            // 삭제 버튼은 화면 아래에 붙인다 (보드 `margin-top: auto`)
            Box(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DeleteHeight)
                    .clip(RoundedCornerShapeCard)
                    .border(1.dp, Line200, RoundedCornerShapeCard)
                    .clickable(enabled = !state.saving, role = Role.Button) { onAction(AnniversaryEditAction.DeleteClicked) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.anniversary_edit_delete),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Danger700,
                )
            }
        }
    }
}

private val RoundedCornerShapeCard = RoundedCornerShape(CardRadius)

@Composable
private fun RowLabel(iconRes: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RowIconGap)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(RowIconSize),
            tint = Ink600,
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Ink600)
    }
}

/** 이름: 라벨 + 글자 수 "9 / 30" + 입력. 30자에서 더 입력되지 않는다 (D-122) */
@Composable
private fun NameField(state: AnniversaryEditUiState, onAction: (AnniversaryEditAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LabelGap)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.anniversary_edit_name_label),
                style = MaterialTheme.typography.labelLarge,
                color = Ink600,
            )
            Text(
                text = stringResource(R.string.anniversary_edit_name_count, state.nameLength, AnniversaryRules.NAME_MAX),
                style = MaterialTheme.typography.labelMedium,
                color = Ink500,
            )
        }
        val shape = MaterialTheme.shapes.medium
        BasicTextField(
            value = state.name,
            onValueChange = { onAction(AnniversaryEditAction.NameChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DuriseoSize.Input)
                .background(Surface0, shape)
                .border(1.dp, Line300, shape),
            enabled = !state.saving,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink900),
            cursorBrush = SolidColor(Ink900),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .heightIn(min = DuriseoSize.Input)
                        .padding(horizontal = DuriseoSpacing.Space4),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (state.name.isEmpty()) {
                        Text(
                            text = stringResource(R.string.anniversary_edit_name_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ink500,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** "2026년 12월 5일 (토)" */
@Composable
private fun dateFull(date: LocalDate): String = stringResource(
    R.string.anniversary_date_full,
    date.year,
    date.monthValue,
    date.dayOfMonth,
    stringArrayResource(DesignR.array.weekday_short)[date.dayOfWeek.value % 7],
)

// --- Preview. 견본 값은 보드와 같다 ---

private val PreviewToday = LocalDate.of(2026, 10, 12)

private fun previewState(
    isEdit: Boolean = false,
    isCouple: Boolean = true,
    name: String = "처음 여행 간 날",
    dialog: Boolean = false,
) = AnniversaryEditUiState(
    loaded = true,
    isEdit = isEdit,
    isCouple = isCouple,
    name = name,
    originalName = name,
    date = LocalDate.of(2026, 12, 5),
    repeatsYearly = true,
    saveEnabled = AnniversaryRules.isNameValid(name),
    deleteDialogVisible = dialog,
    today = PreviewToday,
)

@Composable
private fun PreviewScreen(state: AnniversaryEditUiState) {
    DuriseoTheme { AnniversaryEditScreen(state = state, onAction = {}) }
}

/** `SCR-ANNIV-02.png`: 커플 공간, 새로 추가 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryEditPreview_Couple() = PreviewScreen(previewState())

/** `SCR-ANNIV-02--solo.png`: 개인 공간 안내 문구 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryEditPreview_Solo() = PreviewScreen(previewState(isCouple = false, name = "엄마 생신"))

/** 수정: [기념일 삭제]가 있다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryEditPreview_Edit() = PreviewScreen(previewState(isEdit = true))

/** `SCR-ANNIV-02--delete-dialog.png`: 삭제 확인 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryEditPreview_DeleteDialog() = PreviewScreen(previewState(isEdit = true, dialog = true))

/** 이름이 비어 [저장]이 꺼진 모습 (D-122) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryEditPreview_EmptyName() = PreviewScreen(previewState(name = ""))

/** 날짜 시트가 뜬 모습 (일정과 같은 부품) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryEditPreview_DateSheet() = PreviewScreen(
    previewState().copy(dateSheet = DateSheet(YearMonth.of(2026, 12), LocalDate.of(2026, 12, 5))),
)
