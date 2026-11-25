package com.duriseo.android.data.repository.memo

import com.duriseo.android.core.common.Logger
import com.duriseo.android.data.local.memo.MEMO_LIST_DETAIL
import com.duriseo.android.data.local.memo.MEMO_LIST_PINNED
import com.duriseo.android.data.local.memo.MEMO_STATE_COUNTS
import com.duriseo.android.data.local.memo.MEMO_STATE_PINNED
import com.duriseo.android.data.local.memo.MemoLocalDataSource
import com.duriseo.android.data.remote.memo.MemoRemoteDataSource
import com.duriseo.android.data.repository.cache.LocalCacheGeneration
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.memo.Memo
import com.duriseo.android.domain.memo.MemoList
import com.duriseo.android.domain.memo.MemoPatch
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.domain.memo.NewMemo
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 메모: 화면은 Room만 구독하고, 서버 응답은 Room에 넣은 뒤 Flow로 보인다 (ARCHITECTURE.md 2장).
 * 응답 배열의 위치를 보기별·고정 목록마다 그대로 저장하고 다시 정렬하지 않는다 (API.md 3.4 "앱 동작").
 * 쓰기는 [runOnlineWrite]로 감싸 오프라인이면 요청하지 않고, 성공하면 목록·고정을 다시 불러온다 (낙관적 갱신 없음).
 *
 * 락 순서는 기념일과 같다: 목록별 락 -> `LocalCacheGeneration`의 Mutex (맨 안쪽). 목록별 락 두 개를 함께 잡지 않는다.
 */
@Singleton
class MemoRepositoryImpl @Inject constructor(
    private val remote: MemoRemoteDataSource,
    private val local: MemoLocalDataSource,
    private val networkMonitor: NetworkMonitor,
    private val cache: LocalCacheGeneration,
) : MemoRepository {

    /** 같은 보기의 첫 페이지·다음 페이지 요청이 겹쳐 이어 붙이기가 두 번 일어나지 않게 한다 */
    private val viewLocks: Map<MemoView, Mutex> = MemoView.entries.associateWith { Mutex() }
    private val pinnedLock = Mutex()

    /**
     * 상세 응답과 삭제가 엇갈리지 않게 한다: 삭제·404로 지운 시점의 번호를 [goneAt]에 적고, 그보다 먼저 요청한 상세 응답은 저장하지 않는다.
     * 삭제 성공 뒤 늦게 도착한 이전 응답이 DETAIL 행을 되살리지 않게 하려는 것이다. 락 순서: [detailLock] -> `LocalCacheGeneration`.
     */
    private val detailLock = Mutex()
    private val detailSeq = AtomicLong(0)
    private val goneAt = ConcurrentHashMap<String, Long>()

    override fun observeList(view: MemoView): Flow<MemoList> =
        combine(local.observeList(view.name), local.observeState(view.name)) { rows, state ->
            MemoList(
                items = rows.mapNotNull { it.toDomainOrNull() },
                totalCount = state?.totalCount ?: 0,
                hasMore = state?.nextCursor != null,
                isLoaded = state != null,
            )
        }

    override fun observeFavoriteCount(): Flow<Int> =
        local.observeState(MEMO_STATE_COUNTS).map { it?.favoriteCount ?: 0 }

    override fun observePinned(): Flow<List<Memo>> =
        local.observeList(MEMO_LIST_PINNED).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observePinnedLoaded(): Flow<Boolean> = local.observeState(MEMO_STATE_PINNED).map { it != null }

    override fun observeMemo(id: String): Flow<Memo?> = local.observeMemo(id).map { it?.toDomainOrNull() }

    override suspend fun refreshList(view: MemoView): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return viewLocks.getValue(view).withLock {
            val generation = cache.current()
            when (val result = remote.list(view)) {
                is DataResult.Success -> {
                    val page = result.value
                    cache.saveIfCurrent(generation) {
                        local.replaceList(
                            view.name,
                            page.items.mapIndexed { i, m -> m.toEntity(view.name, i) },
                            page.nextCursor,
                            page.totalCount,
                            page.favoriteCount,
                        )
                    }
                    DataResult.Success(Unit)
                }

                is DataResult.Failure -> result
            }
        }
    }

    override suspend fun loadMoreList(view: MemoView): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return viewLocks.getValue(view).withLock {
            // 잠금 안에서 읽어 앞선 요청이 끝난 뒤의 커서를 쓴다. 없으면 마지막 페이지다 (세대는 커서를 읽기 전에 적는다)
            val generation = cache.current()
            val cursor = local.nextCursor(view.name) ?: return@withLock DataResult.Success(Unit)
            when (val result = remote.list(view, cursor = cursor)) {
                is DataResult.Success -> {
                    val page = result.value
                    cache.saveIfCurrent(generation) {
                        local.appendList(
                            view.name,
                            page.items.map { it.toEntity(view.name, 0) },
                            page.nextCursor,
                            page.totalCount,
                            page.favoriteCount,
                        )
                    }
                    DataResult.Success(Unit)
                }

                is DataResult.Failure -> result
            }
        }
    }

    override suspend fun refreshPinned(): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return pinnedLock.withLock {
            val generation = cache.current()
            when (val result = remote.pinned()) {
                is DataResult.Success -> {
                    cache.saveIfCurrent(generation) {
                        local.replacePinned(result.value.mapIndexed { i, m -> m.toEntity(MEMO_LIST_PINNED, i) })
                    }
                    DataResult.Success(Unit)
                }

                is DataResult.Failure -> result
            }
        }
    }

    override suspend fun refreshMemo(id: String): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        val generation = cache.current()
        val seq = detailSeq.incrementAndGet() // 요청 전에 적는다
        return when (val result = remote.get(id)) {
            is DataResult.Success -> {
                detailLock.withLock {
                    // 이 요청 뒤에 그 메모를 지웠으면 늦게 온 응답은 버린다
                    if ((goneAt[id] ?: 0L) < seq) {
                        cache.saveIfCurrent(generation) { local.replaceDetail(result.value.toEntity(MEMO_LIST_DETAIL, 0)) }
                    }
                }
                DataResult.Success(Unit)
            }

            // 404 `NOT_FOUND`: 없거나 다른 공간의 메모다. Room에서도 지운다 (ARCHITECTURE.md 5장, R-01, D-121)
            is DataResult.Failure -> result.thenForgetIfGone(id)
        }
    }

    override suspend fun forget(id: String) = forgetLocally(id)

    // supervisorScope: 한 갈래가 예외로 끝나도 형제를 취소하지 않는다. 예외는 awaitAll이 원래 예외 그대로 던진다
    override suspend fun refreshAll(): DataResult<Unit> = supervisorScope {
        val views = local.loadedViews().mapNotNull { name -> MemoView.entries.firstOrNull { it.name == name } }
        val jobs = buildList {
            views.forEach { view -> add(async { refreshList(view) }) }
            add(async { refreshPinned() })
        }
        jobs.awaitAll().firstOrNull { it is DataResult.Failure } ?: DataResult.Success(Unit)
    }

    override suspend fun create(memo: NewMemo): DataResult<Memo> {
        val generation = cache.current()
        return networkMonitor.runOnlineWrite { remote.create(memo) }
            .thenSaveDetail(generation)
            .thenRefreshAll(detailId = null)
    }

    override suspend fun update(id: String, patch: MemoPatch): DataResult<Unit> {
        if (patch.isEmpty) return DataResult.Success(Unit)
        val generation = cache.current()
        return networkMonitor.runOnlineWrite { remote.update(id, patch) }
            .thenSaveDetail(generation)
            .thenRefreshAll(detailId = null)
            .dropValue()
            .thenForgetIfGone(id)
            .thenRereadIfNotAuthor(id)
    }

    override suspend fun delete(id: String): DataResult<Unit> {
        val result = networkMonitor.runOnlineWrite { remote.delete(id) }
        // 서버가 지웠으니 기기에서도 지운다 (낙관적 갱신이 아니라 성공 응답의 반영). 다시 불러오기가 개수를 맞춘다.
        // 서버가 성공했으면 Room 반영이 실패해도 결과는 성공이다
        if (result is DataResult.Success) quietly { forgetLocally(id) }
        return result
            .thenRefreshAll(detailId = null)
            .thenForgetIfGone(id)
            .thenRereadIfNotAuthor(id)
    }

    override suspend fun setPinned(id: String, pinned: Boolean): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.setPinned(id, pinned) }
            .thenRefreshAll(detailId = id)
            .thenForgetIfGone(id)

    override suspend fun setFavorite(id: String, favorite: Boolean): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.setFavorite(id, favorite) }
            .thenRefreshAll(detailId = id)
            .thenForgetIfGone(id)

    /**
     * 생성·수정 응답의 메모를 상세로 저장한다 (성공 응답의 반영). 요청 전에 적은 [generation]이 지금도 같을 때만.
     * 서버가 성공했으면 Room 반영이 실패해도(예외) 삼키고 결과는 성공 그대로다.
     */
    private suspend fun DataResult<Memo>.thenSaveDetail(generation: Long): DataResult<Memo> {
        if (this is DataResult.Success) {
            val memo = value
            quietly { cache.saveIfCurrent(generation) { local.replaceDetail(memo.toEntity(MEMO_LIST_DETAIL, 0)) } }
        }
        return this
    }

    /** 그 메모를 기기에서 지운다. 지운 시점을 적어 두어 그 전에 요청한 상세 응답이 되살리지 못하게 한다 */
    private suspend fun forgetLocally(id: String) {
        detailLock.withLock {
            goneAt[id] = detailSeq.incrementAndGet()
            local.delete(id)
        }
    }

    /**
     * 쓰기가 성공했으면 목록(받은 보기들)·고정·개수를 다시 불러온다. [detailId]가 있고 그 상세를 저장해 둔 적이 있으면
     * 상세도 함께 다시 받는다 (고정·즐겨찾기는 204라 응답에 메모가 없다). 이 갱신이 실패하거나 예외가 나도 쓰기 결과는 바꾸지 않는다.
     */
    private suspend fun <T> DataResult<T>.thenRefreshAll(detailId: String?): DataResult<T> {
        if (this is DataResult.Success) {
            coroutineScope {
                val all = async { quietly { refreshAll() } }
                val detail = async {
                    quietly { if (detailId != null && local.hasDetail(detailId)) refreshMemo(detailId) }
                }
                all.await()
                detail.await()
            }
        }
        return this
    }

    /** 서버 성공 뒤의 기기 반영·다시 불러오기에서 예외가 나도 삼킨다 (이미 끝난 쓰기 결과를 실패로 바꾸지 않는다). 취소는 그대로 던진다 */
    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            // 내 코루틴이 정말 취소됐을 때만 다시 던진다. 안쪽 갈래의 취소가 새어 나온 것이면 삼킨다
            currentCoroutineContext().ensureActive()
            Logger.w(TAG, "쓰기 뒤 기기 반영·다시 불러오기가 취소됐어요: ${e.javaClass.simpleName}")
        } catch (e: Exception) {
            Logger.w(TAG, "쓰기 뒤 기기 반영·다시 불러오기에 실패했어요: ${e.javaClass.simpleName}")
        }
    }

    private fun <T> DataResult<T>.dropValue(): DataResult<Unit> = when (this) {
        is DataResult.Success -> DataResult.Success(Unit)
        is DataResult.Failure -> this
    }

    /** 404 `NOT_FOUND`: 없거나 다른 공간의 메모다. Room에서도 지운다 (ARCHITECTURE.md 5장, R-01, D-121) */
    private suspend fun <T> DataResult<T>.thenForgetIfGone(id: String): DataResult<T> {
        if (this is DataResult.Failure && error.hasCode(ErrorCode.NOT_FOUND)) quietly { forgetLocally(id) }
        return this
    }

    /** 403 `NOT_AUTHOR`: 낡은 권한 표시일 수 있어 목록들과 그 메모 상세를 다시 불러온다 (실패해도 결과는 그대로, D-121) */
    private suspend fun <T> DataResult<T>.thenRereadIfNotAuthor(id: String): DataResult<T> {
        if (this is DataResult.Failure && error.hasCode(ErrorCode.NOT_AUTHOR)) {
            coroutineScope {
                val all = async { quietly { refreshAll() } }
                val detail = async { quietly { refreshMemo(id) } }
                all.await()
                detail.await()
            }
        }
        return this
    }

    private companion object {
        const val TAG = "memo"
    }
}
