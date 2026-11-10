package com.duriseo.android.feature.anniversary.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoBottomSheetLayer
import com.duriseo.android.core.designsystem.component.DuriseoButton
import com.duriseo.android.core.designsystem.component.DuriseoButtonSize
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoTogetherMark
import com.duriseo.android.core.designsystem.component.DuriseoTopAppBar
import com.duriseo.android.core.designsystem.component.StartDatePickerSheet
import com.duriseo.android.core.designsystem.theme.Brand100
import com.duriseo.android.core.designsystem.theme.Brand50
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink400
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink700
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.core.designsystem.theme.Surface50
import com.duriseo.android.feature.anniversary.R
import java.time.LocalDate
import java.time.YearMonth

// SCR-ANNIV-01 기념일 목록 (FR-ANNIV-02, FR-ANNIV-04). 수치는 보드 `Anniversaries.dc.html`, design-system.md.
// 토큰에 없는 보드 수치는 이름을 붙인 상수로 둔다.

private val ScreenPadding = DuriseoSpacing.Space5
private val ContentTop = DuriseoSpacing.Space1
private val ContentBottom = 28.dp

/** 덩어리 사이 24 */
private val BlockGap = DuriseoSpacing.Space6

/** 함께한 날 카드: 안쪽 위아래 22·좌우 20, 모서리 24, 겹친 원 56x38, 원과 글 사이 16 */
private val TogetherPaddingVertical = 22.dp
private val TogetherPaddingHorizontal = DuriseoSpacing.Space5
private val TogetherMarkWidth = 56.dp
private val TogetherMarkHeight = 38.dp
private val TogetherTextGap = 2.dp
private val TogetherDayLineHeight = 32.sp

/** 처음 만난 날 안내 카드: 안쪽 20, 모서리 24, 덩어리 사이 14, 글 사이 4, 설명 13/1.6 */
private val PromptPadding = DuriseoSpacing.Space5
private val PromptGap = 14.dp
private val PromptTextGap = DuriseoSpacing.Space1
private val PromptDescriptionLineHeight = 20.8.sp

/** 목록 줄: 위아래 12, 날짜 블록과 글 사이 14, 아래 1 선 `line-100` */
private val RowPaddingVertical = DuriseoSpacing.Space3
private val RowGap = 14.dp
private val DateBlockWidth = 48.dp
private val DateBlockHeight = 52.dp
private val DateBlockRadius = 14.dp
private val DateDayFontSize = 18.sp
private val DateDayLineHeight = 22.sp
private val MetaGap = 3.dp
private val DotSize = 7.dp
private val DotGap = 6.dp
private val BadgePaddingVertical = DuriseoSpacing.Space1
private val BadgePaddingHorizontal = 10.dp

/** 지난 기념일 행 높이 52, 화살표 20 (보드 `button` 높이) */
private val PastRowHeight = 52.dp
private val PastChevronSize = 20.dp
private val PastItemChevronSize = 18.dp

/** 하단 안내 12/1.6 */
private val FootLineHeight = 19.2.sp

/** 빈 화면: 위 72, 아래 24, 원 72에 아이콘 32, 글 사이 14 */
private val EmptyTop = 72.dp
private val EmptyBottom = DuriseoSpacing.Space6
private val EmptyCircle = 72.dp
private val EmptyIcon = 32.dp
private val EmptyGap = 14.dp

/** 접힘 ⌄ / 펼침 ⌃: 오른쪽 화살표를 돌린 각도 */
private const val CHEVRON_ROTATION = 90f

/** 목록 끝에서 이만큼 남으면 다음 페이지를 이어 붙인다 */
private const val LOAD_MORE_THRESHOLD = 3

@Composable
fun AnniversaryListRoute(
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenEdit: (anniversaryId: String, date: LocalDate) -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnniversaryListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AnniversaryListSideEffect.Back -> onBack()
                AnniversaryListSideEffect.OpenCreate -> onOpenCreate()
                is AnniversaryListSideEffect.OpenEdit -> onOpenEdit(effect.anniversaryId, effect.date)
                is AnniversaryListSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    AnniversaryListScreen(state = state, onAction = viewModel::onAction, padding = padding, modifier = modifier)
}

@Composable
fun AnniversaryListScreen(
    state: AnniversaryListUiState,
    onAction: (AnniversaryListAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    val sheet = state.startDateSheet
    DuriseoDialogLayer(
        dialogVisible = sheet != null,
        dialog = {
            if (sheet != null) {
                // 처음 만난 날 선택 시트 (SCR-SET-01 소속 시트 재사용). 저장 요청 중에는 뒤 막과 뒤로 가기로 닫히지 않는다
                BackHandler(enabled = !sheet.saving) { onAction(AnniversaryListAction.StartDateSheetDismissed) }
                DuriseoBottomSheetLayer(
                    onDismissRequest = if (sheet.saving) null else ({ onAction(AnniversaryListAction.StartDateSheetDismissed) }),
                ) {
                    StartDatePickerSheet(
                        visibleMonth = sheet.visibleMonth,
                        selectedDate = sheet.selected,
                        today = state.today,
                        onPreviousMonth = { onAction(AnniversaryListAction.StartDatePreviousMonth) },
                        onNextMonth = { onAction(AnniversaryListAction.StartDateNextMonth) },
                        onDateSelected = { onAction(AnniversaryListAction.StartDateSelected(it)) },
                        onSave = { onAction(AnniversaryListAction.StartDateSaved) },
                        onMonthSelected = { onAction(AnniversaryListAction.StartDateMonthSelected(it)) },
                        saveEnabled = sheet.selected != null && !sheet.saving,
                    )
                }
            }
        },
        modifier = modifier.background(Surface0),
        loading = state.loading,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DuriseoTopAppBar(
                title = stringResource(R.string.anniversary_list_title),
                onBack = { onAction(AnniversaryListAction.BackClicked) },
                actionIconRes = DesignR.drawable.ic_plus,
                actionDescription = stringResource(R.string.anniversary_add_description),
                onAction = { onAction(AnniversaryListAction.AddClicked) },
            )
            if (state.loaded) ListBody(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun ListBody(state: AnniversaryListUiState, onAction: (AnniversaryListAction) -> Unit) {
    val listState = rememberLazyListState()
    // 끝 가까이까지 스크롤하면 다음 페이지를 이어 붙인다 (지난 기념일이 펼쳐져 있으면 그쪽)
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, state.items.size, state.pastItems.size, state.pastOpen) {
        if (!nearEnd) return@LaunchedEffect
        if (state.pastOpen && state.hasMorePast) {
            onAction(AnniversaryListAction.PastEndReached)
        } else if (state.hasMoreUpcoming) {
            onAction(AnniversaryListAction.UpcomingEndReached)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = ContentTop, bottom = ContentBottom),
    ) {
        state.together?.let { card ->
            item(key = "together") {
                TogetherCard(card = card, modifier = Modifier.padding(bottom = BlockGap))
            }
        }
        if (state.showStartDatePrompt) {
            item(key = "start-prompt") {
                StartDatePrompt(
                    onClick = { onAction(AnniversaryListAction.StartDateInputClicked) },
                    modifier = Modifier.padding(bottom = BlockGap),
                )
            }
        }
        if (state.isEmpty) {
            item(key = "empty") { EmptyState(onAdd = { onAction(AnniversaryListAction.AddClicked) }) }
        } else {
            item(key = "upcoming-header") { UpcomingHeader() }
            items(state.items, key = { it.key }) { item ->
                UpcomingRow(
                    item = item,
                    isCouple = state.isCouple,
                    onClick = { onAction(AnniversaryListAction.ItemClicked(item)) },
                )
            }
        }
        if (state.showPastRow) {
            item(key = "past-row") {
                PastRow(
                    count = state.pastCount,
                    open = state.pastOpen,
                    onClick = { onAction(AnniversaryListAction.PastToggled) },
                    modifier = Modifier.padding(top = BlockGap),
                )
            }
            if (state.pastOpen) {
                items(state.pastItems, key = { "past-" + it.key }) { item ->
                    PastRowItem(
                        item = item,
                        isCouple = state.isCouple,
                        onClick = { onAction(AnniversaryListAction.PastItemClicked(item)) },
                    )
                }
            }
        }
        if (!state.isEmpty) {
            item(key = "foot") {
                Text(
                    text = stringResource(if (state.isCouple) R.string.anniversary_foot_couple else R.string.anniversary_foot_solo),
                    modifier = Modifier.padding(top = BlockGap),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = FootLineHeight),
                    color = Ink600,
                )
            }
        }
    }
}

@Composable
private fun TogetherCard(card: TogetherCardUi, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface100, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = TogetherPaddingHorizontal, vertical = TogetherPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space4),
    ) {
        DuriseoTogetherMark(
            width = TogetherMarkWidth,
            height = TogetherMarkHeight,
            meColorKey = card.meColorKey,
            partnerColorKey = card.partnerColorKey,
            togetherColorKey = card.togetherColorKey,
        )
        Column(verticalArrangement = Arrangement.spacedBy(TogetherTextGap)) {
            Text(
                text = stringResource(R.string.anniversary_together_label),
                style = MaterialTheme.typography.labelLarge,
                color = Ink600,
            )
            Text(
                text = stringResource(R.string.anniversary_together_day, card.dayNumber),
                style = MaterialTheme.typography.headlineMedium.copy(lineHeight = TogetherDayLineHeight),
                color = Brand700,
            )
            Text(
                text = stringResource(R.string.anniversary_together_start_date, dateFull(card.startDate)),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
        }
    }
}

/** 커플 + 처음 만난 날 없음: 함께한 날 카드 자리의 안내 카드 (FR-ANNIV-04 AC4) */
@Composable
private fun StartDatePrompt(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brand50, MaterialTheme.shapes.extraLarge)
            .padding(PromptPadding),
        verticalArrangement = Arrangement.spacedBy(PromptGap),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PromptTextGap)) {
            Text(
                text = stringResource(R.string.anniversary_no_start_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = Ink900,
            )
            Text(
                text = stringResource(R.string.anniversary_no_start_description),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Normal,
                    lineHeight = PromptDescriptionLineHeight,
                ),
                color = Ink600,
            )
        }
        DuriseoButton(
            text = stringResource(R.string.anniversary_no_start_action),
            onClick = onClick,
            style = DuriseoButtonStyle.Brand,
            size = DuriseoButtonSize.Small,
        )
    }
}

@Composable
private fun UpcomingHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.anniversary_upcoming_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = Ink900,
        )
        Text(
            text = stringResource(R.string.anniversary_upcoming_sort),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = Ink500,
        )
    }
}

@Composable
private fun UpcomingRow(item: AnniversaryItemUi, isCouple: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.anniversary_item_description, item.name, item.date.monthValue, item.date.dayOfMonth)
    val editDescription = if (item.editable) stringResource(R.string.anniversary_item_description_edit, description) else description
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 내가 만든 직접 기념일만 누를 수 있다. 자동·상대 것은 반응이 없다 (확인 필요 3)
                .then(
                    if (item.editable) {
                        Modifier.clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = editDescription }
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = RowPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            DateBlock(date = item.date, monthColor = Ink600, dayColor = Ink900)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MetaGap)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Meta(author = item.author, yearly = item.repeatsYearly, isCouple = isCouple, auto = item.isAuto)
            }
            DDayBadge(daysLeft = item.daysLeft, near = item.isNear)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line100))
    }
}

@Composable
private fun DateBlock(date: LocalDate, monthColor: Color, dayColor: Color) {
    Column(
        modifier = Modifier
            .size(width = DateBlockWidth, height = DateBlockHeight)
            .background(Surface50, RoundedCornerShape(DateBlockRadius))
            .clearAndSetSemantics { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.anniversary_date_month, date.monthValue),
            style = MaterialTheme.typography.labelSmall,
            color = monthColor,
        )
        Text(
            text = stringResource(R.string.anniversary_date_day, date.dayOfMonth),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = DateDayFontSize, lineHeight = DateDayLineHeight),
            color = dayColor,
        )
    }
}

/** "자동 기념일" 또는 "내가/상대가 만든 기념일 · 매년" + 사람 색 점. 개인 공간은 "내가 만든 기념일" (색만으로 뜻을 전하지 않는다: 글자가 함께 있다) */
@Composable
private fun Meta(author: AnniversaryAuthor?, yearly: Boolean, isCouple: Boolean, auto: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DotGap)) {
        if (auto || author == null) {
            Text(
                text = stringResource(R.string.anniversary_meta_auto),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = Ink500,
            )
            return@Row
        }
        val colors = DuriseoTheme.personColors
        Box(
            Modifier
                .size(DotSize)
                .background(if (author == AnniversaryAuthor.ME) colors.me else colors.partner, DuriseoShapes.Full),
        )
        val by = stringResource(
            if (author == AnniversaryAuthor.ME || !isCouple) R.string.anniversary_meta_mine else R.string.anniversary_meta_partner,
        )
        Text(
            text = if (yearly) stringResource(R.string.anniversary_meta_yearly, by) else by,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = Ink500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** D-N 배지: 오늘은 "D-day", 30일 이내는 `brand-100`/`brand-700` (FR-ANNIV-04 AC1) */
@Composable
private fun DDayBadge(daysLeft: Int, near: Boolean) {
    Text(
        text = if (daysLeft == 0) stringResource(R.string.anniversary_dday_today) else stringResource(R.string.anniversary_dday, daysLeft),
        modifier = Modifier
            .background(if (near) Brand100 else Surface100, DuriseoShapes.Full)
            .padding(horizontal = BadgePaddingHorizontal, vertical = BadgePaddingVertical),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = if (near) Brand700 else Ink600,
        maxLines = 1,
    )
}

@Composable
private fun PastRow(count: Int, open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.anniversary_past_description, count)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PastRowHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.clearAndSetSemantics { }) {
            Text(
                text = stringResource(R.string.anniversary_past_title) + " ",
                style = MaterialTheme.typography.titleSmall,
                color = Ink900,
            )
            Text(
                text = stringResource(R.string.anniversary_past_count, count),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Ink500,
            )
        }
        // 접힘 ⌄, 펼침 ⌃: 오른쪽 화살표를 돌려 쓴다
        Icon(
            painter = painterResource(DesignR.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier
                .size(PastChevronSize)
                .rotate(if (open) -CHEVRON_ROTATION else CHEVRON_ROTATION),
            tint = Ink600,
        )
    }
}

/** 지난 기념일 한 줄: 날짜(연도 없음), 이름, "내가/상대가 만든 기념일", 일수 배지 없음. 내가 만든 것만 › (D-71) */
@Composable
private fun PastRowItem(item: PastItemUi, isCouple: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.anniversary_item_description, item.name, item.date.monthValue, item.date.dayOfMonth)
    val fullDescription = if (item.editable) stringResource(R.string.anniversary_item_description_edit, description) else description
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (item.editable) {
                        Modifier.clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = fullDescription }
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = RowPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            DateBlock(date = item.date, monthColor = Ink500, dayColor = Ink600)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MetaGap)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = DateDayLineHeight),
                    color = Ink700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Meta(author = item.author, yearly = false, isCouple = isCouple, auto = false)
            }
            if (item.editable) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(PastItemChevronSize),
                    tint = Ink400,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line100))
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = EmptyTop, bottom = EmptyBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EmptyGap),
    ) {
        Box(
            modifier = Modifier.size(EmptyCircle).clip(DuriseoShapes.Full).background(Surface100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_calendar),
                contentDescription = null,
                modifier = Modifier.size(EmptyIcon),
                tint = Ink400,
            )
        }
        Text(
            text = stringResource(R.string.anniversary_empty_title),
            style = MaterialTheme.typography.bodyMedium,
            color = Ink600,
        )
        DuriseoButton(
            text = stringResource(R.string.anniversary_empty_action),
            onClick = onAdd,
            style = DuriseoButtonStyle.Brand,
            size = DuriseoButtonSize.Small,
        )
    }
}

/** "2026년 4월 7일 (화)" */
@Composable
private fun dateFull(date: LocalDate): String = stringResource(
    R.string.anniversary_date_full,
    date.year,
    date.monthValue,
    date.dayOfMonth,
    stringArrayResource(DesignR.array.weekday_short)[date.dayOfWeek.value % 7],
)

// --- Preview. 견본 값은 보드(오늘 2026년 10월 12일, 처음 만난 날 2026년 4월 7일)와 같다 ---

private val PreviewToday = LocalDate.of(2026, 10, 12)
private val PreviewStart = LocalDate.of(2026, 4, 7)

private fun auto(name: String, date: LocalDate, left: Int) = AnniversaryItemUi(
    key = "$date-$name", anniversaryId = null, name = name, date = date, daysLeft = left,
    author = null, repeatsYearly = false, editable = false,
)

private fun custom(name: String, date: LocalDate, left: Int, author: AnniversaryAuthor, yearly: Boolean = true) = AnniversaryItemUi(
    key = "$date-$name", anniversaryId = name, name = name, date = date, daysLeft = left,
    author = author, repeatsYearly = yearly, editable = author == AnniversaryAuthor.ME,
)

private val CoupleItems = listOf(
    auto("200일", LocalDate.of(2026, 10, 23), 11),
    custom("[상대 이름] 생일", LocalDate.of(2026, 11, 20), 39, AnniversaryAuthor.ME),
    custom("처음 여행 간 날", LocalDate.of(2026, 12, 5), 54, AnniversaryAuthor.PARTNER),
    auto("300일", LocalDate.of(2027, 1, 31), 111),
    auto("1주년", LocalDate.of(2027, 4, 7), 177),
)

private val SoloItems = listOf(
    custom("엄마 생신", LocalDate.of(2026, 11, 12), 31, AnniversaryAuthor.ME),
    custom("첫 출근한 날", LocalDate.of(2027, 3, 2), 141, AnniversaryAuthor.ME),
)

private val CouplePast = listOf(
    PastItemUi("p1", "p1", "제주 여행 출발", LocalDate.of(2026, 8, 1), AnniversaryAuthor.PARTNER, editable = false),
    PastItemUi("p2", "p2", "처음 같이 본 영화", LocalDate.of(2026, 6, 14), AnniversaryAuthor.ME, editable = true),
)

private val CoupleCard = TogetherCardUi(189, PreviewStart, "pick-orange", "pick-blue", "pick-purple")

private fun coupleState(
    pastOpen: Boolean = false,
    noStartDate: Boolean = false,
    sheet: StartDateSheetState? = null,
) = AnniversaryListUiState(
    loaded = true,
    isCouple = true,
    together = if (noStartDate) null else CoupleCard,
    items = if (noStartDate) CoupleItems.filter { !it.isAuto } else CoupleItems,
    pastCount = CouplePast.size,
    pastItems = CouplePast,
    pastOpen = pastOpen,
    startDateSheet = sheet,
    today = PreviewToday,
)

@Composable
private fun PreviewScreen(state: AnniversaryListUiState) {
    DuriseoTheme { AnniversaryListScreen(state = state, onAction = {}) }
}

/** `SCR-ANNIV-01.png`: 커플 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryListPreview_Couple() = PreviewScreen(coupleState())

/** `SCR-ANNIV-01--solo.png`: 개인 공간, 함께한 날 카드 없음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryListPreview_Solo() = PreviewScreen(
    AnniversaryListUiState(
        loaded = true,
        items = SoloItems,
        pastCount = 1,
        pastItems = listOf(PastItemUi("s1", "s1", "자격증 시험", LocalDate.of(2026, 7, 11), AnniversaryAuthor.ME, editable = true)),
        today = PreviewToday,
    ),
)

/** `SCR-ANNIV-01--solo-empty.png`: 개인 공간, 비었을 때 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryListPreview_SoloEmpty() = PreviewScreen(AnniversaryListUiState(loaded = true, today = PreviewToday))

/** `SCR-ANNIV-01--past-open.png`: 지난 기념일 펼침 */
@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun AnniversaryListPreview_PastOpen() = PreviewScreen(coupleState(pastOpen = true))

/** `SCR-ANNIV-01--no-start-date.png`: 커플 + 처음 만난 날 없음, 자동 기념일 없음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryListPreview_NoStartDate() = PreviewScreen(coupleState(noStartDate = true))

/** 처음 만난 날 시트가 뜬 모습 (SCR-SET-01 소속 시트 재사용) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AnniversaryListPreview_StartDateSheet() = PreviewScreen(
    coupleState(noStartDate = true, sheet = StartDateSheetState(YearMonth.of(2026, 10), null)),
)
