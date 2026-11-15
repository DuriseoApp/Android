package com.duriseo.android.feature.notification.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.NotificationRouteModel
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.feature.notification.list.NotificationListRoute

/** 알림 목록 (SCR-NOTI-01). SCR-CAL-01 앱바의 알림 버튼이 부른다 */
fun NavController.navigateToNotifications(navOptions: NavOptions? = null) {
    navigate(NotificationRouteModel.Notifications, navOptions)
}

/**
 * @param onBack 앱바 뒤로 가기
 * @param onOpenDestination 항목을 눌러 읽음 처리한 뒤 갈 곳. 목록 위에 쌓는다 (일정·메모 상세, 기념일 목록, 설정)
 * @param onShowSnackBar 삭제된 대상 안내 (스낵바는 위에서 40, D-126)
 */
fun NavGraphBuilder.notificationNavGraph(
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpenDestination: (NotificationDestination) -> Unit,
    onShowSnackBar: (Int) -> Unit,
) {
    composable<NotificationRouteModel.Notifications> { entry ->
        NotificationListRoute(
            lifecycleOwner = entry,
            padding = padding,
            onBack = onBack,
            onOpenDestination = onOpenDestination,
            onShowSnackBar = onShowSnackBar,
        )
    }
}
