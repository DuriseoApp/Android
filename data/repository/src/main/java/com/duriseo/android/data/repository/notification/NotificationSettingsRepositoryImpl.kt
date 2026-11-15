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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
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

    /**
     * 요청 번호. 요청을 보내기 전에 매기고, 응답은 그 뒤에 시작한 요청이 없을 때만 저장한다 (늦게 온 옛 응답이 새 값을 덮지 않게).
     * [latestAny]는 GET·PATCH 모두의 마지막 번호, [latestWrite]는 PATCH만의 마지막 번호다.
     */
    private val sequence = AtomicLong(0)
    private var latestAny = 0L
    private var latestWrite = 0L
    private val saveLock = Mutex()

    override suspend fun refresh(): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        val number = start(write = false)
        val result = remote.getNotificationSettings()
        // GET은 그 뒤에 GET이든 PATCH든 시작했으면 버린다: 응답이 PATCH 이전 값일 수 있다
        return result.saveThenUnit { latestAny == number }
    }

    override suspend fun update(kind: NotificationSettingKind, enabled: Boolean): DataResult<Unit> {
        var number = 0L
        val result = networkMonitor.runOnlineWrite {
            number = start(write = true)
            remote.patchNotificationSetting(kind, enabled)
        }
        // PATCH 응답은 서버의 전체 값이라 그 뒤에 시작한 GET이 있어도 저장한다. 더 새로운 PATCH가 있을 때만 버린다
        return result.saveThenUnit { latestWrite == number }
    }

    private suspend fun start(write: Boolean): Long = saveLock.withLock {
        val number = sequence.incrementAndGet()
        latestAny = number
        if (write) latestWrite = number
        number
    }

    private suspend fun DataResult<NotificationSettings>.saveThenUnit(isLatest: () -> Boolean): DataResult<Unit> =
        when (this) {
            is DataResult.Success -> {
                saveLock.withLock { if (isLatest()) local.saveSettings(value) }
                DataResult.Success(Unit)
            }

            is DataResult.Failure -> this
        }
}
