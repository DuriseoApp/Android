package com.duriseo.android.feature.couple.linkedsheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoBottomSheetLayer
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDialog
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoLargeButton
import com.duriseo.android.core.designsystem.component.DuriseoSheetPanel
import com.duriseo.android.core.designsystem.component.DuriseoSnackbarHost
import com.duriseo.android.core.designsystem.component.StartDatePickerSheet
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.Brand700
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
import com.duriseo.android.domain.me.ImportCounts
import com.duriseo.android.feature.couple.R
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

// SCR-COUPLE-04 연결 완료 시트 (SCREENS.md, 보드 `Calendar.dc.html` 128~176행). 수치는 보드 값이고
// 토큰에 없는 것은 이름을 붙인 상수로 둔다 (unit-3.md 확인 필요 14).

/** 시트 안 덩어리 사이 22 (131행) */
private val SheetGap = 22.dp

/** 제목: 위 4, 오른쪽 닫기 버튼 44x44, 위 -4·오른쪽 -10 (133~135행) */
private val TitleTop = 4.dp
private val CloseButtonSize = 44.dp
private val CloseButtonTop = (-4).dp
private val CloseButtonEnd = (-10).dp
private val CloseIconSize = 22.dp

/** 섹션 안 간격 8, 개수 줄 사이 2 (139·159행) */
private val SectionGap = 8.dp
private val LabelGap = 2.dp

/** [나중에 하기]: 높이 32, 좌우 4 (143행) */
private val LaterHeight = 32.dp
private val LaterPaddingHorizontal = 4.dp

/** 처음 만난 날 칸: 높이 `size-input` 52, 안쪽 좌우 16, 모서리 `radius-md` 14, 테두리 1, 아이콘 20 (146행) */
private val FieldBorderWidth = 1.dp
private val FieldIconSize = 20.dp

/** 선택 카드: 최소 높이 60, 안쪽 10/16, 모서리 `radius-lg` 16, 테두리 1(선택 2), 줄 사이 2 (318행) */
private val CardMinHeight = 60.dp
private val CardPaddingVertical = 10.dp
private val CardPaddingHorizontal = 16.dp
private val CardBorderWidth = 1.dp
private val CardSelectedBorderWidth = 2.dp

/** 스낵바는 위에서 40 (SCREENS.md에 이 시트의 위치가 없어 시작 화면과 같게 둔다. 디자인 반영 후 맞춤) */
private val SnackbarTop = 40.dp

/**
 * 연결 완료 시트. 호스트(`feature:main`)가 SCR-CAL-01 위에 그리고, 띄울지는 `MeRepository.observeLinkedSheetNeeded()`가 정한다.
 * 기록을 고르기 전에는 닫을 수 없다: 뒤 막·밀기·뒤로 가기가 모두 막힌다 (FR-COUPLE-03 AC4).
 */
@Composable
fun LinkedSheetRoute(
    modifier: Modifier = Modifier,
    viewModel: LinkedSheetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LinkedSheetSideEffect.ShowMessage -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.resources.getString(effect.messageRes),
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    // 뒤로 가기는 늘 이 시트가 받는다. 열린 겹부터 닫고, 기록이 있으면 마지막 겹은 닫지 않는다
    BackHandler(onBack = { viewModel.onAction(LinkedSheetAction.BackPressed) })

    LinkedSheetScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHost = { DuriseoSnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    )
}

@Composable
fun LinkedSheetScreen(
    state: LinkedSheetUiState,
    onAction: (LinkedSheetAction) -> Unit,
    snackbarHost: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        DuriseoDialogLayer(
            dialogVisible = state.dropDialogVisible,
            dialog = { DropDialog(state = state, onAction = onAction) },
        ) {
            DuriseoBottomSheetLayer(
                onDismissRequest = if (state.canDismiss) {
                    { onAction(LinkedSheetAction.CloseClicked) }
                } else {
                    null
                },
            ) {
                SheetPanel(state = state, onAction = onAction)
            }
        }

        // 처음 만난 날 선택 시트는 이 시트 위에 한 겹 더 뜬다 (D-98)
        state.datePicker?.let { picker ->
            DuriseoBottomSheetLayer(onDismissRequest = { onAction(LinkedSheetAction.DatePickerDismissed) }) {
                StartDatePickerSheet(
                    visibleMonth = picker.visibleMonth,
                    selectedDate = picker.selected,
                    today = state.today,
                    onPreviousMonth = { onAction(LinkedSheetAction.PreviousMonth) },
                    onNextMonth = { onAction(LinkedSheetAction.NextMonth) },
                    onDateSelected = { onAction(LinkedSheetAction.DateSelected(it)) },
                    // [저장]은 칸을 채우기만 한다. 날짜를 하나도 안 골랐으면 채울 것이 없다
                    onSave = { onAction(LinkedSheetAction.DateSaved) },
                    saveEnabled = picker.selected != null,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = SnackbarTop),
        ) {
            snackbarHost()
        }
    }
}

@Composable
private fun SheetPanel(state: LinkedSheetUiState, onAction: (LinkedSheetAction) -> Unit) {
    DuriseoSheetPanel(gap = SheetGap) {
        Column(
            modifier = Modifier
                .heightIn(max = SheetContentMaxHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SheetGap),
        ) {
            TitleRow(state = state, onAction = onAction)
            StartDateSection(state = state, onAction = onAction)
            if (state.hasRecords) RecordsSection(state = state, onAction = onAction)
        }
        DuriseoLargeButton(
            text = stringResource(R.string.linked_sheet_start),
            onClick = { onAction(LinkedSheetAction.StartClicked) },
            container = Brand500,
            contentColor = OnBrand,
            enabled = !state.requesting,
        )
    }
}

/** 내용이 화면보다 길어지면 안에서 스크롤한다 (보드에 없는 경우라 화면 높이 844 기준으로 정한 값이 아니라 넉넉한 한계) */
private val SheetContentMaxHeight = 560.dp

@Composable
private fun TitleRow(state: LinkedSheetUiState, onAction: (LinkedSheetAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(R.string.linked_sheet_title, state.partnerName),
            modifier = Modifier
                .weight(1f)
                .padding(top = TitleTop)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = Ink900,
        )
        // 혼자 쓴 기록이 없으면 닫을 수 있다 (FR-COUPLE-03 AC4)
        if (!state.hasRecords) {
            Box(
                modifier = Modifier
                    .offset(x = -CloseButtonEnd, y = CloseButtonTop)
                    .size(CloseButtonSize)
                    .clickable(enabled = !state.requesting, role = Role.Button) {
                        onAction(LinkedSheetAction.CloseClicked)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_close),
                    contentDescription = stringResource(DesignR.string.nav_close),
                    modifier = Modifier.size(CloseIconSize),
                    tint = Ink900,
                )
            }
        }
    }
}

@Composable
private fun StartDateSection(state: LinkedSheetUiState, onAction: (LinkedSheetAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.linked_sheet_date_label),
                style = MaterialTheme.typography.labelLarge,
                color = Ink600,
            )
            // 서버에 이미 날짜가 있으면 없다 (D-98, 지우기 없음 D-84)
            if (state.showLater) {
                Box(
                    modifier = Modifier
                        .height(LaterHeight)
                        .clickable(enabled = !state.requesting, role = Role.Button) {
                            onAction(LinkedSheetAction.LaterClicked)
                        }
                        .padding(horizontal = LaterPaddingHorizontal),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.linked_sheet_date_later),
                        style = MaterialTheme.typography.labelLarge,
                        color = Brand700,
                    )
                }
            }
        }
        DateField(
            date = state.startDate,
            enabled = !state.requesting,
            onClick = { onAction(LinkedSheetAction.DateFieldClicked) },
        )
        Text(
            text = stringResource(R.string.linked_sheet_date_hint),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = Ink600,
        )
    }
}

@Composable
private fun DateField(date: LocalDate?, enabled: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Input)
            .background(Surface0, shape)
            .border(FieldBorderWidth, Line300, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = DuriseoSpacing.Space4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 값이 있으면 입력값 색, 없으면 자리 글자 색 (요청서 4번)
        if (date == null) {
            Text(
                text = stringResource(R.string.linked_sheet_date_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = Ink500,
            )
        } else {
            Text(
                text = stringResource(DesignR.string.date_year_month_day, date.year, date.monthValue, date.dayOfMonth),
                style = MaterialTheme.typography.bodyLarge,
                color = Ink900,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            modifier = Modifier.size(FieldIconSize),
            tint = Ink600,
        )
    }
}

@Composable
private fun RecordsSection(state: LinkedSheetUiState, onAction: (LinkedSheetAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        Column(verticalArrangement = Arrangement.spacedBy(LabelGap)) {
            Text(
                text = stringResource(R.string.linked_sheet_records_label),
                style = MaterialTheme.typography.labelLarge,
                color = Ink600,
            )
            // 개수는 서버가 알려 준다. 0개인 종류도 보여 준다 (D-93, Q-13)
            state.counts?.let { counts ->
                Text(
                    text = stringResource(
                        R.string.linked_sheet_records_counts,
                        counts.events,
                        counts.anniversaries,
                        counts.memos,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink900,
                )
            }
        }
        val groupLabel = stringResource(R.string.linked_sheet_records_group)
        Column(
            modifier = Modifier.semantics { contentDescription = groupLabel },
            verticalArrangement = Arrangement.spacedBy(SectionGap),
        ) {
            ChoiceCard(
                title = stringResource(R.string.linked_sheet_keep),
                hint = stringResource(R.string.linked_sheet_keep_hint),
                selected = state.choice == ImportChoice.Keep,
                enabled = !state.requesting,
                onClick = { onAction(LinkedSheetAction.ChoiceSelected(ImportChoice.Keep)) },
            )
            ChoiceCard(
                title = stringResource(R.string.linked_sheet_drop),
                hint = stringResource(R.string.linked_sheet_drop_hint),
                selected = state.choice == ImportChoice.Drop,
                enabled = !state.requesting,
                onClick = { onAction(LinkedSheetAction.ChoiceSelected(ImportChoice.Drop)) },
            )
        }
        Text(
            text = stringResource(R.string.linked_sheet_records_note),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = Ink600,
        )
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    hint: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val border = if (selected) {
        BorderStroke(CardSelectedBorderWidth, Ink900)
    } else {
        BorderStroke(CardBorderWidth, Line200)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CardMinHeight)
            .background(Surface0, shape)
            .border(border, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = CardPaddingHorizontal, vertical = CardPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(LabelGap, Alignment.CenterVertically),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = Ink900)
        Text(
            text = hint,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = Ink600,
        )
    }
}

@Composable
private fun DropDialog(state: LinkedSheetUiState, onAction: (LinkedSheetAction) -> Unit) {
    val counts = state.counts
    DuriseoDialog(
        title = stringResource(R.string.linked_sheet_drop_dialog_title),
        // 개수는 시트의 개수 안내와 같은 값이다 (D-93). 서버가 개수를 안 줬으면 본문을 비운다
        body = if (counts != null) {
            stringResource(R.string.linked_sheet_drop_dialog_body, counts.events, counts.anniversaries, counts.memos)
        } else {
            ""
        },
        cancelText = stringResource(R.string.linked_sheet_drop_dialog_cancel),
        confirmText = stringResource(R.string.linked_sheet_drop_dialog_confirm),
        onCancel = { onAction(LinkedSheetAction.DropCancelled) },
        onConfirm = { onAction(LinkedSheetAction.DropConfirmed) },
        confirmStyle = DuriseoButtonStyle.Destructive,
        buttonsEnabled = !state.requesting,
    )
}

// Preview의 값은 보드 샘플(개수 [N]에는 임의 샘플 값)이다.

private val PreviewCounts = ImportCounts(events = 3, anniversaries = 0, memos = 2)

@Composable
private fun PreviewLinkedSheet(state: LinkedSheetUiState) {
    DuriseoTheme {
        // 뒤 화면(캘린더) 자리: 시트가 덮는 색 면
        Box(modifier = Modifier.fillMaxSize().background(Surface0)) {
            LinkedSheetScreen(state = state, onAction = {}, snackbarHost = {})
        }
    }
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 31)

/** `SCR-COUPLE-04.png`: 혼자 쓴 기록 있음, [가져오기] 미리 선택 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LinkedSheetWithRecordsPreview() = PreviewLinkedSheet(
    LinkedSheetUiState(partnerName = "지우", hasRecords = true, counts = PreviewCounts, today = PreviewToday),
)

/** `SCR-COUPLE-04--no-records.png`: 기록 영역이 없고 제목 오른쪽에 닫기 버튼 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LinkedSheetNoRecordsPreview() = PreviewLinkedSheet(
    LinkedSheetUiState(partnerName = "지우", today = PreviewToday),
)

/** `SCR-COUPLE-04--drop-dialog.png`: [가져오지 않기]를 골라 [시작하기]를 눌렀다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LinkedSheetDropDialogPreview() = PreviewLinkedSheet(
    LinkedSheetUiState(
        partnerName = "지우",
        hasRecords = true,
        counts = PreviewCounts,
        choice = ImportChoice.Drop,
        dropDialogVisible = true,
        today = PreviewToday,
    ),
)

/** 처음 만난 날을 골랐다 (아직 서버에 저장 전이라 [나중에 하기]가 있다). 이미지 대기 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LinkedSheetDatePickedPreview() = PreviewLinkedSheet(
    LinkedSheetUiState(
        partnerName = "지우",
        startDate = LocalDate.of(2026, 3, 14),
        hasRecords = true,
        counts = PreviewCounts,
        today = PreviewToday,
    ),
)

/**
 * `SCR-COUPLE-04--date-set.png`(이미지 대기, 디자인 반영 후 맞춤): 처음 만난 날이 이미 서버에 있다.
 * 칸에 날짜가 보이고 [나중에 하기]가 없다 (D-98).
 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LinkedSheetDateSetPreview() = PreviewLinkedSheet(
    LinkedSheetUiState(
        partnerName = "지우",
        startDate = LocalDate.of(2026, 3, 14),
        serverStartDate = LocalDate.of(2026, 3, 14),
        hasRecords = true,
        counts = PreviewCounts,
        today = PreviewToday,
    ),
)

/** 처음 만난 날 선택 시트가 위에 한 겹 더 뜬 모습 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LinkedSheetDatePickerPreview() = PreviewLinkedSheet(
    LinkedSheetUiState(
        partnerName = "지우",
        hasRecords = true,
        counts = PreviewCounts,
        datePicker = StartDatePickerState(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 12)),
        today = PreviewToday,
    ),
)

/** 요청 중: [시작하기] 비활성, 문구·스피너 없음 (SCREENS 공통 "요청 중") */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LinkedSheetRequestingPreview() = PreviewLinkedSheet(
    LinkedSheetUiState(
        partnerName = "지우",
        hasRecords = true,
        counts = PreviewCounts,
        requesting = true,
        today = PreviewToday,
    ),
)
