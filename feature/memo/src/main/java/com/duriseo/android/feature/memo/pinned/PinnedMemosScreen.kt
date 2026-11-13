package com.duriseo.android.feature.memo.pinned

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.duriseo.android.core.designsystem.component.DuriseoLoadingLayer
import com.duriseo.android.core.designsystem.component.DuriseoTopAppBar
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.domain.format.MemoDateChip
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.feature.memo.R
import com.duriseo.android.feature.memo.component.MemoAuthor
import com.duriseo.android.feature.memo.component.MemoItemUi
import com.duriseo.android.feature.memo.component.PinCard
import java.time.DayOfWeek

// SCR-MEMO-02 고정 메모 전체 (FR-MEMO-05). 수치는 보드 `PinnedAll.dc.html`: 목록 안쪽 위 4·좌우 20·아래 28, 카드 사이 10,
// 안내 아래 4 더, 비었을 때 문구는 위아래 32에 가운데 정렬 14/1.6.

private val ListTop = DuriseoSpacing.Space1
private val ListBottom = 28.dp
private val CardGap = 10.dp
private val DescriptionBottom = DuriseoSpacing.Space1
private val EmptyPaddingVertical = DuriseoSpacing.Space8
private val EmptyLineHeight = 22.4.sp

@Composable
fun PinnedMemosRoute(
    lifecycleOwner: LifecycleOwner,
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpenDetail: (memoId: String) -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PinnedMemosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.onAction(PinnedMemosAction.Shown)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PinnedMemosSideEffect.Back -> onBack()
                is PinnedMemosSideEffect.OpenDetail -> onOpenDetail(effect.memoId)
                is PinnedMemosSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    PinnedMemosScreen(state = state, onAction = viewModel::onAction, padding = padding, modifier = modifier)
}

@Composable
fun PinnedMemosScreen(
    state: PinnedMemosUiState,
    onAction: (PinnedMemosAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    DuriseoLoadingLayer(loading = state.loading, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().background(Surface0).padding(padding)) {
            DuriseoTopAppBar(
                title = stringResource(R.string.memo_pinned_title, state.items.size),
                onBack = { onAction(PinnedMemosAction.BackClicked) },
            )
            if (state.loaded) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = DuriseoSpacing.Space5,
                        end = DuriseoSpacing.Space5,
                        top = ListTop,
                        bottom = ListBottom,
                    ),
                    verticalArrangement = Arrangement.spacedBy(CardGap),
                ) {
                    item(key = "description") {
                        Text(
                            text = stringResource(R.string.memo_pinned_description),
                            modifier = Modifier.padding(bottom = DescriptionBottom),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                            color = Ink600,
                        )
                    }
                    items(state.items, key = { it.id }) { item ->
                        // 핀 버튼만 있다 (별 없음)
                        PinCard(
                            item = item,
                            onOpen = { onAction(PinnedMemosAction.ItemClicked(item)) },
                            onPinClick = { onAction(PinnedMemosAction.UnpinClicked(item)) },
                            onFavoriteClick = null,
                        )
                    }
                    if (state.isEmpty) {
                        item(key = "empty") {
                            Text(
                                text = stringResource(R.string.memo_pinned_empty),
                                modifier = Modifier.fillMaxWidth().padding(vertical = EmptyPaddingVertical),
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = EmptyLineHeight),
                                color = Ink600,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Preview. 견본은 보드 `PinnedAll.dc.html`와 같다 ---

private val Samples = listOf(
    MemoItemUi(
        id = "2", title = "장보기", preview = "우유, 계란, 두부, 대파, 사과", author = MemoAuthor.PARTNER, nickname = "[상대 이름]",
        writtenAt = WrittenAt.Today(isPm = false, hour12 = 8, minute = 40), memoDate = null, isPinned = true, isFavorite = false, canEdit = false,
    ),
    MemoItemUi(
        id = "1", title = "여행 숙소 후보 정리했어", preview = "세 곳 골라놨는데 주말 전까지 하나 골라줘!", author = MemoAuthor.PARTNER,
        nickname = "[상대 이름]", writtenAt = WrittenAt.Today(isPm = false, hour12 = 9, minute = 12),
        memoDate = MemoDateChip(null, 10, 16, DayOfWeek.FRIDAY), isPinned = true, isFavorite = false, canEdit = false,
    ),
    MemoItemUi(
        id = "5", title = "주말 집안일 분담", preview = "토요일 장보기는 내가, 일요일 청소는 같이 하기", author = MemoAuthor.ME, nickname = "[내 이름]",
        writtenAt = WrittenAt.ThisYear(10, 8), memoDate = null, isPinned = true, isFavorite = false, canEdit = true,
    ),
    MemoItemUi(
        id = "8", title = "이번 달 데이트 통장 정리", preview = "이번 달은 여행 때문에 조금 더 넣기로 했어요. 금액은 [금액]으로 맞춰요.",
        author = MemoAuthor.ME, nickname = "[내 이름]", writtenAt = WrittenAt.ThisYear(10, 1), memoDate = null,
        isPinned = true, isFavorite = true, canEdit = true,
    ),
)

@Composable
private fun PreviewScreen(state: PinnedMemosUiState) {
    DuriseoTheme { PinnedMemosScreen(state = state, onAction = {}) }
}

/** `SCR-MEMO-02.png`: 고정 4개, 최근 고정순 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PinnedMemosPreview_Default() = PreviewScreen(PinnedMemosUiState(loaded = true, items = Samples))

/** 비었을 때: "고정된 메모가 없어요." */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PinnedMemosPreview_Empty() = PreviewScreen(PinnedMemosUiState(loaded = true))

/** 고정 해제 요청 중: 로딩 덮개 (D-115) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PinnedMemosPreview_Loading() = PreviewScreen(PinnedMemosUiState(loaded = true, items = Samples, writing = true))
