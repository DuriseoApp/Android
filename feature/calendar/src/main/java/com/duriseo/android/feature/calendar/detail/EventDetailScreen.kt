package com.duriseo.android.feature.calendar.detail

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDialog
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
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
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface50
import com.duriseo.android.domain.event.EventTarget
import com.duriseo.android.domain.event.ReminderOption
import com.duriseo.android.feature.calendar.R
import com.duriseo.android.feature.calendar.component.color
import com.duriseo.android.feature.calendar.component.formatEventDate
import com.duriseo.android.feature.calendar.component.formatTime12
import com.duriseo.android.feature.calendar.component.label
import com.duriseo.android.feature.calendar.component.reminderLabel
import java.time.LocalDate
import java.time.LocalTime

// SCR-CAL-02 일정 상세 (FR-CAL-03, FR-CAL-05). 수치는 보드 `EventDetail.dc.html`, 메뉴 노출은 PRD 7장 권한 매트릭스.

/** 보드의 안쪽 여백·간격: 좌우 20, 구역 사이 20 */
private val ScreenPadding = DuriseoSpacing.Space5
private val SectionGap = DuriseoSpacing.Space5

/** 누구 칩: 안쪽 5·10, 점 8, 점과 글 6 */
private val ChipDot = 8.dp

/** 정보 줄 높이 52, 라벨 폭 56 (보드 `info`) */
private val InfoRowHeight = 52.dp
private val InfoLabelWidth = 56.dp

/** 알림 칩 높이 40, 안쪽 좌우 16 */
private val ReminderChipHeight = 40.dp

/** 삭제 버튼 높이 52 */
private val DeleteButtonHeight = 52.dp

@Composable
fun EventDetailRoute(
    lifecycleOwner: LifecycleOwner,
    padding: PaddingValues,
    onClose: () -> Unit,
    onOpenEdit: (String) -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.onAction(EventDetailAction.Shown)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EventDetailSideEffect.Close -> onClose()
                is EventDetailSideEffect.OpenEdit -> onOpenEdit(effect.id)
                is EventDetailSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    EventDetailScreen(state = state, onAction = viewModel::onAction, padding = padding, modifier = modifier)
}

@Composable
fun EventDetailScreen(
    state: EventDetailUiState,
    onAction: (EventDetailAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    val event = state.event
    // 삭제 요청 중에는 뒤로 가기로 다이얼로그가 닫히지 않는다
    BackHandler(enabled = state.deleteDialogVisible) { onAction(EventDetailAction.DeleteCancelled) }
    DuriseoDialogLayer(
        dialogVisible = state.deleteDialogVisible,
        dialog = {
            DuriseoDialog(
                title = stringResource(R.string.event_delete_dialog_title),
                body = stringResource(
                    if (state.isPersonalSpace) R.string.event_delete_dialog_body_personal else R.string.event_delete_dialog_body,
                ),
                cancelText = stringResource(DesignR.string.action_cancel),
                confirmText = stringResource(R.string.event_delete_dialog_confirm),
                onCancel = { onAction(EventDetailAction.DeleteCancelled) },
                onConfirm = { onAction(EventDetailAction.DeleteConfirmed) },
                confirmStyle = DuriseoButtonStyle.Destructive,
                buttonsEnabled = !state.deleting,
            )
        },
        modifier = modifier,
        loading = state.loading,
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Surface0).padding(padding)) {
            DetailHeader(
                canEdit = event?.canEdit == true,
                onBack = { onAction(EventDetailAction.BackClicked) },
                onEdit = { onAction(EventDetailAction.EditClicked) },
            )
            if (event != null) {
                DetailBody(event = event, state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun DetailHeader(canEdit: Boolean, onBack: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(horizontal = DuriseoSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(DuriseoSize.ButtonMd)) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_back),
                contentDescription = stringResource(DesignR.string.nav_back),
                modifier = Modifier.size(DuriseoSize.Icon),
                tint = Ink900,
            )
        }
        Text(
            text = stringResource(R.string.event_detail_title),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = Ink900,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        // [수정]은 수정 권한이 있을 때만 보인다 (권한 매트릭스). 없으면 제목을 가운데 두는 빈 자리
        if (canEdit) {
            Box(
                modifier = Modifier
                    .height(DuriseoSize.Touch)
                    .width(DuriseoSize.ButtonMd)
                    .clickable(role = Role.Button, onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.event_detail_edit),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Brand700,
                )
            }
        } else {
            Box(Modifier.width(DuriseoSize.ButtonMd))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailBody(event: EventDetail, state: EventDetailUiState, onAction: (EventDetailAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(bottom = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3)) {
            TargetChip(event.target)
            Text(
                text = event.title,
                // 보드는 24/700이지만 토큰에 24가 없어 title-lg(22)를 쓴다 (unit-4.md "토큰에 없는 값")
                style = MaterialTheme.typography.titleLarge,
                color = Ink900,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .border(1.dp, Line100, MaterialTheme.shapes.large),
        ) {
            InfoRow(stringResource(R.string.event_detail_start), dateText(event.startDate, event.startTime))
            HorizontalDivider(color = Line100)
            InfoRow(stringResource(R.string.event_detail_end), dateText(event.endDate, event.endTime))
            HorizontalDivider(color = Line100)
            InfoRow(stringResource(R.string.event_detail_author), event.authorName)
        }

        if (!event.note.isNullOrEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
                SectionTitle(stringResource(R.string.event_detail_note))
                Text(
                    text = event.note,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface50, MaterialTheme.shapes.large)
                        .padding(DuriseoSpacing.Space4),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink900,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(stringResource(R.string.event_detail_reminder))
                Text(
                    text = stringResource(R.string.event_detail_reminder_hint),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                    color = Ink500,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
                verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
            ) {
                val options = listOf<ReminderOption?>(null) + ReminderOption.optionsFor(event.isAllDay)
                options.forEach { option ->
                    ReminderChip(
                        text = reminderLabel(option),
                        selected = event.myReminder == option,
                        enabled = !state.reminderSaving,
                        onClick = { onAction(EventDetailAction.ReminderSelected(option)) },
                    )
                }
            }
        }

        if (event.canEdit) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DeleteButtonHeight)
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, Line200, MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button) { onAction(EventDetailAction.DeleteClicked) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.event_detail_delete),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Danger700,
                )
            }
        } else {
            // 상대가 만든 "나·상대" 일정은 읽기만 된다 (PRD 7장, FR-CAL-03 AC2)
            Text(
                text = stringResource(R.string.event_detail_read_only),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = Ink900)
}

@Composable
private fun TargetChip(target: EventTarget) {
    Row(
        modifier = Modifier
            .background(Surface0, CircleShape)
            .border(1.dp, Line200, CircleShape)
            .padding(start = DuriseoSpacing.Space3, end = DuriseoSpacing.Space3, top = 5.dp, bottom = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(ChipDot).background(target.color(), CircleShape))
        Text(
            text = target.label(),
            style = MaterialTheme.typography.labelLarge,
            color = Ink900,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(InfoRowHeight)
            .padding(horizontal = DuriseoSpacing.Space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(InfoLabelWidth),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = Ink500,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Ink900)
    }
}

@Composable
private fun ReminderChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    // 보이는 높이 40 그대로, 터치 영역만 44로 키운다
    Box(
        modifier = Modifier
            .heightIn(min = DuriseoSize.Touch)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(ReminderChipHeight)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.background(Ink900) else Modifier.border(1.dp, Line200, CircleShape),
                )
                .padding(horizontal = DuriseoSpacing.Space4),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                color = if (selected) OnBrand else Ink900,
            )
        }
    }
}

/** 종일이면 "10월 17일 (토)", 시간이 있으면 "10월 12일 (월) 오후 7:30" (보드) */
@Composable
private fun dateText(date: LocalDate, time: LocalTime?): String =
    if (time == null) formatEventDate(date) else com.duriseo.android.feature.calendar.component.formatDateTime(date, time)

// --- Preview. 견본 값은 보드 `EventDetail.dc.html`와 같다 ---

private fun sample(
    target: EventTarget = EventTarget.TOGETHER,
    canEdit: Boolean = true,
    allDay: Boolean = false,
    note: String? = "[식당 이름] 예약 완료 · 7시 20분 역 앞에서",
    reminder: ReminderOption? = ReminderOption.MIN_10,
) = EventDetail(
    id = "e1",
    title = "저녁 데이트",
    note = note,
    isAllDay = allDay,
    startDate = LocalDate.of(2026, 10, 12),
    startTime = if (allDay) null else LocalTime.of(19, 30),
    endDate = LocalDate.of(2026, 10, 12),
    endTime = if (allDay) null else LocalTime.of(21, 0),
    target = target,
    authorName = "민지",
    myReminder = reminder,
    canEdit = canEdit,
)

@Composable
private fun PreviewScreen(state: EventDetailUiState) {
    DuriseoTheme { EventDetailScreen(state = state, onAction = {}) }
}

/** `SCR-CAL-02.png`: 시간 일정, 함께 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventDetailPreview() = PreviewScreen(EventDetailUiState(event = sample()))

/** 종일 일정: 알림 선택지가 종일용 (FR-CAL-05 AC3) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventDetailAllDayPreview() = PreviewScreen(EventDetailUiState(event = sample(allDay = true, reminder = null)))

/** 상대가 만든 "상대" 일정: [수정]·[일정 삭제] 없음 (권한 매트릭스) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventDetailReadOnlyPreview() =
    PreviewScreen(EventDetailUiState(event = sample(target = EventTarget.PARTNER, canEdit = false, note = null)))

/** 삭제 확인 다이얼로그 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventDetailDeleteDialogPreview() =
    PreviewScreen(EventDetailUiState(event = sample(), deleteDialogVisible = true))

/** 개인 공간의 삭제 확인 다이얼로그 (D-109) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventDetailDeleteDialogPersonalPreview() =
    PreviewScreen(EventDetailUiState(event = sample(), isPersonalSpace = true, deleteDialogVisible = true))
