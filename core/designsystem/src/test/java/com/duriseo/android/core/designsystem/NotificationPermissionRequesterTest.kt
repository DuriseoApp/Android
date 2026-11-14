package com.duriseo.android.core.designsystem

import com.duriseo.android.core.designsystem.permission.needsNotificationPermissionRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionRequesterTest {
    @Test
    fun `FR-NOTI-01 AC1 알림 권한은 Android 13 이상에서만 요청한다`() {
        assertFalse(needsNotificationPermissionRequest(32))
        assertTrue(needsNotificationPermissionRequest(33))
        assertTrue(needsNotificationPermissionRequest(36))
    }
}
