package com.duriseo.android.feature.memo.list

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoLoadingLayer
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.DuriseoShadows
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
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
import com.duriseo.android.core.designsystem.theme.duriseoShadow
import com.duriseo.android.domain.format.MemoDateChip
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.feature.memo.R
import com.duriseo.android.feature.memo.component.MemoAuthor
import com.duriseo.android.feature.memo.component.MemoItemUi
import com.duriseo.android.feature.memo.component.MemoRow
import com.duriseo.android.feature.memo.component.PinCard
import java.time.DayOfWeek

// SCR-MEMO-01 메모 목록 (FR-MEMO-01, FR-MEMO-05, FR-MEMO-06). 수치는 보드 `Memo.dc.html`, design-system.md.
// 토큰에 없는 보드 수치는 이름을 붙인 상수로 둔다.

/** 앱바: 높이 64(`size-appbar`), 안쪽 왼쪽 20·오른쪽 8, 제목 22/700 (`Memo.dc.html` 19·20행) */
private val AppBarEnd = DuriseoSpacing.Space2

/** 목록: 좌우 20, 위 4, 아래 120(FAB 위까지), 덩어리 사이 24 (22행) */
private val ListTop = DuriseoSpacing.Space1
private val ListBottom = 120.dp
private val BlockGap = DuriseoSpacing.Space6

/** 고정 영역: 제목 줄과 카드 사이 10, 카드 사이 10, 핀 아이콘 18, 아이콘과 글 사이 6 (23·25행) */
private val PinSectionGap = 10.dp
private val PinTitleIcon = 18.dp
private val PinTitleGap = 6.dp

/** 고정 없음 점선 상자: 안쪽 16, 모서리 16, 점선 1 `line-300`, 글 13/1.6 (32행) */
private val DashedPadding = DuriseoSpacing.Space4
private val DashedStroke = 1.dp
private val DashedLineHeight = 20.8.sp

/** [고정 메모 전체 보기]: 높이 44, 모서리 14, 테두리 1 `line-200`, 글 14/600, 화살표 16, 사이 4 (61행) */
private val MoreHeight = 44.dp
private val MoreChevron = 16.dp
private val MoreGap = DuriseoSpacing.Space1

/** 전체 메모: 제목 줄과 칩 사이 6, 칩 줄 안쪽 위 6·아래 4, 칩 사이 8, 칩 높이 36·안쪽 좌우 14, 별 14·사이 4 (64·69·173행) */
private val ListSectionGap = 6.dp
private val ChipsPaddingTop = 6.dp
private val ChipsPaddingBottom = DuriseoSpacing.Space1
private val ChipHeight = 36.dp
private val ChipPaddingHorizontal = 14.dp
private val ChipStar = 14.dp
private val ChipGap = DuriseoSpacing.Space1

/** 즐겨찾기 없음 문구: 위아래 32, 가운데 정렬, 14/1.6 (78행) */
private val EmptyPaddingVertical = DuriseoSpacing.Space8
private val EmptyLineHeight = 22.4.sp

/** FAB 아이콘 26 (`Memo.dc.html` 102행). 위치: 오른쪽 20, 탭 위 20 = 바닥에서 100 */
private val FabIcon = 26.dp
private val FabEnd = DuriseoSpacing.Space5
private val FabBottom = DuriseoSpacing.Space5

/** 스낵바 높이 49 = 위아래 14 + 한 줄 21 (design-system.md "스낵바") */
private val SnackbarHeight = 49.dp

/** 스낵바와 하단 탭 사이 간격 16, 스낵바와 FAB 사이 간격 16 (SCREENS.md 공통 "스낵바") */
private val SnackbarGap = DuriseoSpacing.Space4

/**
 * FAB 아래 간격(탭 위 기준). 스낵바가 없으면 20(바닥에서 100), 있으면 스낵바 위 16: 16 + 49 + 16 = 81 (바닥에서 161, D-126).
 */
internal fun memoFabBottom(snackbarVisible: Boolean): Dp =
    if (snackbarVisible) SnackbarGap + SnackbarHeight + SnackbarGap else FabBottom

/** 목록 끝에서 이만큼 남으면 다음 페이지를 이어 붙인다 */
private const val LOAD_MORE_THRESHOLD = 3

/** 점선 모양: 4 그리고 4 쉬기 (CSS `1px dashed`의 기본 모양) */
private const val DASH_ON = 4f
private const val DASH_OFF = 4f

@Composable
fun MemoListRoute(
    lifecycleOwner: LifecycleOwner,
    padding: PaddingValues,
    onOpenDetail: (memoId: String) -> Unit,
    onOpenPinned: () -> Unit,
    onOpenCreate: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    snackbarVisible: Boolean = false,
    viewModel: MemoListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    // 화면이 보일 때마다(탭으로 들어올 때, 하위 화면에서 돌아올 때) 다시 불러온다
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.onAction(MemoListAction.Shown)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MemoListSideEffect.OpenDetail -> onOpenDetail(effect.memoId)
                MemoListSideEffect.OpenPinned -> onOpenPinned()
                MemoListSideEffect.OpenCreate -> onOpenCreate()
                is MemoListSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    MemoListScreen(
        state = state,
        onAction = viewModel::onAction,
        padding = padding,
        snackbarVisible = snackbarVisible,
        modifier = modifier,
    )
}

@Composable
fun MemoListScreen(
    state: MemoListUiState,
    onAction: (MemoListAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    snackbarVisible: Boolean = false,
) {
    DuriseoLoadingLayer(loading = state.loading, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Surface0)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 앱바는 "메모장"만 있다. 검색 버튼 없음 (SCREENS SCR-MEMO-01)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DuriseoSize.Appbar)
                        .padding(start = DuriseoSpacing.Space5, end = AppBarEnd),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(R.string.memo_list_title),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink900,
                    )
                }
                if (state.loaded) ListBody(state = state, onAction = onAction)
            }
            MemoFab(
                onClick = { onAction(MemoListAction.AddClicked) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = FabEnd, bottom = memoFabBottom(snackbarVisible)),
            )
        }
    }
}

@Composable
private fun ListBody(state: MemoListUiState, onAction: (MemoListAction) -> Unit) {
    val listState = rememberLazyListState()
    // 끝 가까이까지 스크롤하면 다음 페이지를 이어 붙인다
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, state.items.size, state.view) {
        if (nearEnd && state.hasMore) onAction(MemoListAction.EndReached)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = DuriseoSpacing.Space5,
            end = DuriseoSpacing.Space5,
            top = ListTop,
            bottom = ListBottom,
        ),
    ) {
        item(key = "pins-header") {
            PinsHeader(count = state.pins.size, modifier = Modifier.padding(bottom = PinSectionGap))
        }
        if (state.showNoPins) {
            item(key = "pins-empty") { NoPinsBox() }
        }
        items(state.visiblePins, key = { "pin-" + it.id }) { pin ->
            PinCard(
                item = pin,
                onOpen = { onAction(MemoListAction.ItemClicked(pin)) },
                onPinClick = { onAction(MemoListAction.PinClicked(pin)) },
                onFavoriteClick = { onAction(MemoListAction.FavoriteClicked(pin)) },
                modifier = Modifier.padding(bottom = PinSectionGap),
            )
        }
        if (state.showPinsMore) {
            item(key = "pins-more") { PinsMoreButton(count = state.pins.size, onClick = { onAction(MemoListAction.PinsMoreClicked) }) }
        }
        item(key = "all-header") {
            Column(modifier = Modifier.padding(top = BlockGap), verticalArrangement = Arrangement.spacedBy(ListSectionGap)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.memo_all_title),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleSmall,
                        color = Ink900,
                    )
                    CountText(state.totalCount)
                }
                ViewChips(view = state.view, favoriteCount = state.favoriteCount, onSelect = { onAction(MemoListAction.ViewSelected(it)) })
            }
        }
        if (state.showFavoritesEmpty) {
            item(key = "favorites-empty") {
                Text(
                    text = stringResource(R.string.memo_favorites_empty),
                    modifier = Modifier.fillMaxWidth().padding(vertical = EmptyPaddingVertical),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = EmptyLineHeight),
                    color = Ink600,
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(state.items, key = { it.id }) { item ->
            MemoRow(
                item = item,
                onOpen = { onAction(MemoListAction.ItemClicked(item)) },
                onPinClick = { onAction(MemoListAction.PinClicked(item)) },
                onFavoriteClick = { onAction(MemoListAction.FavoriteClicked(item)) },
            )
        }
    }
}

/** "고정된 핀" + 오른쪽 "N개" (13, `ink-500`) */
@Composable
private fun PinsHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PinTitleGap)) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_pin_filled),
                contentDescription = null,
                modifier = Modifier.size(PinTitleIcon),
                tint = Brand700,
            )
            Text(
                text = stringResource(R.string.memo_pins_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                color = Ink900,
            )
        }
        CountText(count)
    }
}

@Composable
private fun CountText(count: Int) {
    Text(
        text = stringResource(R.string.memo_count, count),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
        color = Ink500,
    )
}

/** 고정 없음 점선 상자의 모서리 16 (보드 `Memo.dc.html` 32행 `border-radius: 16px`) */
private val NoPinsRadius = 16.dp

/** 고정 없음: 점선 상자 "중요한 메모는 핀으로 고정하면 둘 다 맨 위에서 공지처럼 볼 수 있어요." */
@Composable
private fun NoPinsBox(modifier: Modifier = Modifier) {
    val radius = NoPinsRadius
    Text(
        text = stringResource(R.string.memo_pins_empty),
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = Line300,
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(
                        width = DashedStroke.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON.dp.toPx(), DASH_OFF.dp.toPx())),
                    ),
                )
            }
            .padding(DashedPadding),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal, lineHeight = DashedLineHeight),
        color = Ink600,
    )
}

@Composable
private fun PinsMoreButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MoreHeight)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, Line200, MaterialTheme.shapes.medium)
            .background(Surface0)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(MoreGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.memo_pins_more, count),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
        Icon(
            painter = painterResource(DesignR.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(MoreChevron),
            tint = Ink900,
        )
    }
}

/** 보기 칩 [최신순 | 오래된순 | ★ 즐겨찾기 N]. 선택은 `ink-900` 채움 + 흰 글자 (SCREENS SCR-MEMO-01) */
@Composable
private fun ViewChips(view: MemoView, favoriteCount: Int, onSelect: (MemoView) -> Unit) {
    Row(
        modifier = Modifier.padding(top = ChipsPaddingTop, bottom = ChipsPaddingBottom),
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
    ) {
        ViewChip(
            text = stringResource(R.string.memo_view_newest),
            selected = view == MemoView.NEWEST,
            onClick = { onSelect(MemoView.NEWEST) },
        )
        ViewChip(
            text = stringResource(R.string.memo_view_oldest),
            selected = view == MemoView.OLDEST,
            onClick = { onSelect(MemoView.OLDEST) },
        )
        ViewChip(
            text = stringResource(R.string.memo_view_favorites, favoriteCount),
            selected = view == MemoView.FAVORITES,
            onClick = { onSelect(MemoView.FAVORITES) },
            showStar = true,
        )
    }
}

@Composable
private fun ViewChip(text: String, selected: Boolean, onClick: () -> Unit, showStar: Boolean = false) {
    // 보이는 높이는 36 그대로, 터치 영역만 44로 키운다
    Box(
        modifier = Modifier
            .heightIn(min = DuriseoSize.Touch)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(ChipHeight)
                .clip(DuriseoShapes.Full)
                .then(if (selected) Modifier.background(Ink900) else Modifier.border(1.dp, Line200, DuriseoShapes.Full))
                .padding(horizontal = ChipPaddingHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChipGap),
        ) {
            val color = if (selected) OnBrand else Ink600
            if (showStar) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_star_filled),
                    contentDescription = null,
                    modifier = Modifier.size(ChipStar),
                    tint = color,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                color = color,
            )
        }
    }
}

@Composable
private fun MemoFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            contentDescription = stringResource(R.string.memo_fab_description),
            modifier = Modifier.size(FabIcon),
            tint = OnBrand,
        )
    }
}

// --- Preview. 견본은 보드 `Memo.dc.html`와 같다 (오늘 2026-10-12) ---

private fun sample(
    id: String,
    title: String,
    preview: String,
    author: MemoAuthor,
    writtenAt: WrittenAt,
    memoDate: MemoDateChip? = null,
    pinned: Boolean = false,
    favorite: Boolean = false,
) = MemoItemUi(
    id = id,
    title = title,
    preview = preview,
    author = author,
    nickname = if (author == MemoAuthor.ME) "[내 이름]" else "[상대 이름]",
    writtenAt = writtenAt,
    memoDate = memoDate,
    isPinned = pinned,
    isFavorite = favorite,
    canEdit = author == MemoAuthor.ME,
)

private val Pin1 = sample("2", "장보기", "우유, 계란, 두부, 대파, 사과", MemoAuthor.PARTNER, WrittenAt.Today(isPm = false, hour12 = 8, minute = 40), pinned = true)
private val Pin2 = sample(
    "1", "여행 숙소 후보 정리했어", "세 곳 골라놨는데 주말 전까지 하나 골라줘!", MemoAuthor.PARTNER,
    WrittenAt.Today(isPm = false, hour12 = 9, minute = 12), MemoDateChip(null, 10, 16, DayOfWeek.FRIDAY), pinned = true,
)
private val Pin3 = sample("5", "주말 집안일 분담", "토요일 장보기는 내가, 일요일 청소는 같이 하기", MemoAuthor.ME, WrittenAt.ThisYear(10, 8), pinned = true)
private val Pin4 = sample("8", "이번 달 데이트 통장 정리", "이번 달은 여행 때문에 조금 더 넣기로 했어요.", MemoAuthor.ME, WrittenAt.ThisYear(10, 1), pinned = true, favorite = true)

private val Rows = listOf(
    sample(
        "3", "선물 아이디어", "향수 같이 시향해보기, 지난번에 본 머플러", MemoAuthor.ME, WrittenAt.Yesterday,
        MemoDateChip(null, 11, 20, DayOfWeek.FRIDAY), favorite = true,
    ),
    sample("4", "여행 준비물", "충전기, 여권 사본, 접이식 우산", MemoAuthor.PARTNER, WrittenAt.ThisYear(10, 9), MemoDateChip(null, 10, 17, DayOfWeek.SATURDAY)),
    sample("6", "부모님 생신 선물 같이 고르기", "생신 전 주말에 같이 보러 가자", MemoAuthor.PARTNER, WrittenAt.ThisYear(10, 5), favorite = true),
    sample("7", "보고 싶은 영화", "[영화 제목] 외 2편", MemoAuthor.ME, WrittenAt.ThisYear(10, 2)),
)

@Composable
private fun PreviewScreen(state: MemoListUiState) {
    DuriseoTheme { MemoListScreen(state = state, onAction = {}) }
}

private val DefaultState = MemoListUiState(
    loaded = true,
    pins = listOf(Pin3),
    pinsLoaded = true,
    items = Rows,
    totalCount = 5,
    favoriteCount = 2,
    listLoaded = true,
)

/** 기본: 고정 1개(3개 이하는 모두), 최신순 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoListPreview_Default() = PreviewScreen(DefaultState)

/** 고정 4개: 최근 고정 3개 + [고정 메모 전체 보기 (4) ›] (`SCR-MEMO-01.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoListPreview_PinsMore() = PreviewScreen(DefaultState.copy(pins = listOf(Pin1, Pin2, Pin3, Pin4)))

/** 고정 없음: 점선 상자 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoListPreview_NoPins() = PreviewScreen(DefaultState.copy(pins = emptyList()))

/** 즐겨찾기 보기 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoListPreview_Favorites() = PreviewScreen(
    DefaultState.copy(
        view = MemoView.FAVORITES,
        items = Rows.filter { it.isFavorite },
        totalCount = 2,
    ),
)

/** 즐겨찾기 없음: "즐겨찾기한 메모가 없어요. 메모 옆 별을 눌러 추가해 보세요." */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoListPreview_FavoritesEmpty() = PreviewScreen(
    DefaultState.copy(view = MemoView.FAVORITES, items = emptyList(), totalCount = 0, favoriteCount = 0),
)

/** 메모 없음: 문서에 문구가 없어 "전체 메모 0개"와 빈 목록 (unit-6.md 확인 필요 2) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoListPreview_Empty() = PreviewScreen(
    DefaultState.copy(pins = emptyList(), items = emptyList(), totalCount = 0, favoriteCount = 0),
)

/** 핀·별 요청 중: 로딩 덮개 (D-115) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoListPreview_Loading() = PreviewScreen(DefaultState.copy(writing = true))

/** 스낵바가 떠 있을 때 FAB은 바닥에서 161 (D-126) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoListPreview_SnackbarFab() {
    DuriseoTheme { MemoListScreen(state = DefaultState, onAction = {}, snackbarVisible = true) }
}
