package com.duriseo.android.feature.notification.list

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoTopAppBar
import com.duriseo.android.core.designsystem.format.writtenAtText
import com.duriseo.android.core.designsystem.theme.Brand50
import com.duriseo.android.core.designsystem.theme.Brand100
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink400
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.feature.notification.R

// SCR-NOTI-01 알림 목록 (FR-NOTI-05, FR-NOTI-03). 수치는 보드 `Notifications.dc.html`.
// 토큰에 없는 보드 수치는 이름을 붙인 상수로 둔다.

/** 항목: 안쪽 위아래 14·좌우 20, 아이콘 원과 글 사이 14, 글 줄 사이 2 */
private val RowPaddingVertical = 14.dp
private val RowGap = 14.dp
private val TextGap = 2.dp

/** 종류 아이콘 원 44, 아이콘 22 */
private val TypeCircle = 44.dp
private val TypeIcon = 22.dp

/** 안 읽음 점 8, 점 칸 폭 8, 점 위 여백 8 */
private val DotSize = 8.dp
private val DotTop = 8.dp

/** 안내 문장: 위 0·아래 8·좌우 20 */
private val HintBottom = DuriseoSpacing.Space2

/** 목록 아래 여백 28 */
private val ListBottom = 28.dp

/** 빈 화면: 좌우 32, 아래 120(보드 `padding: 0 32px 120px`), 원 72에 아이콘 32, 글 사이 16 */
private val EmptyPaddingHorizontal = DuriseoSpacing.Space8
private val EmptyPaddingBottom = 120.dp
private val EmptyCircle = 72.dp
private val EmptyIcon = 32.dp

/** 목록 끝에서 이만큼 남으면 다음 페이지를 이어 붙인다 */
private const val LOAD_MORE_THRESHOLD = 3

/** 제목은 안 읽음이면 700, 읽음이면 500 (보드 `titleWeight`) */
private val TitleUnread = FontWeight.Bold
private val TitleRead = FontWeight.Medium

@Composable
fun NotificationListRoute(
    lifecycleOwner: LifecycleOwner,
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpenDestination: (NotificationDestination) -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    // 화면이 보이는 동안만 "목록이 열려 있음"이다 (동기화 신호가 오면 첫 페이지도 다시 받는다, FR-NOTI-05 AC7)
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAction(NotificationListAction.Shown)
                Lifecycle.Event.ON_STOP -> viewModel.onAction(NotificationListAction.Hidden)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onAction(NotificationListAction.Hidden)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NotificationListSideEffect.Back -> onBack()
                is NotificationListSideEffect.Navigate -> onOpenDestination(effect.destination)
                is NotificationListSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    NotificationListScreen(state = state, onAction = viewModel::onAction, padding = padding, modifier = modifier)
}

@Composable
fun NotificationListScreen(
    state: NotificationListUiState,
    onAction: (NotificationListAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Surface0)
            .padding(padding),
    ) {
        DuriseoTopAppBar(
            title = stringResource(R.string.notification_title),
            onBack = { onAction(NotificationListAction.BackClicked) },
        )
        when {
            state.isEmpty -> EmptyState()
            state.items.isNotEmpty() -> ListBody(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun ListBody(state: NotificationListUiState, onAction: (NotificationListAction) -> Unit) {
    val listState = rememberLazyListState()
    // 끝 가까이까지 스크롤하면 다음 페이지를 이어 붙인다 (안내 문장이 0번 칸이라 개수에 포함된다)
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, state.items.size, state.hasMore) {
        if (nearEnd && state.hasMore) onAction(NotificationListAction.EndReached)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = ListBottom),
    ) {
        item(key = "hint") {
            Text(
                text = stringResource(R.string.notification_hint),
                modifier = Modifier.padding(start = DuriseoSpacing.Space5, end = DuriseoSpacing.Space5, bottom = HintBottom),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
        }
        items(state.items, key = { it.id }) { item ->
            NotificationRow(item = item, onClick = { onAction(NotificationListAction.ItemClicked(item.id)) })
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItemUi, onClick: () -> Unit) {
    val typeLabel = stringResource(item.type.labelRes())
    val time = writtenAtText(item.writtenAt)
    val unreadText = stringResource(R.string.notification_unread)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (item.unread) Brand50 else Surface0)
                .clickable(role = Role.Button, onClick = onClick)
                // 안 읽음은 점만으로 알리지 않고 읽어 주는 문구로도 알린다 (색·점만으로 뜻을 전하지 않음)
                .semantics(mergeDescendants = true) { if (item.unread) stateDescription = unreadText }
                .padding(horizontal = DuriseoSpacing.Space5, vertical = RowPaddingVertical),
            horizontalArrangement = Arrangement.spacedBy(RowGap),
            verticalAlignment = Alignment.Top,
        ) {
            TypeIconCircle(iconRes = item.type.iconRes(), unread = item.unread)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TextGap)) {
                Text(
                    text = stringResource(R.string.notification_type_and_time, typeLabel, time),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink600,
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (item.unread) TitleUnread else TitleRead),
                    color = Ink900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(modifier = Modifier.width(DotSize).padding(top = DotTop), contentAlignment = Alignment.TopCenter) {
                if (item.unread) {
                    Box(modifier = Modifier.size(DotSize).background(Brand500, DuriseoShapes.Full))
                }
            }
        }
        HorizontalDivider(color = Line100)
    }
}

@Composable
private fun TypeIconCircle(@DrawableRes iconRes: Int, unread: Boolean) {
    Box(
        modifier = Modifier
            .size(TypeCircle)
            .background(if (unread) Brand100 else Surface100, DuriseoShapes.Full),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(TypeIcon),
            tint = if (unread) Brand700 else Ink600,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = EmptyPaddingHorizontal, end = EmptyPaddingHorizontal, bottom = EmptyPaddingBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space4, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier.size(EmptyCircle).background(Surface100, DuriseoShapes.Full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_bell),
                contentDescription = null,
                modifier = Modifier.size(EmptyIcon),
                tint = Ink400,
            )
        }
        Text(
            text = stringResource(R.string.notification_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = Ink600,
            textAlign = TextAlign.Center,
        )
    }
}

private fun NotificationRowType.labelRes(): Int = when (this) {
    NotificationRowType.EVENT_REMINDER -> R.string.notification_type_event_reminder
    NotificationRowType.PARTNER_EVENT -> R.string.notification_type_partner_event
    NotificationRowType.PARTNER_MEMO -> R.string.notification_type_partner_memo
    NotificationRowType.MEMO_PINNED -> R.string.notification_type_memo_pinned
    NotificationRowType.ANNIVERSARY -> R.string.notification_type_anniversary
    NotificationRowType.COUPLE_LINKED -> R.string.notification_type_couple_linked
    NotificationRowType.COUPLE_UNLINKED -> R.string.notification_type_couple_unlinked
}

/** 종류 아이콘 (보드: 종 / 달력 / 메모 / 핀 / 링크 / 사람) */
@DrawableRes
private fun NotificationRowType.iconRes(): Int = when (this) {
    NotificationRowType.EVENT_REMINDER -> DesignR.drawable.ic_bell
    NotificationRowType.PARTNER_EVENT, NotificationRowType.ANNIVERSARY -> DesignR.drawable.ic_calendar
    NotificationRowType.PARTNER_MEMO -> DesignR.drawable.ic_tab_memo
    NotificationRowType.MEMO_PINNED -> DesignR.drawable.ic_pin
    NotificationRowType.COUPLE_LINKED -> DesignR.drawable.ic_link
    NotificationRowType.COUPLE_UNLINKED -> DesignR.drawable.ic_user
}

private fun sampleItems(): List<NotificationItemUi> = listOf(
    NotificationItemUi("1", NotificationRowType.PARTNER_EVENT, "[상대 이름]님이 일정을 추가했어요", "10월 17일 (토) · 1박 2일 여행", WrittenAt.Today(false, 9, 12), true),
    NotificationItemUi("2", NotificationRowType.EVENT_REMINDER, "치과 예약", "1시간 뒤 · 오후 2:00", WrittenAt.Today(true, 1, 0), true),
    NotificationItemUi("3", NotificationRowType.MEMO_PINNED, "[상대 이름]님이 메모를 고정했어요", "여행 숙소 후보 정리했어", WrittenAt.Yesterday, true),
    NotificationItemUi("4", NotificationRowType.PARTNER_MEMO, "[상대 이름]님이 메모를 남겼어요", "여행 숙소 후보 정리했어", WrittenAt.ThisYear(10, 6), false),
    NotificationItemUi("5", NotificationRowType.ANNIVERSARY, "오늘은 200일이에요", "함께한 지 D+200", WrittenAt.ThisYear(10, 6), false),
    NotificationItemUi("6", NotificationRowType.COUPLE_LINKED, "[상대 이름]님과 연결됐어요", "이제 캘린더와 메모를 함께 써요", WrittenAt.OtherYear(2025, 9, 20), false),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationListPreview_Default() {
    DuriseoTheme {
        NotificationListScreen(state = NotificationListUiState(loaded = true, items = sampleItems()), onAction = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationListPreview_Read() {
    DuriseoTheme {
        NotificationListScreen(
            state = NotificationListUiState(loaded = true, items = sampleItems().map { it.copy(unread = false) }),
            onAction = {},
        )
    }
}

/** 상대가 탈퇴한 뒤: 커플 공간 알림은 지워지고 연결·해제 알림만 남는다 (PRD D-45, D-53) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationListPreview_Withdrawn() {
    DuriseoTheme {
        NotificationListScreen(
            state = NotificationListUiState(
                loaded = true,
                items = listOf(
                    NotificationItemUi("21", NotificationRowType.COUPLE_UNLINKED, "[상대 이름]님이 탈퇴해 연결이 해제됐어요", "함께 쓴 기록은 더 이상 볼 수 없어요", WrittenAt.Today(false, 9, 12), true),
                    NotificationItemUi("22", NotificationRowType.COUPLE_LINKED, "[상대 이름]님과 연결됐어요", "이제 캘린더와 메모를 함께 써요", WrittenAt.ThisYear(9, 20), false),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationListPreview_Empty() {
    DuriseoTheme {
        NotificationListScreen(state = NotificationListUiState(loaded = true), onAction = {})
    }
}
