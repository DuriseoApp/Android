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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.duriseo.android.core.designsystem.theme.IbmPlexSansKr
import com.duriseo.android.core.designsystem.theme.Ink700
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

/** 보드의 안쪽 여백·간격: 좌우 20, 구역 사이 20, 아래 28 */
private val ScreenPadding = DuriseoSpacing.Space5
private val SectionGap = DuriseoSpacing.Space5
private val ScreenBottomPadding = 28.dp

/** 누구 칩 점 8, 칩과 제목 사이 10 */
private val ChipDot = 8.dp
private val TitleGap = 10.dp

/** 제목 24/700, 행간 1.35(32.4), 자간 -0.01em: 토큰에 없는 보드 값 (`EventDetail.dc.html` 28행) */
private val DetailTitleStyle = TextStyle(
    fontFamily = IbmPlexSansKr,
    fontSize = 24.sp,
    lineHeight = 32.4.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.01).em,
)

/** 정보 줄: 최소 52(안쪽 위아래 14), 작성 줄 최소 56(안쪽 위아래 12), 아이콘 20, 라벨 폭 52, 값 행간 22 */
private val InfoRowHeight = 52.dp
private val InfoRowPadding = 14.dp
private val AuthorRowHeight = 56.dp
private val AuthorRowPadding = 12.dp
private val InfoIconSize = 20.dp
private val InfoLabelWidth = 52.dp
private val InfoLineHeight = 22.sp

/** 메모 상자 안쪽 위아래 14, 안내 문구 13/1.6 */
private val NotePaddingVertical = 14.dp
private val NoteLineHeight = 20.8.sp

/** 내 알림 줄과 칩 사이 10, 알림 칩 높이 40·안쪽 좌우 14 */
private val ReminderGap = 10.dp
private val ReminderChipHeight = 40.dp
private val ReminderChipPadding = 14.dp

/** 삭제 막대: 버튼 높이 52, 막대 아래 여백 32 */
private val DeleteButtonHeight = 52.dp
private val DeleteBarBottom = 32.dp

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
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = ScreenPadding, end = ScreenPadding, top = DuriseoSpacing.Space1, bottom = ScreenBottomPadding),
            verticalArrangement = Arrangement.spacedBy(SectionGap),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(TitleGap),
                horizontalAlignment = Alignment.Start,
            ) {
                TargetChip(event.target)
                Text(
                    text = event.title,
                    modifier = Modifier.semantics { heading() },
                    // 보드 24/700, 행간 1.35, 자간 -0.01em. 토큰에 24가 없어 보드 값을 이 파일 상수로 둔다 (unit-5.md 결정 28)
                    style = DetailTitleStyle,
                    color = Ink900,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .border(1.dp, Line100, MaterialTheme.shapes.large),
            ) {
                InfoRow(
                    iconRes = DesignR.drawable.ic_calendar,
                    label = stringResource(R.string.event_detail_start),
                    value = dateText(event.startDate, event.startTime),
                    minHeight = InfoRowHeight,
                    vertical = InfoRowPadding,
                )
                HorizontalDivider(color = Line100)
                InfoRow(
                    iconRes = null,
                    label = stringResource(R.string.event_detail_end),
                    value = dateText(event.endDate, event.endTime),
                    minHeight = InfoRowHeight,
                    vertical = InfoRowPadding,
                )
                HorizontalDivider(color = Line100)
                InfoRow(
                    iconRes = DesignR.drawable.ic_user,
                    label = stringResource(R.string.event_detail_author),
                    value = event.authorName,
                    minHeight = AuthorRowHeight,
                    vertical = AuthorRowPadding,
                )
            }

            if (!event.note.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
                    SectionTitle(stringResource(R.string.event_detail_note))
                    Text(
                        text = event.note,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface50, MaterialTheme.shapes.medium)
                            .padding(horizontal = DuriseoSpacing.Space4, vertical = NotePaddingVertical),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink900,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ReminderGap)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(DesignR.drawable.ic_bell),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Ink600,
                        )
                        SectionTitle(stringResource(R.string.event_detail_reminder))
                    }
                    Text(
                        text = stringResource(R.string.event_detail_reminder_hint),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
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

            if (!event.canEdit) {
                // 상대가 만든 "나·상대" 일정은 읽기만 된다 (PRD 7장, FR-CAL-03 AC2)
                Text(
                    text = stringResource(R.string.event_detail_read_only),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = NoteLineHeight,
                    ),
                    color = Ink600,
                )
            }
        }

        if (event.canEdit) {
            // 아래에 붙는 막대: 위 선 1 `line-100`, 안쪽 위 12·좌우 20·아래 32 (보드)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface0)
                    .drawTopLine()
                    .padding(start = ScreenPadding, end = ScreenPadding, top = DuriseoSpacing.Space3, bottom = DeleteBarBottom),
            ) {
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
            }
        }
    }
}

/** 막대 위 선 1 `line-100` */
private fun Modifier.drawTopLine(): Modifier = drawBehind {
    drawLine(color = Line100, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
}

/** 섹션 제목 13/600 `ink-600` (메모·내 알림, 보드 `h3`) */
@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = Ink600)
}

@Composable
private fun TargetChip(target: EventTarget) {
    // 보드: 안쪽 위아래 3·왼 8·오른 10, 점과 글 5, 글 12/600, 테두리 1 `line-200`
    Row(
        modifier = Modifier
            .background(Surface0, CircleShape)
            .border(1.dp, Line200, CircleShape)
            .padding(start = 8.dp, end = DuriseoSpacing.Space2 + 2.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(ChipDot).background(target.color(), CircleShape))
        Text(
            text = target.label(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
    }
}

/** 정보 줄: 아이콘 20 `ink-600`(종료 줄은 빈 자리), 라벨 14 `ink-600` 폭 52, 값 15/600, 줄 사이 12 (보드 `info`) */
@Composable
private fun InfoRow(iconRes: Int?, label: String, value: String, minHeight: Dp, vertical: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(horizontal = DuriseoSpacing.Space4, vertical = vertical),
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
        verticalAlignment = Alignment.Top,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.padding(top = 1.dp).size(InfoIconSize),
                tint = Ink600,
            )
        } else {
            Box(Modifier.size(InfoIconSize))
        }
        Text(
            text = label,
            modifier = Modifier.width(InfoLabelWidth),
            style = MaterialTheme.typography.bodySmall,
            color = Ink600,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = InfoLineHeight),
            color = Ink900,
        )
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
                .padding(horizontal = ReminderChipPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                // 선택 안 한 칩 글자 `ink-700`, 선택은 흰 글자 (보드 `chip`)
                color = if (selected) OnBrand else Ink700,
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
    title: String = "저녁 데이트",
    author: String = "민지",
    date: LocalDate = LocalDate.of(2026, 10, 12),
    start: LocalTime = LocalTime.of(19, 30),
    end: LocalTime = LocalTime.of(21, 0),
) = EventDetail(
    id = "e1",
    title = title,
    note = note,
    isAllDay = allDay,
    startDate = date,
    startTime = if (allDay) null else start,
    endDate = date,
    endTime = if (allDay) null else end,
    target = target,
    authorName = author,
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

/** 개인 공간 견본 (보드 `EventDetail.dc.html`의 `solo`): 대상 "나", 작성 내 닉네임 */
private fun soloSample() = sample(
    target = EventTarget.ME,
    note = "늦게 끝날 수도 있어요",
    reminder = ReminderOption.HOUR_1,
    title = "팀 회식",
    author = "[내 이름]",
    date = LocalDate.of(2026, 10, 9),
    start = LocalTime.of(19, 0),
    end = LocalTime.of(22, 0),
)

/** `SCR-CAL-02--solo.png`: 개인 공간 상세 (D-109) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventDetailPreview_Solo() = PreviewScreen(EventDetailUiState(event = soloSample(), isPersonalSpace = true))

/** `SCR-CAL-02--solo-delete-dialog.png`: 개인 공간 삭제 확인 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventDetailPreview_SoloDeleteDialog() =
    PreviewScreen(EventDetailUiState(event = soloSample(), isPersonalSpace = true, deleteDialogVisible = true))
