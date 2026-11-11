package com.duriseo.android.data.repository.anniversary

import com.duriseo.android.data.local.anniversary.AnniversaryLocalDataSource
import com.duriseo.android.data.local.anniversary.LIST_RANGE
import com.duriseo.android.data.local.anniversary.LIST_UPCOMING
import com.duriseo.android.core.common.Logger
import com.duriseo.android.data.remote.anniversary.AnniversaryRemoteDataSource
import com.duriseo.android.data.repository.cache.LocalCacheGeneration
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.anniversary.Anniversary
import com.duriseo.android.domain.anniversary.AnniversaryOccurrence
import com.duriseo.android.domain.anniversary.AnniversaryPatch
import com.duriseo.android.domain.anniversary.AnniversaryRepository
import com.duriseo.android.domain.anniversary.AnniversaryRules
import com.duriseo.android.domain.anniversary.NewAnniversary
import com.duriseo.android.domain.anniversary.PastAnniversaries
import com.duriseo.android.domain.anniversary.UpcomingAnniversaries
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 기념일: 화면은 Room만 구독하고, 서버 응답은 Room에 넣은 뒤 Flow로 보인다 (ARCHITECTURE.md 2장).
 * 응답 배열의 위치를 그대로 저장하고 다시 정렬하지 않는다 (API.md "리드 보충 (순서 사용)").
 * 쓰기는 [runOnlineWrite]로 감싸 오프라인이면 요청하지 않고, 성공하면 목록과 달을 다시 불러온다 (낙관적 갱신 없음).
 */
@Singleton
class AnniversaryRepositoryImpl @Inject constructor(
    private val remote: AnniversaryRemoteDataSource,
    private val local: AnniversaryLocalDataSource,
    private val networkMonitor: NetworkMonitor,
    private val cache: LocalCacheGeneration,
) : AnniversaryRepository {

    /**
     * 캘린더가 알려 준 보이는 범위 ([setVisibleRange]). 쓰기가 성공하면 이 범위를 다시 불러온다.
     * 알린 뒤 캐시가 지워졌으면(세대가 바뀜) 비운 것으로 본다.
     */
    private class VisibleRange(val from: LocalDate, val to: LocalDate, val generation: Long)

    @Volatile
    private var visibleRange: VisibleRange? = null

    /** 저장한 범위 응답의 요청 번호. 겹치는 더 새 응답이 이미 저장됐으면 오래된 응답으로 덮지 않는다 */
    private class SavedRange(val seq: Long, val from: LocalDate, val to: LocalDate, val generation: Long)

    private val rangeSeq = AtomicLong(0)
    private val rangeLock = Mutex()
    private var lastSavedRange: SavedRange? = null // rangeLock 아래에서만 읽고 쓴다

    /** 같은 목록의 첫 페이지·다음 페이지 요청이 겹쳐 이어 붙이기가 두 번 일어나지 않게 한다 */
    private val upcomingLock = Mutex()
    private val pastLock = Mutex()

    override fun setVisibleRange(from: LocalDate, to: LocalDate) {
        visibleRange = VisibleRange(from, to, cache.current())
    }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<AnniversaryOccurrence>> =
        local.observeRange(from, to).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observeUpcoming(): Flow<UpcomingAnniversaries> =
        combine(local.observeUpcoming(), local.observeUpcomingState()) { rows, state ->
            UpcomingAnniversaries(rows.mapNotNull { it.toDomainOrNull() }, hasMore = state?.nextCursor != null)
        }

    override fun observePast(): Flow<PastAnniversaries> =
        combine(local.observePast(), local.observePastState()) { rows, state ->
            PastAnniversaries(
                items = rows.mapNotNull { it.toDomainOrNull() },
                totalCount = state?.totalCount ?: 0,
                hasMore = state?.nextCursor != null,
            )
        }

    override suspend fun refreshRange(from: LocalDate, to: LocalDate): DataResult<Unit> {
        // 1950-01-01 ~ 2050-12-31 안으로 자른다. 잘라낸 구간이 비면 요청하지 않는다 (R-15)
        val start = if (from.isBefore(AnniversaryRules.MIN_DATE)) AnniversaryRules.MIN_DATE else from
        val end = if (to.isAfter(AnniversaryRules.MAX_DATE)) AnniversaryRules.MAX_DATE else to
        if (start.isAfter(end)) return DataResult.Success(Unit)
        // 읽기라서 쓰기 규칙은 아니지만 오프라인이면 요청 없이 조용히 실패한다 (NFR-OFF-01)
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        // 요청 전에 캐시 세대와 요청 번호를 적어 둔다
        val generation = cache.current()
        val seq = rangeSeq.incrementAndGet()
        return when (val result = remote.list(start, end)) {
            is DataResult.Success -> {
                rangeLock.withLock {
                    val last = lastSavedRange?.takeIf { it.generation == generation && it.seq > seq }
                    // 겹치는 더 새 응답이 이미 저장됐으면 그 겹치는 날짜는 덮지 않고, 나머지 날짜만 저장한다
                    var saveFrom = start
                    var saveTo = end
                    if (last != null && !last.to.isBefore(start) && !last.from.isAfter(end)) {
                        when {
                            !last.from.isAfter(start) -> saveFrom = last.to.plusDays(1)
                            !last.to.isBefore(end) -> saveTo = last.from.minusDays(1)
                            else -> saveTo = saveFrom.minusDays(1) // 가운데가 겹치는 경우는 저장하지 않는다
                        }
                    }
                    if (!saveFrom.isAfter(saveTo)) {
                        val entities = result.value
                            .filter { !it.date.isBefore(saveFrom) && !it.date.isAfter(saveTo) }
                            .mapIndexed { i, o -> o.toEntity(LIST_RANGE, i) }
                        val saved = cache.saveIfCurrent(generation) { local.replaceRange(saveFrom, saveTo, entities) }
                        if (saved && last == null) lastSavedRange = SavedRange(seq, start, end, generation)
                    }
                }
                DataResult.Success(Unit)
            }

            is DataResult.Failure -> result
        }
    }

    override suspend fun refreshUpcoming(): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return upcomingLock.withLock {
            val generation = cache.current()
            when (val result = remote.upcoming()) {
                is DataResult.Success -> {
                    cache.saveIfCurrent(generation) {
                        local.replaceUpcoming(
                            result.value.items.mapIndexed { i, o -> o.toEntity(LIST_UPCOMING, i) },
                            result.value.nextCursor,
                        )
                    }
                    DataResult.Success(Unit)
                }

                is DataResult.Failure -> result
            }
        }
    }

    override suspend fun loadMoreUpcoming(): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return upcomingLock.withLock {
            // 잠금 안에서 읽어 앞선 요청이 끝난 뒤의 커서를 쓴다. 없으면 마지막 페이지다 (세대는 커서를 읽기 전에 적는다)
            val generation = cache.current()
            val cursor = local.upcomingNextCursor() ?: return@withLock DataResult.Success(Unit)
            when (val result = remote.upcoming(cursor = cursor)) {
                is DataResult.Success -> {
                    cache.saveIfCurrent(generation) {
                        local.appendUpcoming(
                            result.value.items.map { it.toEntity(LIST_UPCOMING, 0) },
                            result.value.nextCursor,
                        )
                    }
                    DataResult.Success(Unit)
                }

                is DataResult.Failure -> result
            }
        }
    }

    override suspend fun refreshPast(): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return pastLock.withLock {
            val generation = cache.current()
            when (val result = remote.past()) {
                is DataResult.Success -> {
                    cache.saveIfCurrent(generation) {
                        local.replacePast(
                            result.value.items.mapIndexed { i, p -> p.toEntity(i) },
                            result.value.nextCursor,
                            result.value.totalCount ?: result.value.items.size,
                        )
                    }
                    DataResult.Success(Unit)
                }

                is DataResult.Failure -> result
            }
        }
    }

    override suspend fun loadMorePast(): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return pastLock.withLock {
            val generation = cache.current()
            val cursor = local.pastNextCursor() ?: return@withLock DataResult.Success(Unit)
            when (val result = remote.past(cursor = cursor)) {
                is DataResult.Success -> {
                    cache.saveIfCurrent(generation) {
                        // 다음 페이지에 totalCount가 없으면 저장된 값을 그대로 둔다
                        local.appendPast(
                            result.value.items.map { it.toEntity(0) },
                            result.value.nextCursor,
                            result.value.totalCount,
                        )
                    }
                    DataResult.Success(Unit)
                }

                is DataResult.Failure -> result
            }
        }
    }

    override suspend fun refreshAll(): DataResult<Unit> = coroutineScope {
        val range = visibleRange?.takeIf { it.generation == cache.current() }
        val jobs = buildList {
            add(async { refreshUpcoming() })
            add(async { refreshPast() })
            if (range != null) add(async { refreshRange(range.from, range.to) })
        }
        jobs.awaitAll().firstOrNull { it is DataResult.Failure } ?: DataResult.Success(Unit)
    }

    override suspend fun create(anniversary: NewAnniversary): DataResult<Anniversary> =
        networkMonitor.runOnlineWrite { remote.create(anniversary) }.thenRefreshAll()

    override suspend fun update(id: String, patch: AnniversaryPatch): DataResult<Unit> {
        if (patch.isEmpty) return DataResult.Success(Unit)
        return networkMonitor.runOnlineWrite { remote.update(id, patch) }
            .thenRefreshAll()
            .dropValue()
            .thenForgetIfGone(id)
            .thenRereadIfNotAuthor()
    }

    override suspend fun delete(id: String): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.delete(id) }
            .thenRefreshAll()
            .thenForgetIfGone(id)
            .thenRereadIfNotAuthor()

    /** 쓰기가 성공했으면 목록과 달을 다시 불러온다 (자동 기념일 계산은 서버). 이 갱신이 실패해도 쓰기 결과는 바꾸지 않는다 */
    private suspend fun <T> DataResult<T>.thenRefreshAll(): DataResult<T> {
        if (this is DataResult.Success) refreshAllQuietly()
        return this
    }

    /** 다시 불러오다 예외가 나도 삼킨다 (이미 끝난 쓰기 결과를 실패로 바꾸지 않는다). 취소는 그대로 던진다 */
    private suspend fun refreshAllQuietly() {
        try {
            refreshAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "쓰기 뒤 다시 불러오기에 실패했어요: ${e.javaClass.simpleName}")
        }
    }

    private fun <T> DataResult<T>.dropValue(): DataResult<Unit> = when (this) {
        is DataResult.Success -> DataResult.Success(Unit)
        is DataResult.Failure -> this
    }

    /** 404 `NOT_FOUND`: 없거나 다른 공간의 기념일이다. Room에서도 지운다 (ARCHITECTURE.md 5장, R-01, D-121) */
    private suspend fun <T> DataResult<T>.thenForgetIfGone(id: String): DataResult<T> {
        if (this is DataResult.Failure && error.hasCode(ErrorCode.NOT_FOUND)) local.delete(id)
        return this
    }

    /** 403 `NOT_AUTHOR`: 낡은 권한 표시일 수 있어 목록과 달을 다시 불러온다 (실패해도 결과는 그대로, D-121) */
    private suspend fun <T> DataResult<T>.thenRereadIfNotAuthor(): DataResult<T> {
        if (this is DataResult.Failure && error.hasCode(ErrorCode.NOT_AUTHOR)) refreshAllQuietly()
        return this
    }

    private companion object {
        const val TAG = "anniversary"
    }
}
