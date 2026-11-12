package com.duriseo.android.feature.memo.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.feature.memo.R
import java.time.LocalDate
import java.time.YearMonth

// SCR-MEMO-04 메모 작성·수정 화면 (FR-MEMO-02, FR-MEMO-03). 앱바·[저장]·로딩 덮개·날짜 시트를 바깥에서 그리고 입력은 [MemoForm]이 그린다.
// 새로 쓰기에서 SCR-CAL-03의 [일정|메모] 탭이 필요한 진입은 묶음 C가 `feature:main`에서 같은 부품([MemoForm]·[MemoDateSheet])으로 짠다.
// 이 화면은 종류 탭이 없다: 수정은 문서대로이고, 메모 탭의 FAB에서 오는 새로 쓰기도 지금은 탭 없이 연다 (unit-6.md 보고).

@Composable
fun MemoEditRoute(
    padding: PaddingValues,
    onClose: () -> Unit,
    onCloseWithDetail: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MemoEditSideEffect.Close -> onClose()
                MemoEditSideEffect.CloseWithDetail -> onCloseWithDetail()
                is MemoEditSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    MemoEditScreen(state = state, onAction = viewModel::onAction, padding = padding, modifier = modifier)
}

@Composable
fun MemoEditScreen(
    state: MemoEditUiState,
    onAction: (MemoEditAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    DuriseoDialogLayer(
        dialogVisible = state.dateSheet != null,
        dialog = { MemoDateSheet(state = state, onAction = onAction) },
        modifier = modifier,
        loading = state.loading,
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Surface0).padding(padding)) {
            EditHeader(state = state, onAction = onAction)
            if (state.loaded) {
                MemoForm(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = DuriseoSpacing.Space5, end = DuriseoSpacing.Space5, top = DuriseoSpacing.Space2, bottom = DuriseoSpacing.Space6),
                )
            }
        }
    }
}

/** 앱바: 높이 64, 닫기 48x48, 제목 17/700 가운데, [저장] 16/700 `brand-700`(비활성은 `ink-500`) (보드 `AddEvent.dc.html` 19~25행) */
@Composable
private fun EditHeader(state: MemoEditUiState, onAction: (MemoEditAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(horizontal = DuriseoSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onAction(MemoEditAction.CloseClicked) }, modifier = Modifier.size(DuriseoSize.ButtonMd)) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_close),
                contentDescription = stringResource(DesignR.string.nav_close),
                modifier = Modifier.size(DuriseoSize.Icon),
                tint = Ink900,
            )
        }
        Text(
            text = stringResource(if (state.isEdit) R.string.memo_edit_title_edit else R.string.memo_edit_title_new),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = Ink900,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .height(DuriseoSize.Touch)
                .width(DuriseoSize.ButtonMd)
                .clickable(enabled = state.saveEnabled, role = Role.Button) { onAction(MemoEditAction.SaveClicked) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.memo_edit_save),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                // 비활성 [저장]의 글자색은 문서에 없어 SCR-CAL-03과 같이 `ink-500`을 쓴다
                color = if (state.saveEnabled) Brand700 else Ink500,
            )
        }
    }
}

// --- Preview. 견본은 보드 `AddEventMemo.dc.html`·`AddEventMemoDate.dc.html`와 같다 ---

@Composable
private fun PreviewScreen(state: MemoEditUiState) {
    DuriseoTheme { MemoEditScreen(state = state, onAction = {}) }
}

private val PreviewBase = MemoEditUiState(loaded = true, isCouple = true, today = LocalDate.of(2026, 10, 12))

/** 기본: 새로 추가, 제목·본문 비어 [저장] 비활성, 날짜 없음, 알림 스위치 켜짐 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoEditPreview_Default() = PreviewScreen(PreviewBase)

/** `SCR-MEMO-04--date.png`: 날짜 추가됨 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoEditPreview_Date() = PreviewScreen(
    PreviewBase.copy(title = "주말에 살 것", memoDate = LocalDate.of(2026, 10, 17)),
)

/** 수정: 알림 스위치 없음, 제목 "메모 수정" */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoEditPreview_Edit() = PreviewScreen(
    PreviewBase.copy(isEdit = true, title = "선물 아이디어", body = "향수 같이 시향해보기, 지난번에 본 머플러\n생일 전 주말에 같이 보러 가기", memoDate = LocalDate.of(2026, 11, 20)),
)

/** 날짜 시트가 열림 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoEditPreview_DateSheet() = PreviewScreen(
    PreviewBase.copy(dateSheet = MemoDateSheetState(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 12))),
)

/** 저장 요청 중: 로딩 덮개 (D-115) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoEditPreview_Loading() = PreviewScreen(
    PreviewBase.copy(title = "주말에 살 것", saving = true, loading = true),
)
