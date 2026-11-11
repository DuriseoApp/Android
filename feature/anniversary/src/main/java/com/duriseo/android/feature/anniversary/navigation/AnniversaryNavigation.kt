package com.duriseo.android.feature.anniversary.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.AnniversaryRouteModel
import com.duriseo.android.feature.anniversary.edit.AnniversaryEditRoute
import com.duriseo.android.feature.anniversary.list.AnniversaryListRoute
import java.time.LocalDate

/** 기념일 목록 (SCR-ANNIV-01). 캘린더 앱바의 "함께한 지 D+N"·"기념일 ›"·날짜 목록의 기념일 카드가 부른다 (단위 5 묶음 B) */
fun NavController.navigateToAnniversaries() {
    navigate(AnniversaryRouteModel.Anniversaries)
}

/** 새 직접 기념일 (SCR-ANNIV-02) */
fun NavController.navigateToAnniversaryCreate() {
    navigate(AnniversaryRouteModel.Edit())
}

/** 내 직접 기념일 수정 (SCR-ANNIV-02). [date]는 누른 항목의 날짜다 (unit-5.md 확인 필요 1) */
fun NavController.navigateToAnniversaryEdit(anniversaryId: String, date: LocalDate) {
    navigate(AnniversaryRouteModel.Edit(anniversaryId = anniversaryId, date = date.toString()))
}

/**
 * @param onOpenCreate 목록의 추가 버튼·빈 화면 [기념일 추가]
 * @param onOpenEdit 내가 만든 직접 기념일(다가오는·지난)을 눌렀을 때
 */
fun NavGraphBuilder.anniversaryNavGraph(
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenEdit: (anniversaryId: String, date: LocalDate) -> Unit,
    onShowSnackBar: (Int) -> Unit,
) {
    composable<AnniversaryRouteModel.Anniversaries> {
        AnniversaryListRoute(
            padding = padding,
            onBack = onBack,
            onOpenCreate = onOpenCreate,
            onOpenEdit = onOpenEdit,
            onShowSnackBar = onShowSnackBar,
        )
    }
    composable<AnniversaryRouteModel.Edit> {
        AnniversaryEditRoute(
            padding = padding,
            onClose = onBack,
            onShowSnackBar = onShowSnackBar,
        )
    }
}
