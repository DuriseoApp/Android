package com.duriseo.android.feature.memo.detail

import androidx.annotation.StringRes
import com.duriseo.android.domain.format.MemoDateChip
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.feature.memo.component.MemoAuthor

/**
 * 상세에 그리는 메모 한 건.
 *
 * @property title 제목. 없으면 null이고 제목 줄을 그리지 않는다 (목록의 "본문 첫 줄" 규칙 D-131은 목록·카드용이다)
 * @property body 본문 전체. 없으면 null
 * @property canEdit 내가 만든 메모만 true: [수정]·[메모 삭제]가 보인다 (FR-MEMO-03 AC1·AC2, PRD 7장)
 */
data class MemoDetailUi(
    val id: String,
    val title: String?,
    val body: String?,
    val author: MemoAuthor,
    val nickname: String,
    val writtenAt: WrittenAt,
    val memoDate: MemoDateChip?,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val canEdit: Boolean,
)

/**
 * SCR-MEMO-03 메모 상세 (FR-MEMO-03, FR-MEMO-05, FR-MEMO-06).
 *
 * @property deleting 삭제 요청이 도는 동안(성공해도 닫힐 때까지) true: 다이얼로그 버튼을 막는다
 * @property deleteLoading 삭제 요청이 도는 동안만 true: 로딩 덮개 (D-115)
 * @property writing 핀·별 요청이 도는 동안 true: 로딩 덮개 (D-115)
 */
data class MemoDetailUiState(
    val memo: MemoDetailUi? = null,
    val deleteDialogVisible: Boolean = false,
    val deleting: Boolean = false,
    val deleteLoading: Boolean = false,
    val writing: Boolean = false,
) {
    val loading: Boolean get() = deleteLoading || writing
}

sealed interface MemoDetailAction {
    /** 화면이 보이기 시작했다: `GET /memos/{id}`로 다시 불러온다. 404면 D-121 */
    data object Shown : MemoDetailAction
    data object BackClicked : MemoDetailAction
    data object EditClicked : MemoDetailAction
    data object PinClicked : MemoDetailAction
    data object FavoriteClicked : MemoDetailAction
    data object DeleteClicked : MemoDetailAction
    data object DeleteCancelled : MemoDetailAction
    data object DeleteConfirmed : MemoDetailAction
}

sealed interface MemoDetailSideEffect {
    data object Close : MemoDetailSideEffect

    /** 수정 (SCR-MEMO-04) */
    data class OpenEdit(val memoId: String) : MemoDetailSideEffect

    data class ShowMessage(@StringRes val messageRes: Int) : MemoDetailSideEffect
}
