package com.duriseo.android.feature.memo.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDialog
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.format.writtenAtText
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.Danger700
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.IbmPlexSansKr
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.domain.format.MemoDateChip
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.feature.memo.R
import com.duriseo.android.feature.memo.component.FavoriteToggleButton
import com.duriseo.android.feature.memo.component.MemoAuthor
import com.duriseo.android.feature.memo.component.MemoAuthorDot
import com.duriseo.android.feature.memo.component.MemoDateBadge
import com.duriseo.android.feature.memo.component.PinToggleButton
import java.time.DayOfWeek

// SCR-MEMO-03 메모 상세 (FR-MEMO-03, FR-MEMO-05, FR-MEMO-06). 수치는 보드 `MemoDetail.dc.html`.

/** 앱바: 높이 64, 안쪽 좌우 8, 뒤로 48x48, 핀·별 44x48(아이콘 22), [수정] 높이 48·안쪽 왼 6·오른 12, 글 16/700 `brand-700` (19~28행) */
private val HeaderIconButton = DuriseoSize.ButtonMd
private val HeaderToggleIcon = 22.dp
private val EditPaddingStart = 6.dp
private val EditPaddingEnd = DuriseoSpacing.Space3

/** 본문 영역: 안쪽 위 4·좌우 20·아래 28, 덩어리 사이 16 (31행). 제목과 메타·칩 사이 10 */
private val BodyTop = DuriseoSpacing.Space1
private val BodyBottom = 28.dp
private val SectionGap = DuriseoSpacing.Space4
private val TitleGap = 10.dp

/** 제목 24/700, 행간 1.35(32.4), 자간 -0.01em: 토큰에 없는 보드 값 (`MemoDetail.dc.html` 33행). SCR-CAL-02와 같다 */
private val DetailTitleStyle = TextStyle(
    fontFamily = IbmPlexSansKr,
    fontSize = 24.sp,
    lineHeight = 32.4.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.01).em,
)

/** 메타 줄 13, 점과 글 사이 6 (34행). 본문 16/1.7(27.2), 위 선과 본문 사이 16 (39행). 안내 13/1.6 (41행) */
private val MetaGap = 6.dp
private val BodyLineHeight = 27.2.sp
private val NoteLineHeight = 20.8.sp

/** 삭제 막대: 버튼 높이 52, 막대 안쪽 위 12·좌우 20·아래 32, 위 선 1 `line-100` (45·46행) */
private val DeleteButtonHeight = 52.dp
private val DeleteBarBottom = 32.dp

@Composable
fun MemoDetailRoute(
    lifecycleOwner: LifecycleOwner,
    padding: PaddingValues,
    onClose: () -> Unit,
    onOpenEdit: (memoId: String) -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.onAction(MemoDetailAction.Shown)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MemoDetailSideEffect.Close -> onClose()
                is MemoDetailSideEffect.OpenEdit -> onOpenEdit(effect.memoId)
                is MemoDetailSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    MemoDetailScreen(state = state, onAction = viewModel::onAction, padding = padding, modifier = modifier)
}

@Composable
fun MemoDetailScreen(
    state: MemoDetailUiState,
    onAction: (MemoDetailAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    val memo = state.memo
    BackHandler(enabled = state.deleteDialogVisible && !state.deleting) { onAction(MemoDetailAction.DeleteCancelled) }
    DuriseoDialogLayer(
        dialogVisible = state.deleteDialogVisible,
        dialog = {
            DuriseoDialog(
                title = stringResource(R.string.memo_delete_dialog_title),
                body = stringResource(R.string.memo_delete_dialog_body),
                cancelText = stringResource(DesignR.string.action_cancel),
                confirmText = stringResource(R.string.memo_delete_dialog_confirm),
                onCancel = { onAction(MemoDetailAction.DeleteCancelled) },
                onConfirm = { onAction(MemoDetailAction.DeleteConfirmed) },
                confirmStyle = DuriseoButtonStyle.Destructive,
                buttonsEnabled = !state.deleting,
            )
        },
        modifier = modifier,
        loading = state.loading,
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Surface0).padding(padding)) {
            DetailHeader(memo = memo, onAction = onAction)
            if (memo != null) DetailBody(memo = memo, onAction = onAction)
        }
    }
}

@Composable
private fun DetailHeader(memo: MemoDetailUi?, onAction: (MemoDetailAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(horizontal = DuriseoSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onAction(MemoDetailAction.BackClicked) }, modifier = Modifier.size(HeaderIconButton)) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_back),
                contentDescription = stringResource(DesignR.string.nav_back),
                modifier = Modifier.size(DuriseoSize.Icon),
                tint = Ink900,
            )
        }
        Box(modifier = Modifier.weight(1f))
        if (memo != null) {
            // 꺼진 핀·별은 `ink-600` 선이다 (상세 보드 `pinStroke`·`starStroke`)
            PinToggleButton(
                pinned = memo.isPinned,
                onClick = { onAction(MemoDetailAction.PinClicked) },
                offTint = Ink600,
                iconSize = HeaderToggleIcon,
                height = HeaderIconButton,
            )
            FavoriteToggleButton(
                favorite = memo.isFavorite,
                onClick = { onAction(MemoDetailAction.FavoriteClicked) },
                offTint = Ink600,
                iconSize = HeaderToggleIcon,
                height = HeaderIconButton,
            )
            // [수정]은 내 메모일 때만 보인다 (권한 매트릭스)
            if (memo.canEdit) {
                Box(
                    modifier = Modifier
                        .height(HeaderIconButton)
                        .clickable(role = Role.Button) { onAction(MemoDetailAction.EditClicked) }
                        .padding(start = EditPaddingStart, end = EditPaddingEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.memo_detail_edit),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Brand700,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailBody(memo: MemoDetailUi, onAction: (MemoDetailAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = DuriseoSpacing.Space5, end = DuriseoSpacing.Space5, top = BodyTop, bottom = BodyBottom),
            verticalArrangement = Arrangement.spacedBy(SectionGap),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TitleGap), horizontalAlignment = Alignment.Start) {
                if (!memo.title.isNullOrEmpty()) {
                    Text(
                        text = memo.title,
                        modifier = Modifier.semantics { heading() },
                        style = DetailTitleStyle,
                        color = Ink900,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MetaGap)) {
                    MemoAuthorDot(memo.author)
                    Text(
                        text = stringResource(R.string.memo_detail_author, memo.nickname, writtenAtText(memo.writtenAt)),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                        color = Ink600,
                    )
                }
                memo.memoDate?.let { chip -> MemoDateBadge(chip = chip, background = Surface100, large = true) }
            }
            HorizontalDivider(color = Line100)
            if (!memo.body.isNullOrEmpty()) {
                Text(
                    text = memo.body,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = BodyLineHeight),
                    color = Ink900,
                )
            }
            if (!memo.canEdit) {
                // 상대가 쓴 메모는 읽기만 된다. 고정·즐겨찾기는 할 수 있다 (FR-MEMO-03 AC2)
                Text(
                    text = stringResource(R.string.memo_detail_read_only),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal, lineHeight = NoteLineHeight),
                    color = Ink600,
                )
            }
        }
        if (memo.canEdit) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface0)
                    .drawBehind { drawLine(Line100, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
                    .padding(
                        start = DuriseoSpacing.Space5,
                        end = DuriseoSpacing.Space5,
                        top = DuriseoSpacing.Space3,
                        bottom = DeleteBarBottom,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DeleteButtonHeight)
                        .clip(MaterialTheme.shapes.medium)
                        .border(1.dp, Line200, MaterialTheme.shapes.medium)
                        .clickable(role = Role.Button) { onAction(MemoDetailAction.DeleteClicked) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.memo_detail_delete),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Danger700,
                    )
                }
            }
        }
    }
}

// --- Preview. 견본은 보드 `MemoDetail.dc.html`와 같다 ---

private val Mine = MemoDetailUi(
    id = "3",
    title = "선물 아이디어",
    body = "향수 같이 시향해보기, 지난번에 본 머플러\n생일 전 주말에 같이 보러 가기",
    author = MemoAuthor.ME,
    nickname = "[내 이름]",
    writtenAt = WrittenAt.ThisYear(10, 11),
    memoDate = MemoDateChip(null, 11, 20, DayOfWeek.FRIDAY),
    isPinned = false,
    isFavorite = true,
    canEdit = true,
)

private val Partner = MemoDetailUi(
    id = "2",
    title = "장보기",
    body = "우유, 계란, 두부, 대파, 사과",
    author = MemoAuthor.PARTNER,
    nickname = "[상대 이름]",
    writtenAt = WrittenAt.Today(isPm = false, hour12 = 8, minute = 40),
    memoDate = null,
    isPinned = true,
    isFavorite = false,
    canEdit = false,
)

@Composable
private fun PreviewScreen(state: MemoDetailUiState) {
    DuriseoTheme { MemoDetailScreen(state = state, onAction = {}) }
}

/** `SCR-MEMO-03.png`: 내 메모, 날짜 있음. [수정]·[메모 삭제] 있음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoDetailPreview_Mine() = PreviewScreen(MemoDetailUiState(memo = Mine))

/** `SCR-MEMO-03--partner.png`: 상대 메모, 날짜 없음. 고정·즐겨찾기만 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoDetailPreview_Partner() = PreviewScreen(MemoDetailUiState(memo = Partner))

/** `SCR-MEMO-03--other-year.png`: 올해가 아닌 메모 날짜 칩 "2027년 10월 17일 (일)" (D-129) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoDetailPreview_OtherYear() =
    PreviewScreen(MemoDetailUiState(memo = Mine.copy(memoDate = MemoDateChip(2027, 10, 17, DayOfWeek.SUNDAY))))

/** `SCR-MEMO-03--delete-dialog.png`: 삭제 확인 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoDetailPreview_DeleteDialog() = PreviewScreen(MemoDetailUiState(memo = Mine, deleteDialogVisible = true))

/** 핀·별·삭제 요청 중: 로딩 덮개 (D-115) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoDetailPreview_Loading() = PreviewScreen(MemoDetailUiState(memo = Mine, writing = true))
