package com.duriseo.android.data.repository.event

import com.duriseo.android.data.local.event.EventLocalDataSource
import com.duriseo.android.data.remote.event.EventRemoteDataSource
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.event.Event
import com.duriseo.android.domain.event.EventPatch
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.event.EventRules
import com.duriseo.android.domain.event.NewEvent
import com.duriseo.android.domain.event.ReminderOption
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 일정: 화면은 Room만 구독하고, 서버 응답은 Room에 넣은 뒤 Flow로 보인다 (ARCHITECTURE.md 2장).
 * 쓰기는 [runOnlineWrite]로 감싸 오프라인이면 요청하지 않고, 성공하면 응답으로 Room을 바꾼다 (낙관적 갱신 없음).
 */
@Singleton
class EventRepositoryImpl @Inject constructor(
    private val remote: EventRemoteDataSource,
    private val local: EventLocalDataSource,
    private val networkMonitor: NetworkMonitor,
) : EventRepository {

    override fun observeEvents(from: LocalDate, to: LocalDate, zone: ZoneId): Flow<List<Event>> =
        local.observeRange(from, to, zone).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observeEvent(id: String): Flow<Event?> = local.observe(id).map { it?.toDomainOrNull() }

    override suspend fun refreshRange(from: LocalDate, to: LocalDate, zone: ZoneId): DataResult<Unit> {
        // 1950-01-01 ~ 2050-12-31 안으로 자른다. 잘라낸 구간이 비면 요청하지 않는다 (R-15)
        val start = if (from.isBefore(EventRules.MIN_DATE)) EventRules.MIN_DATE else from
        val end = if (to.isAfter(EventRules.MAX_DATE)) EventRules.MAX_DATE else to
        if (start.isAfter(end)) return DataResult.Success(Unit)
        // 읽기라서 쓰기 규칙은 아니지만 오프라인이면 요청 없이 조용히 실패한다 (NFR-OFF-01)
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return when (val result = remote.list(start, end)) {
            is DataResult.Success -> {
                local.replaceRange(start, end, zone, result.value.map { it.toEntity() })
                DataResult.Success(Unit)
            }

            is DataResult.Failure -> result
        }
    }

    override suspend fun refreshEvent(id: String): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return remote.get(id).thenSave().dropValue().thenForgetIfGone(id)
    }

    override suspend fun create(event: NewEvent): DataResult<Event> =
        networkMonitor.runOnlineWrite { remote.create(event) }.thenSave()

    override suspend fun update(id: String, patch: EventPatch): DataResult<Unit> {
        if (patch.isEmpty) return DataResult.Success(Unit)
        return networkMonitor.runOnlineWrite { remote.update(id, patch) }
            .thenSave()
            .dropValue()
            .thenForgetIfGone(id)
            .thenRereadIfNotAuthor(id)
    }

    override suspend fun delete(id: String): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.delete(id) }
            .also { if (it is DataResult.Success) local.delete(id) }
            .thenForgetIfGone(id)
            .thenRereadIfNotAuthor(id)

    override suspend fun setReminder(id: String, option: ReminderOption?): DataResult<Unit> =
        if (option == null) {
            networkMonitor.runOnlineWrite { remote.deleteReminder(id) }
                .also { if (it is DataResult.Success) clearReminderLocally(id) }
                .thenForgetIfGone(id)
        } else {
            networkMonitor.runOnlineWrite { remote.putReminder(id, option) }
                .thenSave()
                .dropValue()
                .thenForgetIfGone(id)
        }

    private suspend fun clearReminderLocally(id: String) {
        val row = local.get(id) ?: return
        local.upsert(row.copy(reminderOption = null, reminderFireAt = null))
    }

    private suspend fun DataResult<Event>.thenSave(): DataResult<Event> {
        if (this is DataResult.Success) local.upsert(value.toEntity())
        return this
    }

    private fun <T> DataResult<T>.dropValue(): DataResult<Unit> = when (this) {
        is DataResult.Success -> DataResult.Success(Unit)
        is DataResult.Failure -> this
    }

    /** 404 `NOT_FOUND`: 없거나 다른 공간의 일정이다. Room에서도 지운다 (ARCHITECTURE.md 5장, R-01) */
    private suspend fun <T> DataResult<T>.thenForgetIfGone(id: String): DataResult<T> {
        if (this is DataResult.Failure && error.hasCode(ErrorCode.NOT_FOUND)) local.delete(id)
        return this
    }

    /** 403 `NOT_AUTHOR`: 낡은 권한 표시일 수 있어 그 일정을 다시 읽는다 (실패해도 결과는 그대로) */
    private suspend fun <T> DataResult<T>.thenRereadIfNotAuthor(id: String): DataResult<T> {
        if (this is DataResult.Failure && error.hasCode(ErrorCode.NOT_AUTHOR)) refreshEvent(id)
        return this
    }
}
