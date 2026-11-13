package com.duriseo.android.feature.memo.pinned

import androidx.annotation.StringRes
import com.duriseo.android.feature.memo.component.MemoItemUi

/**
 * SCR-MEMO-02 고정 메모 전체 (FR-MEMO-05 AC3). 앱바 "고정된 메모 N"의 N은 [items]의 길이다.
 *
 * @property items 고정 메모 전체, 최근 고정순 (서버 순서 그대로)
 * @property loaded 내 정보를 읽었다
 * @property writing 고정 해제 요청이 도는 동안 true (로딩 덮개, D-115)
 */
data class PinnedMemosUiState(
    val loaded: Boolean = false,
    val items: List<MemoItemUi> = emptyList(),
    val writing: Boolean = false,
) {
    val loading: Boolean get() = writing

    /** "고정된 메모가 없어요." */
    val isEmpty: Boolean get() = loaded && items.isEmpty()
}

sealed interface PinnedMemosAction {
    data object Shown : PinnedMemosAction
    data object BackClicked : PinnedMemosAction
    data class ItemClicked(val item: MemoItemUi) : PinnedMemosAction

    /** 핀을 누르면 고정이 풀린다 (SCREENS SCR-MEMO-02 안내) */
    data class UnpinClicked(val item: MemoItemUi) : PinnedMemosAction
}

sealed interface PinnedMemosSideEffect {
    data object Back : PinnedMemosSideEffect
    data class OpenDetail(val memoId: String) : PinnedMemosSideEffect
    data class ShowMessage(@StringRes val messageRes: Int) : PinnedMemosSideEffect
}
