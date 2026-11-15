package com.duriseo.android.core.designsystem.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

// 알림 권한 요청의 Android 쪽 공용 부품 (FR-NOTI-01, D-70, D-144).
// ViewModel은 "권한 요청" 효과만 내고(이미 물었는지는 `NotificationPermissionGate`), 실제 요청은 여기서 한다.
// 요청 결과는 쓰지 않는다: 거부해도 다시 묻거나 안내하지 않는다 (FR-NOTI-01 AC3).

/** 알림 권한을 요청한다. Android 13 미만이거나 이미 허용돼 있으면 아무것도 하지 않는다 */
fun interface NotificationPermissionRequester {
    fun request()
}

/** 아무것도 하지 않는 요청기. 앱 맨 위층이 [ProvideNotificationPermissionRequester]로 채우기 전의 기본값이고 Preview에서 쓴다 */
val NoOpNotificationPermissionRequester = NotificationPermissionRequester { }

/**
 * 화면이 `LocalNotificationPermissionRequester.current.request()`로 부른다. 앱 맨 위층(`MainScreen`)이 한 번 제공한다.
 * 요청을 맨 위층의 런처로 하므로 요청 직후 화면이 닫혀도(직접 기념일 저장 뒤 등) 요청이 끊기지 않는다.
 */
val LocalNotificationPermissionRequester = staticCompositionLocalOf { NoOpNotificationPermissionRequester }

/** 앱 맨 위층에서 [content] 전체에 알림 권한 요청기를 제공한다 */
@Composable
fun ProvideNotificationPermissionRequester(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // 결과는 쓰지 않는다 (거부해도 안내 없음)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val requester = remember(context, launcher) {
        NotificationPermissionRequester {
            if (needsNotificationPermissionRequest(Build.VERSION.SDK_INT) &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    CompositionLocalProvider(LocalNotificationPermissionRequester provides requester, content = content)
}

/** `POST_NOTIFICATIONS`는 Android 13(API 33)부터 런타임 권한이다 (FR-NOTI-01 AC1) */
internal fun needsNotificationPermissionRequest(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU
