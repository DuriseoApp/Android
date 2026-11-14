package com.duriseo.android.data.repository.notification

import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.data.local.notification.NotificationLocalDataSource
import com.duriseo.android.data.remote.notification.NotificationRemoteDataSource
import com.duriseo.android.data.repository.cache.LocalCacheGeneration
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.notification.NotificationList
import com.duriseo.android.domain.notification.NotificationRepository
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 알림 목록: 화면은 Room만 구독하고, 서버 응답은 Room에 넣은 뒤 Flow로 보인다 (ARCHITECTURE.md 2장).
 * 응답 배열의 위치를 그대로 저장하고 다시 정렬하지 않는다. 읽음은 기기에서 먼저 바꾸고 서버 요청은 뒤에서 보낸다 (D-127).
 *
 * 락 순서는 메모·기념일과 같다: 목록 락([listLock]) -> `LocalCacheGeneration`의 Mutex (맨 안쪽).
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val remote: NotificationRemoteDataSource,
    private val local: NotificationLocalDataSource,
    private val networkMonitor: NetworkMonitor,
    private val cache: LocalCacheGeneration,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) : NotificationRepository {

    /** 첫 페이지·다음 페이지 요청이 겹쳐 이어 붙이기가 두 번 일어나지 않게 한다 */
    private val listLock = Mutex()

    @Volatile
    private var listOpen = false

    override val isListOpen: Boolean get() = listOpen

    override fun setListOpen(open: Boolean) {
        listOpen = open
    }

    /** 만든 지 30일이 지난 알림의 경계: 이 시각보다 뒤에 만든 것만 보인다 (D-141, `created_at > now − 30일`) */
    private fun cutoffMillis(): Long = clock.instant().minus(RETENTION).toEpochMilli()

    override fun observeNotifications(): Flow<NotificationList> =
        combine(local.observeAll(), local.observeListState()) { rows, state ->
            val cutoff = cutoffMillis()
            NotificationList(
                items = rows.filter { it.createdAt > cutoff }.mapNotNull { it.toDomainOrNull() },
                hasMore = state?.nextCursor != null,
                isLoaded = state != null,
            )
        }

    /**
     * 안 읽은 개수 (D-141, FR-NOTI-05 AC7). 온라인이고 서버가 알려 준 개수가 있으면 그 값이고,
     * 오프라인이거나 아직 못 받았으면 기기에 저장된 30일 안의 안 읽은 알림 개수다.
     */
    override fun observeUnreadCount(): Flow<Int> =
        combine(local.observeAll(), local.observeUnreadState(), networkMonitor.isOnline) { rows, state, online ->
            val server = state?.unreadCount
            if (online && server != null) {
                server
            } else {
                val cutoff = cutoffMillis()
                rows.count { !it.isRead && it.createdAt > cutoff }
            }
        }.distinctUntilChanged()

    /** 기기에 저장된 30일 지난 알림을 지운다 (D-141). 실패해도 읽기·표시는 계속한다 */
    private suspend fun pruneOld() {
        try {
            local.deleteOlderThan(cutoffMillis())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "오래된 알림 정리에 실패했어요: ${e.javaClass.simpleName}")
        }
    }

    override suspend fun refreshFirstPage(): DataResult<Unit> {
        pruneOld()
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return listLock.withLock {
            // 요청 전에 캐시 세대를 적어 두고, 응답이 오기 전에 캐시가 지워졌으면 저장하지 않는다
            val generation = cache.current()
            when (val result = remote.list()) {
                is DataResult.Success -> {
                    val page = result.value
                    cache.saveIfCurrent(generation) {
                        local.replaceFirstPage(page.items.mapIndexed { i, n -> n.toEntity(i) }, page.nextCursor)
                    }
                    DataResult.Success(Unit)
                }

                is DataResult.Failure -> result
            }
        }
    }

    override suspend fun loadMore(): DataResult<Unit> {
        pruneOld()
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return listLock.withLock {
            // 잠금 안에서 읽어 앞선 요청이 끝난 뒤의 커서를 쓴다. 없으면 마지막 페이지다 (세대는 커서를 읽기 전에 적는다)
            val generation = cache.current()
            val cursor = local.nextCursor() ?: return@withLock DataResult.Success(Unit)
            when (val result = remote.list(cursor = cursor)) {
                is DataResult.Success -> {
                    val page = result.value
                    cache.saveIfCurrent(generation) { local.appendPage(page.items.map { it.toEntity() }, page.nextCursor) }
                    DataResult.Success(Unit)
                }

                is DataResult.Failure -> result
            }
        }
    }

    override suspend fun refreshUnreadCount(): DataResult<Unit> {
        pruneOld()
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        val generation = cache.current()
        return when (val result = remote.unreadCount()) {
            is DataResult.Success -> {
                cache.saveIfCurrent(generation) { local.setUnreadCount(result.value) }
                DataResult.Success(Unit)
            }

            is DataResult.Failure -> result
        }
    }

    override suspend fun markRead(id: String) {
        // 기기에서 먼저 읽음으로 바꾼다. Room 실패가 이동을 막지 않게 삼킨다 (D-127)
        try {
            local.markRead(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "읽음 반영에 실패했어요: ${e.javaClass.simpleName}")
        }
        // 서버 요청은 뒤에서. 오프라인이면 보내지 않고, 실패는 안내 없이 넘긴다 (다음 불러오기가 서버 값으로 맞춘다)
        if (!networkMonitor.isOnlineNow()) return
        scope.launch {
            try {
                if (remote.markRead(id) is DataResult.Success) refreshUnreadCount()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(TAG, "읽음 요청에 실패했어요: ${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        /** 알림 보관 기간 (D-141) */
        val RETENTION: Duration = Duration.ofDays(30)
        const val TAG = "notification"
    }
}
