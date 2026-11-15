package com.duriseo.android.data.repository.notification

import com.duriseo.android.data.local.notification.NotificationPreferencesLocalDataSource
import com.duriseo.android.domain.notification.NotificationPermissionGate
import javax.inject.Inject
import javax.inject.Singleton

/** "이미 물었는지" 기록 하나를 세 시점이 함께 쓴다 (FR-NOTI-01 AC3, D-144). 설정 DataStore에 둔다 */
@Singleton
class NotificationPermissionGateImpl @Inject constructor(
    private val preferences: NotificationPreferencesLocalDataSource,
) : NotificationPermissionGate {
    override suspend fun shouldAskNow(): Boolean = preferences.markPermissionAskedIfFirst()
}
