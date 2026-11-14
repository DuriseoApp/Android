package com.duriseo.android.core.designsystem.theme

import androidx.compose.ui.graphics.toArgb

/**
 * 상태바 알림의 강조색(작은 아이콘 색)으로 쓰는 brand-500 (ARGB). 알림은 Compose가 아니라
 * `NotificationCompat`이 그려서 `Color` 대신 Int로 준다 (`core:push`). 값은 [Brand500] 그대로다.
 */
val NotificationAccentArgb: Int get() = Brand500.toArgb()
