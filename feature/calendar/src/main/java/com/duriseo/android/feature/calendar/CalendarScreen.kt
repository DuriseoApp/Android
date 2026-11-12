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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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
import com.duriseo.android.core.designsystem.theme.Brand50
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
// 알림 버튼은 가는 화면이 단위 7에 있어 그리지 않는다.
// 메모(단위 6): 날짜 목록 맨 아래 "메모" 카드 → SCR-MEMO-03, 달력의 메모 점은 작성자 색 (D-118·D-119).
// 기념일(단위 5): 앱바 "함께한 지 D+N ›"·"기념일 ›" → SCR-ANNIV-01, 기념일 카드, 달력의 `brand-500` 고리·막대 (D-65·D-118·D-119).

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

/** 사람 색 점 지름 5, 점 사이 3, 점 줄 높이 6 (소스 `dots`). 고리를 포함해 최대 3개 (`MAX_DAY_MARKS`) */
private val DotSize = 5.dp
private val DotGap = 3.dp
private val DotRowHeight = 6.dp

/** 기념일 고리: 지름 6, 선 1.5, `brand-500` (소스 `ringStyle`, design-system.md "날짜 칸 점 규칙") */
private val RingSize = 6.dp
private val RingStroke = 1.5.dp

/** 기념일 막대의 `brand-500` 테두리 1 (소스 `annBarStyle`의 `inset 0 0 0 1px`) */
private val AnnBarBorder = 1.dp

/** 펼친 칸의 일정 막대: 높이 16, 모서리 4, 글자 10/600, 막대 사이 2, 칸에 최대 2개 + "+N" (소스 `barStyle`) */
private val BarHeight = 16.dp
private val BarRadius = 4.dp
private val BarGap = 2.dp
private const val MAX_BARS = 2
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
    onOpenMemo: (String) -> Unit,
    onOpenCreate: (LocalDate) -> Unit,
    onOpenAnniversaries: () -> Unit,
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
                is CalendarSideEffect.OpenMemo -> onOpenMemo(effect.id)
                is CalendarSideEffect.OpenCreate -> onOpenCreate(effect.date)
                CalendarSideEffect.OpenAnniversaries -> onOpenAnniversaries()
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
        loading = state.loading,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Surface0)) {
            Column(modifier = Modifier.fillMaxSize()) {
                CalendarAppBar(
                    header = state.header,
                    onOpenMenu = onOpenMenu,
                    onAnniversariesClick = { onAction(CalendarAction.AnniversariesClicked) },
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
    onAnniversariesClick: () -> Unit,
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
                    // 개인 공간: "나의 캘린더" + "기념일 ›" → SCR-ANNIV-01 (FR-CAL-01 AC5, D-54)
                    val description = stringResource(R.string.calendar_solo_title_description)
                    AppBarLink(
                        title = stringResource(R.string.calendar_title_solo),
                        link = stringResource(R.string.calendar_anniversary_link),
                        description = description,
                        onClick = onAnniversariesClick,
                    )
                }

                is CalendarHeader.Couple -> {
                    Row {
                        RingedAvatar(label = stringResource(R.string.calendar_avatar_me), colorKey = header.myColorKey)
                        RingedAvatar(label = stringResource(R.string.calendar_avatar_partner), colorKey = header.partnerColorKey)
                    }
                    CoupleTitle(
                        dayNumber = header.dayNumber,
                        onAnniversariesClick = onAnniversariesClick,
                        onStartDatePromptClick = onStartDatePromptClick,
                    )
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
private fun CoupleTitle(dayNumber: Long?, onAnniversariesClick: () -> Unit, onStartDatePromptClick: () -> Unit) {
    if (dayNumber != null) {
        // 처음 만난 날이 있으면 "함께한 지 D+N ›" → SCR-ANNIV-01. N은 최소 1이다 (D-116)
        AppBarLink(
            title = stringResource(R.string.calendar_title_couple),
            link = stringResource(R.string.calendar_dday, dayNumber),
            description = stringResource(R.string.calendar_dday_description, dayNumber),
            onClick = onAnniversariesClick,
        )
        return
    }
    Column {
        Text(
            text = stringResource(R.string.calendar_title_couple),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Ink900,
        )
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

/**
 * 앱바의 제목 + 링크 줄 "[link] ›". 두 줄 전체가 하나의 버튼이고 SCR-ANNIV-01로 간다 (보드 `<a>`가 두 줄을 감싼다, 28·37행).
 * 높이는 터치 영역 44 이상으로 둔다.
 */
@Composable
private fun AppBarLink(title: String, link: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(min = DuriseoSize.Touch)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                heading()
            },
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Ink900,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(LinkGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = link,
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

/** 링크 글자와 "›" 사이 2 (`Calendar.dc.html` 28행 `gap: 2px`) */
private val LinkGap = 2.dp

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
    val anniversaries = if (inMonth) state.anniversariesByDay[date].orEmpty() else emptyList()
    val memos = if (inMonth) state.memosByDay[date].orEmpty() else emptyList()
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
    // "10월 12일, 기념일 200일, 일정·메모 2개: 상대 치과 예약, 함께 저녁 데이트" (보드 318행)
    val label = buildString {
        append(description)
        if (anniversaries.isNotEmpty()) {
            append(resources.getString(R.string.calendar_day_anniversary_part, anniversaries.joinToString(", ") { it.name }))
        }
        if (events.isNotEmpty() || memos.isNotEmpty()) {
            // 일정 다음에 메모 (보드 318행: 일정·메모를 한 목록으로 읽는다)
            val summary = (
                events.map { targetLabels.getValue(it.target) to it.title } +
                    memos.map { targetLabels.getValue(it.author) to it.title }
                ).joinToString(", ") { (who, title) -> resources.getString(R.string.calendar_day_event_summary, who, title) }
            append(resources.getString(R.string.calendar_day_events_part, events.size + memos.size, summary))
        }
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
        if (inMonth && (events.isNotEmpty() || anniversaries.isNotEmpty() || memos.isNotEmpty())) {
            if (expanded) {
                // 펼친 칸의 막대는 기념일·일정만이다: 메모 막대는 문서에 없다 (unit-6.md 결정 17)
                DayBars(anniversaries, events)
            } else {
                DayMarks(calendarDayMarks(hasAnniversary = anniversaries.isNotEmpty(), events = events, memos = memos))
            }
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

/** 접힌 칸의 표시: 기념일 고리 하나 + 사람 색 점, 합쳐서 최대 3개 (D-65·D-118). 규칙은 [calendarDayMarks] */
@Composable
private fun DayMarks(marks: CalendarDayMarks) {
    Row(
        modifier = Modifier.height(DotRowHeight).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(DotGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (marks.hasAnniversary) {
            Box(Modifier.size(RingSize).border(RingStroke, Brand500, CircleShape))
        }
        marks.dots.forEach { target ->
            Box(Modifier.size(DotSize).background(target.color(), CircleShape))
        }
    }
}

/**
 * 펼친 칸의 막대: 기념일 막대(`brand-50` 면 + `brand-500` 테두리) 먼저, 그다음 일정. 최대 2개 + "+N" (소스 `bars`·`hasMore`).
 * 막대 안 글자는 기념일이 `brand-700` 700, 일정은 사람 색 위 글자 (`annBarStyle`·`barStyle`)
 */
@Composable
private fun DayBars(anniversaries: List<CalendarAnniversary>, events: List<CalendarEvent>) {
    val total = anniversaries.size + events.size
    Column(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(BarGap),
    ) {
        val shape = RoundedCornerShape(BarRadius)
        val barStyle = MaterialTheme.typography.labelSmall.copy(fontSize = BarFontSize, lineHeight = BarLineHeight)
        anniversaries.take(MAX_BARS).forEach { anniversary ->
            Text(
                text = anniversary.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .background(Brand50, shape)
                    .border(AnnBarBorder, Brand500, shape)
                    .padding(horizontal = BarPadding),
                style = barStyle.copy(fontWeight = FontWeight.Bold),
                color = Brand700,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        events.take(MAX_BARS - minOf(anniversaries.size, MAX_BARS)).forEach { event ->
            val color = event.target.color()
            Text(
                text = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .background(color, shape)
                    .padding(horizontal = BarPadding),
                style = barStyle,
                color = onPersonColor(color),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        if (total > MAX_BARS) {
            Text(
                text = stringResource(R.string.calendar_bar_more, total - MAX_BARS),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySection(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullState = rememberPullToRefreshState()
    // 당겨서 새로고침 (Q-16): 날짜 목록을 당기면 목록 영역 위에서 64에 원판이 뜬다 (design-system.md "당겨서 새로고침")
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { onAction(CalendarAction.Refresh) },
        modifier = modifier.fillMaxWidth(),
        state = pullState,
        indicator = {
            RefreshIndicator(
                refreshing = state.refreshing,
                fraction = pullState.distanceFraction,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        DayContent(state = state, onAction = onAction)
    }
}

/** 당겨서 새로고침 표시: 원판 지름 44(`size-touch`, `surface-0`, `shadow-sm`) + 원형 표시 24(`size-icon`) / 선 2 (보드 131행) */
private val RefreshTop = 64.dp
private val RefreshStroke = 2.dp

@Composable
private fun RefreshIndicator(refreshing: Boolean, fraction: Float, modifier: Modifier = Modifier) {
    if (!refreshing && fraction <= 0f) return
    // 당기는 동안의 모양은 보드에 없어서, 끄는 만큼 나타나게만 한다 (새로고침 중에는 보드 그대로)
    val alpha = if (refreshing) 1f else fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .padding(top = RefreshTop)
            .graphicsLayer { this.alpha = alpha }
            .size(DuriseoSize.Touch)
            .duriseoShadow(CircleShape, DuriseoShadows.Sm)
            .background(Surface0, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(DuriseoSize.Icon),
            color = Brand500,
            strokeWidth = RefreshStroke,
            trackColor = Line100,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun DayContent(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleLabel = stringResource(if (state.expanded) R.string.calendar_collapse else R.string.calendar_expand)
    val dragged = remember { mutableFloatStateOf(0f) }
    val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { DRAG_THRESHOLD.dp.toPx() }
    Column(modifier = modifier.fillMaxSize()) {
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
        val listItems = state.selectedItems
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
            if (listItems.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.calendar_count, listItems.size),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                    color = Ink500,
                )
            }
        }
        if (listItems.isEmpty()) {
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
                items(listItems, key = { it.key }) { item ->
                    when (item) {
                        is CalendarListItem.Anniversary -> {
                            val onClick = { onAction(CalendarAction.AnniversariesClicked) }
                            if (state.expanded) {
                                AnniversaryRow(anniversary = item.value, onClick = onClick)
                            } else {
                                AnniversaryCard(anniversary = item.value, onClick = onClick)
                            }
                        }

                        is CalendarListItem.Memo -> {
                            val memo = item.value
                            val onClick = { onAction(CalendarAction.MemoClicked(memo.id)) }
                            if (state.expanded) MemoRow(memo = memo, onClick = onClick) else MemoCard(memo = memo, onClick = onClick)
                        }

                        is CalendarListItem.Event -> {
                            val event = item.value
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
    }
}

/** 목록 키: 기념일은 그날 안의 위치, 일정은 id */
private val CalendarListItem.key: String
    get() = when (this) {
        is CalendarListItem.Anniversary -> "anniversary-$index"
        is CalendarListItem.Event -> "event-${value.id}"
        is CalendarListItem.Memo -> "memo-${value.id}"
    }

/** 펼친 행 사이 6 (소스 `ul` gap) */
private val CardRowGap = 6.dp

@Composable
private fun EmptyDay(onAddClick: () -> Unit) {
    Column(
        // 비어 있어도 당겨서 새로고침이 되도록 스크롤 영역으로 둔다 (Q-16)
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = DuriseoSpacing.Space4),
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

/** 메모 카드 (D-119): 일정 카드와 같은 `surface-50` 면, 시간 칸 자리에 "메모", 제목(D-131), 작성자 사람 태그. 누르면 SCR-MEMO-03 */
@Composable
private fun MemoCard(memo: CalendarMemo, onClick: () -> Unit) {
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
            text = stringResource(R.string.calendar_memo_mark),
            modifier = Modifier.width(TimeColumnWidth),
            style = MaterialTheme.typography.labelLarge,
            color = Ink600,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = memo.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
        TargetTag(memo.author)
    }
}

/** 펼친 메모 행: 일정 행과 같은 모양, 점은 작성자 색, 시간 칸 자리에 "메모" */
@Composable
private fun MemoRow(memo: CalendarMemo, onClick: () -> Unit) {
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
        Box(Modifier.size(TagDot).background(memo.author.color(), CircleShape))
        Text(
            text = stringResource(R.string.calendar_memo_mark),
            modifier = Modifier.width(RowTimeWidth),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink600,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = memo.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = memo.author.label(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink600,
        )
    }
}

@Composable
private fun TargetTag(target: EventTarget) = PillTag(text = target.label(), dotColor = target.color())

/** 사람 태그 알약. [dotColor]가 null이면 점 없이 글만 (자동 기념일의 "D+N") */
@Composable
private fun PillTag(text: String, dotColor: Color?) {
    Row(
        modifier = Modifier
            .background(Surface0, CircleShape)
            .border(1.dp, Line200, CircleShape)
            .padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(TagGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) Box(Modifier.size(TagDot).background(dotColor, CircleShape))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
    }
}

/** 기념일 카드의 보조 줄: "자동 기념일", "내가 만든 기념일 · 매년" (SCR-CAL-01--anniversary.png, 보드 `sub`) */
@Composable
private fun CalendarAnniversary.subText(): String {
    if (isAuto) return stringResource(R.string.calendar_anniversary_sub_auto)
    val base = stringResource(
        if (author == EventTarget.ME) R.string.calendar_anniversary_sub_mine else R.string.calendar_anniversary_sub_partner,
    )
    return if (repeatsYearly) stringResource(R.string.calendar_anniversary_sub_yearly, base) else base
}

/** 기념일 카드 (D-65·D-117): `brand-50` 면, "기념일"(`brand-700`), 이름, 자동은 그날의 D+N, 직접은 만든 사람 색 점 + 나·상대 */
@Composable
private fun AnniversaryCard(anniversary: CalendarAnniversary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brand50)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = DuriseoSpacing.Space4, vertical = CardPaddingV),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(R.string.calendar_anniversary_mark),
            modifier = Modifier.width(TimeColumnWidth),
            style = MaterialTheme.typography.labelLarge,
            color = Brand700,
            maxLines = 1,
            softWrap = false,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CardTextGap)) {
            Text(
                text = anniversary.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Ink900,
            )
            Text(
                text = anniversary.subText(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal, lineHeight = NoteLineHeight),
                color = Ink600,
            )
        }
        AnniversaryTag(anniversary)
    }
}

@Composable
private fun AnniversaryTag(anniversary: CalendarAnniversary) {
    val author = anniversary.author
    when {
        anniversary.isAuto && anniversary.dayNumber != null ->
            PillTag(text = stringResource(R.string.calendar_anniversary_day_number, anniversary.dayNumber), dotColor = null)

        author != null -> TargetTag(author)
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

/** 펼친 기념일 행: `brand-50` 면, 8짜리 `brand-500` 고리(선 2), "기념일", 이름, D+N 또는 나·상대 (보드 `annItems`) */
@Composable
private fun AnniversaryRow(anniversary: CalendarAnniversary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(RoundedCornerShape(RowRadius))
            .background(Brand50)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = RowPadding),
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3 - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(TagDot).border(MarkRingStroke, Brand500, CircleShape))
        Text(
            text = stringResource(R.string.calendar_anniversary_mark),
            modifier = Modifier.width(RowTimeWidth),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink600,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = anniversary.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val right = when {
            anniversary.isAuto -> anniversary.dayNumber?.let { stringResource(R.string.calendar_anniversary_day_number, it) }
            else -> anniversary.author?.label()
        }
        if (right != null) {
            Text(
                text = right,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Ink600,
            )
        }
    }
}

/** 펼친 기념일 행의 고리 선 2 (`markStyle`의 `border: 2px`) */
private val MarkRingStroke = 2.dp

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

private fun sampleAnniversaries(): Map<LocalDate, List<CalendarAnniversary>> = mapOf(
    LocalDate.of(2026, 10, 5) to listOf(CalendarAnniversary("처음 손잡은 날", false, null, EventTarget.PARTNER, true)),
    LocalDate.of(2026, 10, 23) to listOf(CalendarAnniversary("200일", true, 200, null, false)),
)

private fun soloAnniversaries(): Map<LocalDate, List<CalendarAnniversary>> = mapOf(
    PreviewToday to listOf(CalendarAnniversary("엄마 생신", false, null, EventTarget.ME, true)),
    LocalDate.of(2026, 10, 28) to listOf(CalendarAnniversary("이사한 날", false, null, EventTarget.ME, false)),
)

private fun previewState(
    header: CalendarHeader = CalendarHeader.Couple("pick-orange", "pick-blue", 189),
    selected: LocalDate = PreviewToday,
    expanded: Boolean = false,
    events: Map<LocalDate, List<CalendarEvent>> = sampleEvents(),
    anniversaries: Map<LocalDate, List<CalendarAnniversary>> = sampleAnniversaries(),
    memos: Map<LocalDate, List<CalendarMemo>> = emptyMap(),
    startDateSheet: CalendarStartDateSheet? = null,
    refreshing: Boolean = false,
) = CalendarUiState(
    header = header,
    today = PreviewToday,
    visibleMonth = YearMonth.of(2026, 10),
    selectedDate = selected,
    expanded = expanded,
    eventsByDay = events,
    anniversariesByDay = anniversaries,
    memosByDay = memos,
    startDateSheet = startDateSheet,
    refreshing = refreshing,
)

@Composable
private fun PreviewScreen(state: CalendarUiState) {
    DuriseoTheme {
        CalendarScreen(state = state, padding = PaddingValues(), onOpenMenu = {}, onAction = {})
    }
}

/** `SCR-CAL-01.png`: 기념일 고리(5일·23일)가 달력에 보인다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_Default() = PreviewScreen(previewState())

/** `SCR-CAL-01--anniversary.png`: 기념일이 있는 날(자동 200일, D+200)을 골랐다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_Anniversary() = PreviewScreen(previewState(selected = LocalDate.of(2026, 10, 23)))

/** 직접 기념일(상대가 만든 매년 반복)을 고른 날: 만든 사람 색 점 + "상대" */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_AnniversaryCustom() = PreviewScreen(previewState(selected = LocalDate.of(2026, 10, 5)))

/** `SCR-CAL-01.png`의 메모 카드: 12일 맨 아래 "메모" 카드(작성자 나), 16일·17일 달력 점은 작성자 색 (D-118·D-119) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_Memo() = PreviewScreen(
    previewState(
        memos = mapOf(
            PreviewToday to listOf(CalendarMemo("m1", "선물 리스트 정리하기", EventTarget.ME)),
            LocalDate.of(2026, 10, 16) to listOf(CalendarMemo("m2", "여행 숙소 후보 정리했어", EventTarget.PARTNER)),
            LocalDate.of(2026, 10, 17) to listOf(
                CalendarMemo("m3", "여행 준비물", EventTarget.PARTNER),
                CalendarMemo("m4", "충전기 챙기기", EventTarget.ME),
            ),
        ),
    ),
)

/** `SCR-CAL-01--expanded.png`: 날짜 칸에 일정 막대, 기념일은 `brand-50` 면 + `brand-500` 테두리 막대 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_Expanded() = PreviewScreen(previewState(expanded = true))

/** 펼친 상태에서 기념일 행이 보이는 날 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_ExpandedAnniversary() = PreviewScreen(previewState(expanded = true, selected = LocalDate.of(2026, 10, 23)))

/** `SCR-CAL-01--solo.png`: 개인 공간 앱바 "나의 캘린더 / 기념일 ›", 직접 기념일과 나 일정만 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_Solo() = PreviewScreen(
    previewState(
        header = CalendarHeader.Solo("pick-orange"),
        events = mapOf(
            PreviewToday to listOf(CalendarEvent("1", "필라테스", null, false, LocalTime.of(7, 30), EventTarget.ME)),
            LocalDate.of(2026, 10, 9) to listOf(CalendarEvent("2", "팀 회식", null, false, LocalTime.of(19, 0), EventTarget.ME)),
        ),
        anniversaries = soloAnniversaries(),
    ),
)

/** `SCR-CAL-01--refresh.png`: 당겨서 새로고침 중 (목록 위 64, 가로 가운데 원판) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_Refresh() = PreviewScreen(previewState(refreshing = true))

/** `SCR-CAL-01--no-start-date.png`: 커플 + 처음 만난 날 없음 (자동 기념일 없음) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_NoStartDate() = PreviewScreen(
    previewState(
        header = CalendarHeader.Couple("pick-orange", "pick-blue", null),
        anniversaries = sampleAnniversaries().filterValues { list -> list.none { it.isAuto } },
    ),
)

/** 선택한 날 비어 있음: 보드 문구 "아직 일정이나 메모가 없어요" (이미지 없음) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_EmptyDay() = PreviewScreen(previewState(selected = LocalDate.of(2026, 10, 14)))

/** 처음 만난 날 선택 시트 (SCR-CAL-01 앱바 "처음 만난 날 입력하기") */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview_StartDateSheet() = PreviewScreen(
    previewState(
        header = CalendarHeader.Couple("pick-orange", "pick-blue", null),
        startDateSheet = CalendarStartDateSheet(YearMonth.of(2026, 10), null),
    ),
)

/** 날짜 목록 카드: 일정, 기념일(자동·직접)과 펼친 행 */
@Preview(showBackground = true, widthDp = 390)
@Composable
private fun CalendarPreview_Cards() {
    DuriseoTheme {
        Column(Modifier.padding(DuriseoSpacing.Space4), verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
            AnniversaryCard(CalendarAnniversary("200일", true, 200, null, false), {})
            AnniversaryCard(CalendarAnniversary("엄마 생신", false, null, EventTarget.ME, true), {})
            EventCard(CalendarEvent("1", "저녁 데이트", "예약 완료", false, LocalTime.of(19, 30), EventTarget.TOGETHER), {})
            AnniversaryRow(CalendarAnniversary("200일", true, 200, null, false), {})
            AnniversaryRow(CalendarAnniversary("처음 손잡은 날", false, null, EventTarget.PARTNER, true), {})
            EventRow(CalendarEvent("2", "1박 2일 여행", null, true, null, EventTarget.PARTNER), {})
            MemoCard(CalendarMemo("m1", "선물 리스트 정리하기", EventTarget.ME), {})
            MemoRow(CalendarMemo("m2", "여행 숙소 후보 정리했어", EventTarget.PARTNER), {})
        }
    }
}
