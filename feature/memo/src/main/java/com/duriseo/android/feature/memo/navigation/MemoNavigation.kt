package com.duriseo.android.feature.memo.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.core.common.navigation.MemoRouteModel
import com.duriseo.android.feature.memo.detail.MemoDetailRoute
import com.duriseo.android.feature.memo.edit.MemoEditRoute
import com.duriseo.android.feature.memo.list.MemoListRoute
import com.duriseo.android.feature.memo.pinned.PinnedMemosRoute

/** 고정 메모 전체 (SCR-MEMO-02). SCR-MEMO-01의 [고정 메모 전체 보기] */
fun NavController.navigateToPinnedMemos() {
    navigate(MemoRouteModel.Pinned)
}

/** 메모 상세 (SCR-MEMO-03). 메모 탭의 목록·핀 카드, 고정 메모 전체, (묶음 C) 캘린더 날짜 목록의 메모가 부른다 */
fun NavController.navigateToMemoDetail(memoId: String) {
    navigate(MemoRouteModel.Detail(memoId))
}

/** 메모 수정 (SCR-MEMO-04, 종류 탭 없음). 상세의 [수정]. 새 메모는 SCR-CAL-03의 [메모] 탭이다 (D-133) */
fun NavController.navigateToMemoEdit(memoId: String) {
    navigate(MemoRouteModel.Edit(memoId = memoId))
}

/**
 * 수정 화면과 상세를 함께 닫는다: 수정 중 서버가 그 메모가 없다고 답했을 때 (D-121, FR-MEMO-03 AC3).
 * 상세([MemoRouteModel.Detail])까지 포함해 되돌린다. 상세가 캘린더에서 열렸으면 캘린더로, 메모 탭에서 열렸으면 메모 탭으로 돌아간다.
 * 상세가 백스택에 없으면(직접 열린 수정) 수정 화면만 닫는다.
 */
fun NavController.closeMemoEditAndDetail() {
    if (!popBackStack<MemoRouteModel.Detail>(inclusive = true)) popBackStack()
}

/**
 * 메모 화면 그래프. 메모 탭 루트(SCR-MEMO-01, [MainRouteModel.Memo])와 하위 화면(SCR-MEMO-02·03·04)이다.
 *
 * @param onOpenDetail 목록·핀 카드·고정 메모 전체에서 메모를 눌렀을 때
 * @param onOpenPinned [고정 메모 전체 보기]
 * @param onOpenCreate 메모 탭의 FAB. 새 메모 화면은 이 모듈에 없고 SCR-CAL-03의 [메모] 탭이라 앱 쪽 이동에 맡긴다 (D-133)
 * @param onOpenEdit 상세의 [수정]
 * @param onBack 하위 화면의 뒤로 가기·닫기
 * @param onCloseEditAndDetail 수정이 404였을 때 수정 화면과 상세를 함께 닫는다 (`NavController.closeMemoEditAndDetail`)
 * @param snackbarVisible 스낵바가 떠 있는지. 메모 탭의 FAB가 스낵바 위로 올라가는 데 쓴다 (D-126)
 */
fun NavGraphBuilder.memoNavGraph(
    padding: PaddingValues,
    onOpenDetail: (memoId: String) -> Unit,
    onOpenPinned: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenEdit: (memoId: String) -> Unit,
    onBack: () -> Unit,
    onCloseEditAndDetail: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    snackbarVisible: Boolean = false,
) {
    composable<MainRouteModel.Memo> { entry ->
        MemoListRoute(
            lifecycleOwner = entry,
            padding = padding,
            onOpenDetail = onOpenDetail,
            onOpenPinned = onOpenPinned,
            onOpenCreate = onOpenCreate,
            onShowSnackBar = onShowSnackBar,
            snackbarVisible = snackbarVisible,
        )
    }
    composable<MemoRouteModel.Pinned> { entry ->
        PinnedMemosRoute(
            lifecycleOwner = entry,
            padding = padding,
            onBack = onBack,
            onOpenDetail = onOpenDetail,
            onShowSnackBar = onShowSnackBar,
        )
    }
    composable<MemoRouteModel.Detail> { entry ->
        MemoDetailRoute(
            lifecycleOwner = entry,
            padding = padding,
            onClose = onBack,
            onOpenEdit = onOpenEdit,
            onShowSnackBar = onShowSnackBar,
        )
    }
    composable<MemoRouteModel.Edit> {
        MemoEditRoute(
            padding = padding,
            onClose = onBack,
            onCloseWithDetail = onCloseEditAndDetail,
            onShowSnackBar = onShowSnackBar,
        )
    }
}
