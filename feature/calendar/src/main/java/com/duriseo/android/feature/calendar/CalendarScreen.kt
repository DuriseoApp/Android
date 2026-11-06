package com.duriseo.android.feature.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoAvatar
import com.duriseo.android.core.designsystem.component.DuriseoAvatarSize
import com.duriseo.android.core.designsystem.component.DuriseoBottomSheetLayer
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.StartDatePickerSheet
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.Danger600
import com.duriseo.android.core.designsystem.theme.DuriseoShadows
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink300
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface50
import com.duriseo.android.core.designsystem.theme.duriseoShadow
import com.duriseo.android.core.designsystem.theme.onPersonColor
import com.duriseo.android.domain.event.EventTarget
import com.duriseo.android.feature.calendar.component.color
import com.duriseo.android.feature.calendar.component.formatEventDate
import com.duriseo.android.feature.calendar.component.formatTime24
import com.duriseo.android.feature.calendar.component.label
import com.duriseo.android.feature.calendar.component.weekdayLabel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// SCR-CAL-01 캘린더 홈 (FR-CAL-01, FR-COUPLE-04 AC2). 수치는 보드 `Calendar.dc.html`, `CalendarExpanded.dc.html`.
// 토큰에 없는 수치는 이름 붙인 상수로 두고 보드 행을 적는다. 하단 탭(80)과 FAB(오른쪽 20, 탭 위 20)는 SCREENS.md 공통 표.
// 알림 버튼·개인 공간 "기념일 ›" 링크·"함께한 지 D+N ›"의 화살표는 가는 화면이 단위 5·7에 있어 그리지 않는다
// (PRD v0.10 반영 대기, unit-4.md 확인 필요 4). 기념일·메모 카드는 단위 5·6.

/** 앱바 높이는 `DuriseoSize.Appbar`(64). 좌 20·우 8 (14행) */
private val AppBarStart = DuriseoSpacing.Space5
private val AppBarEnd = DuriseoSpacing.Space2

/** 아바타 지름 32 + 가장자리 흰 고리 2 (70행 `box-shadow: 0 0 0 2px`), 아바타와 글 사이 12 (16행) */
private val AvatarRing = 2.dp
private val AppBarGap = DuriseoSpacing.Space3

/** 월 제목 줄: 높이 56, 좌 20 우 8 (46행). 월 제목 22/700 (`titleLarge`) */
private val MonthRowHeight = 56.dp

/** 요일 줄: 높이 28, 12/600, 좌우 8 (66행). 달력 좌우 8 */
private val WeekdayRowHeight = 28.dp
private val GridPadding = DuriseoSpacing.Space2

/** 날짜 칸: 접힘 높이 52 / 펼침 88, 날짜 원 34 / 26, 글자 15 / 13 (`Calendar.dc.html` 소스의 `cellCollapsed`·`cellExpanded`, `size`) */
private val CollapsedCellHeight = 52.dp
private val ExpandedCellHeight = 88.dp
private val CollapsedDaySize = 34.dp
private val ExpandedDaySize = 26.dp

/** 사람 색 점 지름 5, 점 사이 3, 점 줄 높이 6 (소스 `dots`). 최대 3개 */
private val DotSize = 5.dp
private val DotGap = 3.dp
private val DotRowHeight = 6.dp

/** 펼친 칸의 일정 막대: 높이 16, 모서리 4, 글자 10/600, 막대 사이 2, 칸에 최대 2개 + "+N" (소스 `barStyle`) */
private val BarHeight = 16.dp
private val BarRadius = 4.dp
private val BarGap = 2.dp
private const val MAX_BARS = 2
private const val MAX_DOTS = 3
private val BarFontSize = 10.sp
private val BarLineHeight = 16.sp
private val MoreLineHeight = 12.sp

/** 손잡이 줄 높이 24, 막대 36x4 (소스 `data-drag` 버튼) */
private val HandleRowHeight = 24.dp
private val HandleWidth = 36.dp
private val HandleHeight = 4.dp

/** 이만큼(dp) 끌면 펼치거나 접는다 (소스 `dragEnd`의 40) */
private const val DRAG_THRESHOLD = 40f

/** 선택한 날 제목 줄: 좌우 20, 아래 10, 제목 17/700, "오늘" 배지 11/600, 개수 13 */
private val DayHeaderBottom = 10.dp
private val TodayBadgePaddingV = 2.dp
private val TodayBadgePaddingH = DuriseoSpacing.Space2

/** 목록: 좌우 20, 접힘 카드 사이 8 / 펼침 행 사이 6, 아래 여백은 FAB(56) 위까지 88 / 80 (소스 `ul`) */
private val ListBottomCollapsed = 88.dp
private val ListBottomExpanded = 80.dp

/** 일정 카드: 안쪽 14·16, 시간 칸 38 + 사이 14, 글자 사이 3 (소스 `cardBase`·`timeBase`) */
private val CardPaddingV = 14.dp
private val CardGap = 14.dp
private val TimeColumnWidth = 38.dp
private val CardTextGap = 3.dp

/** 사람 태그: 안쪽 3·9(점 쪽 7), 점 8, 점과 글 5 (소스) */
private val TagDot = 8.dp
private val TagGap = 5.dp

/** 펼친 행: 높이 44, 모서리 12, 안쪽 좌우 14, 사이 10, 시간 칸 34 (소스 `rowBase`) */
private val RowHeight = 44.dp
private val RowRadius = 12.dp
private val RowTimeWidth = 34.dp

/** FAB 아이콘 26 (`Calendar.dc.html` 133행). 위치: 오른쪽 20, 탭 위 20 */
private val FabIcon = 26.dp
private val FabEnd = DuriseoSpacing.Space5
private val FabBottom = DuriseoSpacing.Space5

/** 스낵바 높이 49 = 위아래 14 + 한 줄 21 (design-system.md "스낵바", `Calendar.dc.html` 364행) */
private val SnackbarHeight = 49.dp

/** 스낵바와 하단 탭 사이 간격 16, 스낵바와 FAB 사이 간격 16 (SCREENS.md 42행) */
private val SnackbarGap = DuriseoSpacing.Space4

/**
 * FAB 아래 간격(탭 위 기준). 스낵바가 없으면 20(바닥에서 100), 있으면 스낵바 위 16:
 * 16 + 49 + 16 = 81 (바닥에서 161, SCREENS.md 42·85행, `Calendar.dc.html` 364행)
 */
internal fun calendarFabBottom(snackbarVisible: Boolean): Dp =
    if (snackbarVisible) SnackbarGap + SnackbarHeight + SnackbarGap else FabBottom

private const val DAYS_PER_WEEK = 7

@Composable
fun CalendarRoute(
    lifecycleOwner: LifecycleOwner,
    padding: PaddingValues,
    onOpenMenu: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenCreate: (LocalDate) -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    snackbarVisible: Boolean = false,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // 화면이 보일 때마다 보이는 달을 다시 불러온다 (NFR-SYNC-02)
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.onAction(CalendarAction.Shown)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CalendarSideEffect.OpenEvent -> onOpenEvent(effect.id)
                is CalendarSideEffect.OpenCreate -> onOpenCreate(effect.date)
                is CalendarSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }

    CalendarScreen(
        state = state,
        padding = padding,
        onOpenMenu = onOpenMenu,
        onAction = viewModel::onAction,
        modifier = modifier,
        snackbarVisible = snackbarVisible,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    padding: PaddingValues,
    onOpenMenu: () -> Unit,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarVisible: Boolean = false,
) {
    val sheet = state.startDateSheet
    DuriseoDialogLayer(
        dialogVisible = sheet != null,
        dialog = {
            if (sheet != null) {
                // 처음 만난 날 선택 시트 (D-74). 저장 요청 중에는 뒤 막과 뒤로 가기로 닫히지 않는다
                BackHandler(enabled = !sheet.saving) { onAction(CalendarAction.StartDateSheetDismissed) }
                DuriseoBottomSheetLayer(
                    onDismissRequest = if (sheet.saving) null else ({ onAction(CalendarAction.StartDateSheetDismissed) }),
                ) {
                    StartDatePickerSheet(
                        visibleMonth = sheet.visibleMonth,
                        selectedDate = sheet.selected,
                        today = state.today,
                        onPreviousMonth = { onAction(CalendarAction.StartDatePreviousMonth) },
                        onNextMonth = { onAction(CalendarAction.StartDateNextMonth) },
                        onDateSelected = { onAction(CalendarAction.StartDateSelected(it)) },
                        onSave = { onAction(CalendarAction.StartDateSaved) },
                        saveEnabled = sheet.selected != null && !sheet.saving,
                        onMonthSelected = { onAction(CalendarAction.StartDateMonthSelected(it)) },
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Surface0)) {
            Column(modifier = Modifier.fillMaxSize()) {
                CalendarAppBar(
                    header = state.header,
                    onOpenMenu = onOpenMenu,
                    onStartDatePromptClick = { onAction(CalendarAction.StartDatePromptClicked) },
                )
                MonthRow(state = state, onAction = onAction)
                WeekdayRow()
                MonthGrid(state = state, onAction = onAction)
                DaySection(state = state, onAction = onAction, modifier = Modifier.weight(1f))
            }
            CalendarFab(
                onClick = { onAction(CalendarAction.AddClicked) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = FabEnd, bottom = calendarFabBottom(snackbarVisible)),
            )
        }
    }
}

// --- 앱바 ---

@Composable
private fun CalendarAppBar(
    header: CalendarHeader,
    onOpenMenu: () -> Unit,
    onStartDatePromptClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(start = AppBarStart, end = AppBarEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppBarGap),
        ) {
            when (header) {
                CalendarHeader.Loading -> Unit
                is CalendarHeader.Solo -> {
                    RingedAvatar(label = stringResource(R.string.calendar_avatar_me), colorKey = header.myColorKey)
                    Text(
                        text = stringResource(R.string.calendar_title_solo),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Ink900,
                    )
                }

                is CalendarHeader.Couple -> {
                    Row {
                        RingedAvatar(label = stringResource(R.string.calendar_avatar_me), colorKey = header.myColorKey)
                        RingedAvatar(label = stringResource(R.string.calendar_avatar_partner), colorKey = header.partnerColorKey)
                    }
                    CoupleTitle(dayNumber = header.dayNumber, onStartDatePromptClick = onStartDatePromptClick)
                }
            }
        }
        IconButton(onClick = onOpenMenu, modifier = Modifier.size(DuriseoSize.ButtonMd)) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_menu),
                contentDescription = stringResource(R.string.calendar_menu_description),
                modifier = Modifier.size(DuriseoSize.Icon),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RingedAvatar(label: String, colorKey: String) {
    Box(modifier = Modifier.border(AvatarRing, Surface0, CircleShape)) {
        DuriseoAvatar(label = label, colorKey = colorKey, size = DuriseoAvatarSize.Small)
    }
}

@Composable
private fun CoupleTitle(dayNumber: Long?, onStartDatePromptClick: () -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.calendar_title_couple),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Ink900,
        )
        if (dayNumber != null) {
            // 처음 만난 날이 있으면 "함께한 지 D+N". 기념일 목록(단위 5)으로 가는 화살표는 그때 더한다
            Text(
                text = stringResource(R.string.calendar_dday, dayNumber),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Brand700,
            )
        } else {
            // 커플 + 처음 만난 날 없음: "처음 만난 날 입력하기 ›" → 시트 (FR-COUPLE-04 AC2)
            Row(
                modifier = Modifier
                    .height(DuriseoSize.Touch)
                    .clickable(role = Role.Button, onClick = onStartDatePromptClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.calendar_start_date_prompt),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Brand700,
                )
                Icon(
                    painter = painterResource(DesignR.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(PromptChevron),
                    tint = Brand700,
                )
            }
        }
    }
}

/** "›" 아이콘 14, 글자와 사이 2 (`Calendar.dc.html` 28행) */
private val PromptChevron = 14.dp

// --- 월 제목 줄 ---

@Composable
private fun MonthRow(state: CalendarUiState, onAction: (CalendarAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MonthRowHeight)
            .padding(start = AppBarStart, end = AppBarEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.calendar_month_title, state.visibleMonth.year, state.visibleMonth.monthValue),
                modifier = Modifier.padding(end = DuriseoSpacing.Space1).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = Ink900,
            )
            MonthButton(
                iconRes = DesignR.drawable.ic_back,
                description = stringResource(R.string.calendar_previous_month),
                enabled = state.canGoPrevious,
                onClick = { onAction(CalendarAction.PreviousMonth) },
            )
            MonthButton(
                iconRes = DesignR.drawable.ic_chevron_right,
                description = stringResource(R.string.calendar_next_month),
                enabled = state.canGoNext,
                onClick = { onAction(CalendarAction.NextMonth) },
            )
        }
        val toggleLabel = stringResource(if (state.expanded) R.string.calendar_collapse else R.string.calendar_expand)
        IconButton(
            onClick = { onAction(CalendarAction.ToggleExpanded) },
            modifier = Modifier.size(DuriseoSize.Touch).semantics { contentDescription = toggleLabel },
        ) {
            // 사각 틀 안의 ⌄(펼치기) / ⌃(접기). 보드는 둥근 사각 + 화살표 선 (`Calendar.dc.html` 54행)
            Icon(
                painter = painterResource(DesignR.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(ToggleIcon).rotate(if (state.expanded) -90f else 90f),
                tint = Ink600,
            )
        }
    }
}

/** 크게 보기 토글 아이콘 22 (54행) */
private val ToggleIcon = 22.dp

@Composable
private fun MonthButton(iconRes: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(DuriseoSize.Touch)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier.size(MonthIcon),
            // 범위 끝에서 꺼진 버튼의 모양은 보드에 없어 `ink-300`(비활성 글자 자리)로 둔다
            tint = if (enabled) Ink600 else Ink300,
        )
    }
}

/** 이전·다음 달 아이콘 20 (48행). 터치 영역은 44로 키웠다 (보드 36) */
private val MonthIcon = 20.dp

// --- 요일 줄, 월 달력 ---

@Composable
private fun WeekdayRow() {
    val names = com.duriseo.android.core.designsystem.R.array.weekday_short
    val weekdays = androidx.compose.ui.res.stringArrayResource(names)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GridPadding)
            .height(WeekdayRowHeight)
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        weekdays.forEachIndexed { index, name ->
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (index == 0) Danger600 else Ink500,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onAction: (CalendarAction) -> Unit) {
    // 끌어서 펼치기·접기 (소스 `dragEnd`): 아래로 40 이상이면 펼치고 위로 40 이상이면 접는다
    val dragged = remember { mutableFloatStateOf(0f) }
    val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { DRAG_THRESHOLD.dp.toPx() }
    val expanded = state.expanded
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GridPadding)
            .pointerInput(expanded) {
                detectVerticalDragGestures(
                    onDragStart = { dragged.floatValue = 0f },
                    onDragEnd = {
                        val dy = dragged.floatValue
                        if (dy > thresholdPx && !expanded) onAction(CalendarAction.ToggleExpanded)
                        if (dy < -thresholdPx && expanded) onAction(CalendarAction.ToggleExpanded)
                        dragged.floatValue = 0f
                    },
                ) { _, delta -> dragged.floatValue += delta }
            },
    ) {
        val first = state.visibleMonth.atDay(1)
        // 일요일 시작: 1일 앞의 이전 달 날짜 수 = 요일 값(월 1 ... 일 7)을 7로 나눈 나머지
        val leading = first.dayOfWeek.value % DAYS_PER_WEEK
        val start = first.minusDays(leading.toLong())
        val weeks = (leading + state.visibleMonth.lengthOfMonth() + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK
        repeat(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(DAYS_PER_WEEK) { col ->
                    val date = start.plusDays((week * DAYS_PER_WEEK + col).toLong())
                    DayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == state.visibleMonth,
                        column = col,
                        state = state,
                        onClick = { onAction(CalendarAction.DateSelected(date)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    column: Int,
    state: CalendarUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = state.expanded
    val events = if (inMonth) state.eventsByDay[date].orEmpty() else emptyList()
    val selected = inMonth && date == state.selectedDate
    val isToday = inMonth && date == state.today
    val circle = if (expanded) ExpandedDaySize else CollapsedDaySize
    val description = stringResource(R.string.calendar_day_description, date.monthValue, date.dayOfMonth)
    // joinToString 람다는 @Composable이 아니라서 문구와 대상 라벨을 먼저 읽어 둔다
    val resources = LocalContext.current.resources
    val targetLabels = mapOf(
        EventTarget.ME to EventTarget.ME.label(),
        EventTarget.PARTNER to EventTarget.PARTNER.label(),
        EventTarget.TOGETHER to EventTarget.TOGETHER.label(),
    )
    val label = if (events.isEmpty()) {
        description
    } else {
        val summary = events.joinToString(", ") {
            resources.getString(R.string.calendar_day_event_summary, targetLabels.getValue(it.target), it.title)
        }
        stringResource(R.string.calendar_day_description_with_events, description, events.size, summary)
    }
    Column(
        modifier = modifier
            .height(if (expanded) ExpandedCellHeight else CollapsedCellHeight)
            .clickable(enabled = inMonth, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(top = DuriseoSpacing.Space1),
        verticalArrangement = Arrangement.spacedBy(if (expanded) DuriseoSpacing.Space1 else DotGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val numberColor = when {
            !inMonth -> Ink300
            selected -> OnBrand
            column == 0 -> Danger600
            else -> Ink900
        }
        Box(
            modifier = Modifier
                .size(circle)
                .then(
                    when {
                        selected -> Modifier.background(Brand500, CircleShape)
                        isToday -> Modifier.border(TodayRing, Ink900, CircleShape)
                        else -> Modifier
                    },
                )
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (expanded) DayNumberExpanded else DayNumberCollapsed,
                    lineHeight = DayNumberLineHeight,
                    fontWeight = if (selected || isToday) FontWeight.Bold else if (inMonth) FontWeight.Medium else FontWeight.Normal,
                ),
                color = numberColor,
            )
        }
        if (inMonth && events.isNotEmpty()) {
            if (expanded) EventBars(events) else EventDots(events)
        }
    }
}

/** 오늘 표시 테두리 1.5 (`box-shadow: inset 0 0 0 1.5px`) */
private val TodayRing = 1.5.dp

/** 날짜 숫자 글자 크기(접힘 15, 펼침 13)와 행간 20 (보드 값) */
private val DayNumberCollapsed = 15.sp
private val DayNumberExpanded = 13.sp
private val DayNumberLineHeight = 20.sp

/** 일정 카드 메모 행간 19.5 (보드 값) */
private val NoteLineHeight = 19.5.sp

@Composable
private fun EventDots(events: List<CalendarEvent>) {
    Row(
        modifier = Modifier.height(DotRowHeight).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(DotGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        events.take(MAX_DOTS).forEach { event ->
            Box(Modifier.size(DotSize).background(event.target.color(), CircleShape))
        }
    }
}

@Composable
private fun EventBars(events: List<CalendarEvent>) {
    Column(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(BarGap),
    ) {
        events.take(MAX_BARS).forEach { event ->
            val color = event.target.color()
            Text(
                text = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .background(color, RoundedCornerShape(BarRadius))
                    .padding(horizontal = BarPadding),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = BarFontSize, lineHeight = BarLineHeight),
                color = onPersonColor(color),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        if (events.size > MAX_BARS) {
            Text(
                text = stringResource(R.string.calendar_bar_more, events.size - MAX_BARS),
                modifier = Modifier.padding(horizontal = BarPadding),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = BarFontSize, lineHeight = MoreLineHeight),
                color = Ink600,
            )
        }
    }
}

/** 막대 안쪽 좌우 3 (소스 `barStyle`) */
private val BarPadding = 3.dp

// --- 선택한 날 ---

@Composable
private fun DaySection(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleLabel = stringResource(if (state.expanded) R.string.calendar_collapse else R.string.calendar_expand)
    val dragged = remember { mutableFloatStateOf(0f) }
    val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { DRAG_THRESHOLD.dp.toPx() }
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = Line100)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HandleRowHeight)
                .clickable(role = Role.Button) { onAction(CalendarAction.ToggleExpanded) }
                .semantics { contentDescription = toggleLabel }
                .pointerInput(state.expanded) {
                    detectVerticalDragGestures(
                        onDragStart = { dragged.floatValue = 0f },
                        onDragEnd = {
                            val dy = dragged.floatValue
                            if (dy > thresholdPx && !state.expanded) onAction(CalendarAction.ToggleExpanded)
                            if (dy < -thresholdPx && state.expanded) onAction(CalendarAction.ToggleExpanded)
                            dragged.floatValue = 0f
                        },
                    ) { _, delta -> dragged.floatValue += delta }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = HandleWidth, height = HandleHeight).background(Line300, RoundedCornerShape(HandleHeight / 2)))
        }
        val events = state.selectedEvents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppBarStart, end = AppBarStart, bottom = DayHeaderBottom),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
            ) {
                Text(
                    text = stringResource(
                        R.string.calendar_selected_day_title,
                        state.selectedDate.monthValue,
                        state.selectedDate.dayOfMonth,
                        weekdayLabel(state.selectedDate),
                    ),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink900,
                )
                if (state.selectedDate == state.today) {
                    Text(
                        text = stringResource(R.string.calendar_today_badge),
                        modifier = Modifier
                            .background(Ink900, CircleShape)
                            .padding(horizontal = TodayBadgePaddingH, vertical = TodayBadgePaddingV),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBrand,
                    )
                }
            }
            if (events.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.calendar_count, events.size),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                    color = Ink500,
                )
            }
        }
        if (events.isEmpty()) {
            EmptyDay(onAddClick = { onAction(CalendarAction.AddClicked) })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = AppBarStart,
                    end = AppBarStart,
                    bottom = if (state.expanded) ListBottomExpanded else ListBottomCollapsed,
                ),
                verticalArrangement = Arrangement.spacedBy(if (state.expanded) CardRowGap else DuriseoSpacing.Space2),
            ) {
                items(events, key = { it.id }) { event ->
                    if (state.expanded) {
                        EventRow(event = event, onClick = { onAction(CalendarAction.EventClicked(event.id)) })
                    } else {
                        EventCard(event = event, onClick = { onAction(CalendarAction.EventClicked(event.id)) })
                    }
                }
            }
        }
    }
}

/** 펼친 행 사이 6 (소스 `ul` gap) */
private val CardRowGap = 6.dp

@Composable
private fun EmptyDay(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = DuriseoSpacing.Space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space1),
    ) {
        Text(
            text = stringResource(R.string.calendar_empty_day),
            style = MaterialTheme.typography.bodySmall,
            color = Ink500,
        )
        Box(
            modifier = Modifier
                .height(DuriseoSize.Touch)
                .clickable(role = Role.Button, onClick = onAddClick)
                .padding(horizontal = DuriseoSpacing.Space3),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.calendar_add_on_day),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Brand700,
            )
        }
    }
}

/** 시간 칸 글: 종일은 "종일", 시간 일정은 "19:30" (보드 `time`) */
@Composable
private fun CalendarEvent.timeText(): String =
    if (isAllDay || startTime == null) stringResource(R.string.calendar_all_day) else formatTime24(startTime)

@Composable
private fun EventCard(event: CalendarEvent, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Surface50)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = DuriseoSpacing.Space4, vertical = CardPaddingV),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = event.timeText(),
            modifier = Modifier.width(TimeColumnWidth),
            style = MaterialTheme.typography.labelLarge,
            color = Ink600,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CardTextGap)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Ink900,
            )
            if (!event.note.isNullOrEmpty()) {
                Text(
                    text = event.note,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal, lineHeight = NoteLineHeight),
                    color = Ink600,
                )
            }
        }
        TargetTag(event.target)
    }
}

@Composable
private fun TargetTag(target: EventTarget) {
    Row(
        modifier = Modifier
            .background(Surface0, CircleShape)
            .border(1.dp, Line200, CircleShape)
            .padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(TagGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(TagDot).background(target.color(), CircleShape))
        Text(
            text = target.label(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
    }
}

@Composable
private fun EventRow(event: CalendarEvent, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(RoundedCornerShape(RowRadius))
            .background(Surface50)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = RowPadding),
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3 - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(TagDot).background(event.target.color(), CircleShape))
        Text(
            text = event.timeText(),
            modifier = Modifier.width(RowTimeWidth),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink600,
            maxLines = 1,
        )
        Text(
            text = event.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = event.target.label(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink600,
        )
    }
}

/** 펼친 행 안쪽 좌우 14 (소스 `rowBase`) */
private val RowPadding = 14.dp

@Composable
private fun CalendarFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(DuriseoSize.Fab)
            .duriseoShadow(DuriseoShapes.Fab, DuriseoShadows.Fab)
            .background(Brand500, DuriseoShapes.Fab)
            .clip(DuriseoShapes.Fab)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_plus),
            contentDescription = stringResource(R.string.calendar_fab_description),
            modifier = Modifier.size(FabIcon),
            tint = OnBrand,
        )
    }
}

// --- Preview. 날짜는 보드(2026년 10월, 오늘 12일)와 같은 견본이다 ---

private val PreviewToday = LocalDate.of(2026, 10, 12)

private fun sampleEvents(): Map<LocalDate, List<CalendarEvent>> = mapOf(
    LocalDate.of(2026, 10, 3) to listOf(CalendarEvent("1", "주말 브런치", null, false, LocalTime.of(11, 0), EventTarget.TOGETHER)),
    LocalDate.of(2026, 10, 9) to listOf(CalendarEvent("2", "팀 회식", "늦게 끝날 수도 있어요", false, LocalTime.of(19, 0), EventTarget.ME)),
    LocalDate.of(2026, 10, 12) to listOf(
        CalendarEvent("3", "치과 예약", null, false, LocalTime.of(14, 0), EventTarget.PARTNER),
        CalendarEvent("4", "저녁 데이트", "[식당 이름] 예약 완료 · 7시 20분 역 앞에서", false, LocalTime.of(19, 30), EventTarget.TOGETHER),
    ),
    LocalDate.of(2026, 10, 17) to listOf(CalendarEvent("5", "1박 2일 여행", "숙소 후보는 메모 핀 참고", true, null, EventTarget.TOGETHER)),
    LocalDate.of(2026, 10, 18) to listOf(CalendarEvent("5", "1박 2일 여행", null, true, null, EventTarget.TOGETHER)),
)

private fun previewState(
    header: CalendarHeader = CalendarHeader.Couple("pick-orange", "pick-blue", 189),
    selected: LocalDate = PreviewToday,
    expanded: Boolean = false,
    events: Map<LocalDate, List<CalendarEvent>> = sampleEvents(),
    startDateSheet: CalendarStartDateSheet? = null,
) = CalendarUiState(
    header = header,
    today = PreviewToday,
    visibleMonth = YearMonth.of(2026, 10),
    selectedDate = selected,
    expanded = expanded,
    eventsByDay = events,
    startDateSheet = startDateSheet,
)

@Composable
private fun PreviewScreen(state: CalendarUiState) {
    DuriseoTheme {
        CalendarScreen(state = state, padding = PaddingValues(), onOpenMenu = {}, onAction = {})
    }
}

/** `SCR-CAL-01.png` */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarScreenPreview() = PreviewScreen(previewState())

/** `SCR-CAL-01--expanded.png`: 날짜 칸에 일정 막대 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarScreenExpandedPreview() = PreviewScreen(previewState(expanded = true))

/** `SCR-CAL-01--solo.png`: 개인 공간, 나 일정만 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarScreenSoloPreview() = PreviewScreen(
    previewState(
        header = CalendarHeader.Solo("pick-orange"),
        events = mapOf(
            PreviewToday to listOf(CalendarEvent("1", "필라테스", null, false, LocalTime.of(7, 30), EventTarget.ME)),
        ),
    ),
)

/** `SCR-CAL-01--no-start-date.png`: 커플 + 처음 만난 날 없음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarScreenNoStartDatePreview() = PreviewScreen(previewState(header = CalendarHeader.Couple("pick-orange", "pick-blue", null)))

/** 선택한 날 비어 있음: 보드 문구 "아직 일정이나 메모가 없어요" (이미지 없음) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarScreenEmptyDayPreview() = PreviewScreen(previewState(selected = LocalDate.of(2026, 10, 14)))

/** 처음 만난 날 선택 시트 (SCR-CAL-01 앱바 "처음 만난 날 입력하기") */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarScreenStartDateSheetPreview() = PreviewScreen(
    previewState(
        header = CalendarHeader.Couple("pick-orange", "pick-blue", null),
        startDateSheet = CalendarStartDateSheet(YearMonth.of(2026, 10), null),
    ),
)

/** 선택한 날의 머리글: 오늘 배지 (빈 일정이 아닌 경우의 개수 표시는 기본 Preview) */
@Preview(showBackground = true, widthDp = 390)
@Composable
private fun CalendarEventCardPreview() {
    DuriseoTheme {
        Column(Modifier.padding(DuriseoSpacing.Space4), verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
            EventCard(CalendarEvent("1", "저녁 데이트", "예약 완료", false, LocalTime.of(19, 30), EventTarget.TOGETHER), {})
            EventRow(CalendarEvent("2", "1박 2일 여행", null, true, null, EventTarget.PARTNER), {})
        }
    }
}
