package com.duriseo.android.feature.memo

import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MePartner
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SpaceChange
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.memo.Memo
import com.duriseo.android.domain.memo.MemoList
import com.duriseo.android.domain.memo.MemoPatch
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.domain.memo.NewMemo
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val MyId = "11111111-1111-1111-1111-111111111111"
const val PartnerId = "33333333-3333-3333-3333-333333333333"

/** 2026-10-12(월) 오전 10:20, 서울 */
val TestClock: Clock = Clock.fixed(Instant.parse("2026-10-12T01:20:00Z"), ZoneId.of("Asia/Seoul"))

fun apiError(code: String) = DataResult.Failure(DataError.ApiError(code = code, details = null, traceId = null))

val Offline = DataResult.Failure(DataError.Offline)

fun soloMe() = Me(
    id = MyId,
    accountType = AccountType.MEMBER,
    nickname = "민지",
    timeZone = "Asia/Seoul",
    providers = emptyList(),
    hasProfileImage = false,
    createdAt = Instant.parse("2026-10-01T00:00:00Z"),
    space = MeSpace(
        id = "22222222-2222-2222-2222-222222222222",
        type = SpaceType.PERSONAL,
        myColor = "pick-orange",
        partner = null,
        togetherColor = null,
        startDate = null,
        importPending = false,
    ),
)

fun coupleMe(): Me = soloMe().let { me ->
    me.copy(
        space = me.space.copy(
            type = SpaceType.COUPLE,
            partner = MePartner(id = PartnerId, nickname = "지우", color = "pick-blue", hasProfileImage = false),
            togetherColor = "pick-purple",
            startDate = LocalDate.of(2026, 4, 7),
        ),
    )
}

fun makeMemo(
    id: String,
    title: String? = "제목 $id",
    body: String? = "본문 $id",
    authorId: String = MyId,
    pinned: Boolean = false,
    favorite: Boolean = false,
    memoDate: LocalDate? = null,
    createdAt: Instant = Instant.parse("2026-10-11T01:00:00Z"),
) = Memo(
    id = id,
    title = title,
    body = body,
    memoDate = memoDate,
    authorId = authorId,
    isPinned = pinned,
    pinnedAt = if (pinned) Instant.parse("2026-10-11T02:00:00Z") else null,
    isFavorite = favorite,
    createdAt = createdAt,
    updatedAt = createdAt,
)

class FakeMeRepository(initial: Me? = null) : MeRepository {
    val me = MutableStateFlow(initial)
    override fun observeMe(): Flow<Me?> = me
    override suspend fun refreshMe(): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun syncTimeZone(zoneId: String): DataResult<Unit> = DataResult.Success(Unit)
    override val spaceChanges: Flow<SpaceChange> = MutableSharedFlow()
    override fun observeLinkedSheetNeeded(): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun acknowledgeSpace() = Unit
    override suspend fun resetAfterUnlink() = Unit
}

/**
 * Room처럼 동작하는 가짜 저장소: [lists]·[pinned]·[favoriteCount]·[memos]가 바뀌면 Flow가 따라온다. 요청은 횟수와 인자를 기록한다.
 * 쓰기 응답의 약속(`MemoRepository` 문서)도 흉내 낸다: 404면 기기에서 지우고, 403이면 다시 불러온다. 진짜 동작은 `MemoRepositoryTest`가 확인한다.
 */
class FakeMemoRepository : MemoRepository {
    val lists = MemoView.entries.associateWith { MutableStateFlow(MemoList(emptyList(), 0, hasMore = false, isLoaded = true)) }
    val pinned = MutableStateFlow<List<Memo>>(emptyList())
    val favoriteCount = MutableStateFlow(0)

    /** 상세용으로 저장된 메모 (id -> 메모) */
    val memos = MutableStateFlow<Map<String, Memo>>(emptyMap())

    val refreshListCalls = mutableListOf<MemoView>()
    val loadMoreCalls = mutableListOf<MemoView>()
    var refreshPinnedCalls = 0
    val refreshMemoCalls = mutableListOf<String>()
    var refreshAllCalls = 0

    val created = mutableListOf<NewMemo>()
    val patches = mutableListOf<Pair<String, MemoPatch>>()
    val deleted = mutableListOf<String>()
    val pinCalls = mutableListOf<Pair<String, Boolean>>()
    val favoriteCalls = mutableListOf<Pair<String, Boolean>>()

    var refreshMemoResult: DataResult<Unit> = DataResult.Success(Unit)
    var createResult: DataResult<Unit> = DataResult.Success(Unit)
    var updateResult: DataResult<Unit> = DataResult.Success(Unit)
    var deleteResult: DataResult<Unit> = DataResult.Success(Unit)
    var pinResult: DataResult<Unit> = DataResult.Success(Unit)
    var favoriteResult: DataResult<Unit> = DataResult.Success(Unit)

    /** 설정하면 해당 쓰기가 이 신호를 받을 때까지 멈춘다 (요청 중 상태를 확인하려고) */
    var writeGate: CompletableDeferred<Unit>? = null

    fun setList(view: MemoView, items: List<Memo>, totalCount: Int = items.size, hasMore: Boolean = false) {
        lists.getValue(view).value = MemoList(items, totalCount, hasMore, isLoaded = true)
    }

    override fun observeList(view: MemoView): Flow<MemoList> = lists.getValue(view)
    override fun observeFavoriteCount(): Flow<Int> = favoriteCount
    override fun observePinned(): Flow<List<Memo>> = pinned
    override fun observeDated(from: LocalDate, to: LocalDate): Flow<List<Memo>> = MutableStateFlow(emptyList())
    override fun observeMemo(id: String): Flow<Memo?> = memos.map { it[id] }
    override fun setVisibleRange(from: LocalDate, to: LocalDate) = Unit

    override suspend fun refreshList(view: MemoView): DataResult<Unit> {
        refreshListCalls += view
        return DataResult.Success(Unit)
    }

    override suspend fun loadMoreList(view: MemoView): DataResult<Unit> {
        loadMoreCalls += view
        return DataResult.Success(Unit)
    }

    override suspend fun refreshPinned(): DataResult<Unit> {
        refreshPinnedCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun refreshDated(from: LocalDate, to: LocalDate): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun refreshMemo(id: String): DataResult<Unit> {
        refreshMemoCalls += id
        return refreshMemoResult.alsoApplyServerAnswer(id)
    }

    override suspend fun refreshAll(): DataResult<Unit> {
        refreshAllCalls++
        return DataResult.Success(Unit)
    }

    override suspend fun create(memo: NewMemo): DataResult<Memo> {
        created += memo
        writeGate?.await()
        (createResult as? DataResult.Failure)?.let { return it }
        return DataResult.Success(makeMemo("new-${created.size}", memo.title, memo.body, memoDate = memo.memoDate))
    }

    override suspend fun update(id: String, patch: MemoPatch): DataResult<Unit> {
        patches += id to patch
        writeGate?.await()
        return updateResult.alsoApplyServerAnswer(id)
    }

    override suspend fun delete(id: String): DataResult<Unit> {
        deleted += id
        writeGate?.await()
        return deleteResult.alsoApplyServerAnswer(id)
    }

    override suspend fun setPinned(id: String, pinned: Boolean): DataResult<Unit> {
        pinCalls += id to pinned
        writeGate?.await()
        return pinResult.alsoApplyServerAnswer(id)
    }

    override suspend fun setFavorite(id: String, favorite: Boolean): DataResult<Unit> {
        favoriteCalls += id to favorite
        writeGate?.await()
        val result = favoriteResult.alsoApplyServerAnswer(id)
        if (result is DataResult.Success) {
            // 성공하면 저장소가 개수를 다시 읽는다
            favoriteCount.value += if (favorite) 1 else -1
        }
        return result
    }

    private fun DataResult<Unit>.alsoApplyServerAnswer(id: String): DataResult<Unit> {
        val code = ((this as? DataResult.Failure)?.error as? DataError.ApiError)?.code
        if (code == "NOT_FOUND") {
            memos.value = memos.value - id
            pinned.value = pinned.value.filterNot { it.id == id }
            MemoView.entries.forEach { view ->
                val flow = lists.getValue(view)
                flow.value = flow.value.copy(items = flow.value.items.filterNot { it.id == id })
            }
        }
        if (code == "NOT_AUTHOR") {
            refreshAllCalls++
            refreshMemoCalls += id
        }
        return this
    }
}
