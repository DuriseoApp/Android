package com.duriseo.android.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.feature.calendar.edit.EditKind
import com.duriseo.android.feature.calendar.edit.EventEditMemoTab
import com.duriseo.android.feature.calendar.edit.EventEditScreen
import com.duriseo.android.feature.calendar.edit.EventEditUiState
import com.duriseo.android.feature.memo.edit.MemoDateSheet
import com.duriseo.android.feature.memo.edit.MemoEditAction
import com.duriseo.android.feature.memo.edit.MemoEditSideEffect
import com.duriseo.android.feature.memo.edit.MemoEditUiState
import com.duriseo.android.feature.memo.edit.MemoEditViewModel
import com.duriseo.android.feature.memo.edit.MemoForm
import java.time.LocalDate

// SCR-CAL-03의 [메모] 탭 슬롯 (D-133, unit-6.md 확인 필요 3). `feature:calendar`는 `feature:memo`를 모르므로 앱이 두 화면 모듈을 이어 준다.

/**
 * 새 일정 작성 화면의 내비게이션 항목 안에서 불러 [MemoEditViewModel]을 그 항목에 둔다(일정 ViewModel과 같은 항목이라 탭을 오가도 입력이 남는다).
 * 이 항목의 인자에는 `memoId`가 없어서 새 메모이고 메모 날짜는 비어 있다: 캘린더에서 고른 날(`date`)로 미리 채우지 않는다 (D-133).
 * 메모를 저장해 성공하면 작성 화면을 닫는다.
 */
@Composable
internal fun rememberEventEditMemoTab(
    onClose: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
): EventEditMemoTab {
    val viewModel: MemoEditViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val close by rememberUpdatedState(onClose)
    val showSnackBar by rememberUpdatedState(onShowSnackBar)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MemoEditSideEffect.Close, MemoEditSideEffect.CloseWithDetail -> close()
                is MemoEditSideEffect.ShowMessage -> showSnackBar(effect.messageRes)
            }
        }
    }
    return memoTabOf(state = state, onAction = viewModel::onAction)
}

/** [MemoEditUiState]와 액션으로 슬롯 값을 만든다. 저장 조건·로딩은 메모 쪽 상태 그대로다 */
internal fun memoTabOf(state: MemoEditUiState, onAction: (MemoEditAction) -> Unit) = EventEditMemoTab(
    saveEnabled = state.saveEnabled,
    loading = state.loading,
    dateSheetVisible = state.dateSheet != null,
    onSave = { onAction(MemoEditAction.SaveClicked) },
    form = { modifier -> if (state.loaded) MemoForm(state = state, onAction = onAction, modifier = modifier) },
    dateSheet = { MemoDateSheet(state = state, onAction = onAction) },
)

private val PreviewDay = LocalDate.of(2026, 10, 12)

/** `SCR-MEMO-04.png`: 새로 추가의 [메모] 탭 (종류 탭에서 메모가 고른 상태, 날짜 없음, 알림 스위치 켜짐) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventEditPreview_MemoTab() {
    val memoState = MemoEditUiState(loaded = true, isCouple = true, today = PreviewDay)
    DuriseoTheme {
        EventEditScreen(
            state = EventEditUiState(loaded = true, isCouple = true, kind = EditKind.Memo, today = PreviewDay),
            onAction = {},
            memoTab = memoTabOf(state = memoState, onAction = {}),
        )
    }
}
