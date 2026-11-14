package com.duriseo.android.data.repository.notification

import com.duriseo.android.data.local.notification.NotificationPreferencesLocalDataSource
import com.duriseo.android.data.remote.me.MeRemoteDataSource
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.notification.NotificationSettingKind
import com.duriseo.android.domain.notification.NotificationSettings
import com.duriseo.android.domain.notification.NotificationSettingsRepository
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 설정 (API.md 3.1, D-145). 화면은 기기에 저장된 마지막 서버 값만 본다. 서버 응답은 저장한 뒤 Flow로 보인다.
 * `PATCH`는 낙관적 갱신 없이 성공한 뒤에만 저장하므로 실패하면 스위치는 원래 값 그대로다 (FR-NOTI-02 AC2).
 */
@Singleton
class NotificationSettingsRepositoryImpl @Inject constructor(
    private val remote: MeRemoteDataSource,
    private val local: NotificationPreferencesLocalDataSource,
    private val networkMonitor: NetworkMonitor,
) : NotificationSettingsRepository {

    override fun observe(): Flow<NotificationSettings?> = local.observeSettings()

    override suspend fun refresh(): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return remote.getNotificationSettings().saveThenUnit()
    }

    override suspend fun update(kind: NotificationSettingKind, enabled: Boolean): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.patchNotificationSetting(kind, enabled) }.saveThenUnit()

    private suspend fun DataResult<NotificationSettings>.saveThenUnit(): DataResult<Unit> = when (this) {
        is DataResult.Success -> {
            local.saveSettings(value)
            DataResult.Success(Unit)
        }

        is DataResult.Failure -> this
    }
}
