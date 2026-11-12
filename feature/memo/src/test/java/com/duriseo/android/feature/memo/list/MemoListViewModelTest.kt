package com.duriseo.android.feature.memo.list

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.feature.memo.FakeMemoRepository
import com.duriseo.android.feature.memo.FakeMeRepository
import com.duriseo.android.feature.memo.MutableClock
import com.duriseo.android.feature.memo.Offline
import com.duriseo.android.feature.memo.PartnerId
import com.duriseo.android.feature.memo.TestClock
import com.duriseo.android.feature.memo.coupleMe
import com.duriseo.android.feature.memo.makeMemo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MemoListViewModelTest {

    private val repo = FakeMemoRepository()
    private val me = FakeMeRepository(coupleMe())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MemoListViewModel(repo, me, TestClock)

    private fun pins(count: Int) = (1..count).map { makeMemo("p$it", pinned = true) }

    @Test
    fun `FR-MEMO-01 AC1 최신순 목록은 Room의 순서 그대로이고 고정 메모는 목록에 없다`() = runTest {
        repo.pinned.value = pins(1)
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m2"), makeMemo("m1")), totalCount = 2)

        val state = viewModel().uiState.value

        assertTrue(state.loaded)
        assertEquals(MemoView.NEWEST, state.view)
        assertEquals(listOf("m2", "m1"), state.items.map { it.id })
        assertEquals(2, state.totalCount)
        assertEquals(listOf("p1"), state.pins.map { it.id })
    }

    @Test
    fun `FR-MEMO-01 AC2 보기를 바꾸면 그 보기의 첫 페이지를 불러온다`() = runTest {
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m2"), makeMemo("m1")))
        repo.setList(MemoView.OLDEST, listOf(makeMemo("m1"), makeMemo("m2")))
        repo.setList(MemoView.FAVORITES, listOf(makeMemo("m1", favorite = true)), totalCount = 1)
        val vm = viewModel()

        vm.onAction(MemoListAction.ViewSelected(MemoView.OLDEST))
        assertEquals(listOf(MemoView.OLDEST), repo.refreshListCalls)
        assertEquals(MemoView.OLDEST, vm.uiState.value.view)
        assertEquals(listOf("m1", "m2"), vm.uiState.value.items.map { it.id })

        vm.onAction(MemoListAction.ViewSelected(MemoView.FAVORITES))
        assertEquals(listOf(MemoView.OLDEST, MemoView.FAVORITES), repo.refreshListCalls)
        assertEquals(listOf("m1"), vm.uiState.value.items.map { it.id })
        assertEquals(1, vm.uiState.value.totalCount)

        // 같은 보기를 다시 눌러도 다시 불러오지 않는다
        vm.onAction(MemoListAction.ViewSelected(MemoView.FAVORITES))
        assertEquals(2, repo.refreshListCalls.size)
    }

    @Test
    fun `FR-MEMO-01 AC2 즐겨찾기 칩의 개수는 favoriteCount다`() = runTest {
        repo.favoriteCount.value = 2

        assertEquals(2, viewModel().uiState.value.favoriteCount)
    }

    @Test
    fun `FR-MEMO-01 AC3 작성자는 닉네임이고 작성일은 D-129 표기 계산 결과다`() = runTest {
        // 오늘 오전 10:20(서울) 기준: 같은 날 오전 8:40은 "오늘", 어제, 올해, 다른 해
        repo.setList(
            MemoView.NEWEST,
            listOf(
                makeMemo("a", authorId = PartnerId, createdAt = java.time.Instant.parse("2026-10-11T23:40:00Z")),
                makeMemo("b", createdAt = java.time.Instant.parse("2026-10-11T01:00:00Z"), memoDate = LocalDate.of(2027, 10, 17)),
            ),
        )

        val items = viewModel().uiState.value.items

        assertEquals("지우", items[0].nickname)
        assertEquals(WrittenAt.Today(isPm = false, hour12 = 8, minute = 40), items[0].writtenAt)
        assertEquals("민지", items[1].nickname)
        assertEquals(WrittenAt.Yesterday, items[1].writtenAt)
        // 메모 날짜가 올해가 아니면 칩에 연도가 있다 (D-129)
        assertEquals(2027, items[1].memoDate?.year)
        assertFalse(items[0].canEdit)
        assertTrue(items[1].canEdit)
    }

    @Test
    fun `FR-MEMO-05 AC2 고정이 3개 이하이면 모두 보이고 더보기가 없다`() = runTest {
        repo.pinned.value = pins(3)

        val state = viewModel().uiState.value

        assertEquals(3, state.visiblePins.size)
        assertFalse(state.showPinsMore)
        assertFalse(state.showNoPins)
    }

    @Test
    fun `FR-MEMO-05 AC3 고정이 4개 이상이면 최근 고정 3개만 보이고 더보기가 있다`() = runTest {
        repo.pinned.value = pins(4)

        val state = viewModel().uiState.value

        assertEquals(listOf("p1", "p2", "p3"), state.visiblePins.map { it.id })
        assertTrue(state.showPinsMore)
        // "고정된 핀 N개"의 N은 전체 개수다
        assertEquals(4, state.pins.size)

        val vm = viewModel()
        vm.effects.test {
            vm.onAction(MemoListAction.PinsMoreClicked)
            assertEquals(MemoListSideEffect.OpenPinned, awaitItem())
        }
    }

    @Test
    fun `SCR-MEMO-01 고정이 없으면 점선 상자를 보인다`() = runTest {
        assertTrue(viewModel().uiState.value.showNoPins)
    }

    @Test
    fun `SCR-MEMO-01 고정 목록을 받기 전에는 점선 상자를 숨긴다`() = runTest {
        repo.pinnedLoaded.value = false
        assertFalse(viewModel().uiState.value.showNoPins)
    }

    @Test
    fun `SCR-MEMO-01 즐겨찾기 보기가 비면 안내 문구를 보이고 최신순이 비면 보이지 않는다`() = runTest {
        val vm = viewModel()
        // 최신순이 비어도 새 문구는 없다 (확인 필요 2)
        assertFalse(vm.uiState.value.showFavoritesEmpty)

        vm.onAction(MemoListAction.ViewSelected(MemoView.FAVORITES))
        assertTrue(vm.uiState.value.showFavoritesEmpty)
    }

    @Test
    fun `FR-MEMO-05 AC1 상대 메모도 고정할 수 있다`() = runTest {
        repo.setList(MemoView.NEWEST, listOf(makeMemo("partner-memo", authorId = PartnerId)))
        val vm = viewModel()
        val item = vm.uiState.value.items.single()

        vm.onAction(MemoListAction.PinClicked(item))

        assertEquals(listOf("partner-memo" to true), repo.pinCalls)
    }

    @Test
    fun `FR-MEMO-05 AC5 고정된 핀 카드의 핀을 누르면 고정을 푼다`() = runTest {
        repo.pinned.value = pins(1)
        val vm = viewModel()

        vm.onAction(MemoListAction.PinClicked(vm.uiState.value.pins.single()))

        assertEquals(listOf("p1" to false), repo.pinCalls)
    }

    @Test
    fun `FR-MEMO-06 별을 누르면 즐겨찾기를 바꾸고 개수를 다시 읽는다`() = runTest {
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m1")))
        val vm = viewModel()

        vm.onAction(MemoListAction.FavoriteClicked(vm.uiState.value.items.single()))

        assertEquals(listOf("m1" to true), repo.favoriteCalls)
        assertEquals(1, vm.uiState.value.favoriteCount)
    }

    @Test
    fun `D-115 요청 중에는 loading이 true이고 끝나면 false다`() = runTest {
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m1")))
        val gate = CompletableDeferred<Unit>()
        repo.writeGate = gate
        val vm = viewModel()
        assertFalse(vm.uiState.value.loading)

        vm.onAction(MemoListAction.FavoriteClicked(vm.uiState.value.items.single()))
        assertTrue(vm.uiState.value.loading)
        // 요청 중에는 다른 쓰기를 받지 않는다
        vm.onAction(MemoListAction.PinClicked(vm.uiState.value.items.single()))
        assertTrue(repo.pinCalls.isEmpty())

        gate.complete(Unit)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `D-115 저장소가 예외를 던져도 loading이 false로 돌아온다`() = runTest {
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m1")))
        repo.writeFailure = IllegalStateException("저장 실패")
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(MemoListAction.PinClicked(vm.uiState.value.items.single()))
            assertEquals(MemoListSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
        }
        assertFalse(vm.uiState.value.loading)
        // 덮개가 걷힌 뒤에는 다시 쓸 수 있다
        repo.writeFailure = null
        vm.onAction(MemoListAction.PinClicked(vm.uiState.value.items.single()))
        assertEquals(2, repo.pinCalls.size)
    }

    @Test
    fun `NFR-TIME-02 자정이 지나면 작성일의 오늘과 어제를 다시 계산한다`() = runTest {
        // 2026-10-12 23:50(서울), 같은 날 오후 2시에 쓴 메모는 "오늘"
        val clock = MutableClock(java.time.Instant.parse("2026-10-12T14:50:00Z"))
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m1", createdAt = java.time.Instant.parse("2026-10-12T05:00:00Z"))))
        val vm = MemoListViewModel(repo, me, clock)
        assertTrue(vm.uiState.value.items.single().writtenAt is WrittenAt.Today)

        // 자정이 지났다: 다음 자정까지 쉬던 신호가 깨어나 "어제"로 바뀐다
        clock.now = java.time.Instant.parse("2026-10-12T15:10:00Z")
        testScheduler.advanceTimeBy(20 * 60 * 1000L)
        testScheduler.runCurrent()

        assertEquals(WrittenAt.Yesterday, vm.uiState.value.items.single().writtenAt)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 인터넷 연결이 필요하다는 안내를 보인다`() = runTest {
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m1")))
        repo.pinResult = Offline
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(MemoListAction.PinClicked(vm.uiState.value.items.single()))
            assertEquals(MemoListSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
        // 안내 뒤 덮개는 걷혀 있다
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `FR-MEMO-01 AC1 서버 오류는 공통 오류 안내로 보인다`() = runTest {
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m1")))
        repo.favoriteResult = com.duriseo.android.feature.memo.apiError("INTERNAL_ERROR")
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(MemoListAction.FavoriteClicked(vm.uiState.value.items.single()))
            assertEquals(MemoListSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
        }
    }

    @Test
    fun `FR-MEMO-01 AC2 화면이 보이면 고정 목록과 지금 보기를 다시 불러온다`() = runTest {
        val vm = viewModel()

        vm.onAction(MemoListAction.Shown)

        assertEquals(1, repo.refreshPinnedCalls)
        assertEquals(listOf(MemoView.NEWEST), repo.refreshListCalls)
    }

    @Test
    fun `FR-MEMO-01 AC2 스크롤 끝에서 다음 페이지를 이어 붙이고 더 없으면 요청하지 않는다`() = runTest {
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m1")), totalCount = 30, hasMore = true)
        val vm = viewModel()

        vm.onAction(MemoListAction.EndReached)
        assertEquals(listOf(MemoView.NEWEST), repo.loadMoreCalls)

        repo.setList(MemoView.NEWEST, listOf(makeMemo("m1")), totalCount = 1, hasMore = false)
        vm.onAction(MemoListAction.EndReached)
        assertEquals(1, repo.loadMoreCalls.size)
    }

    @Test
    fun `SCR-MEMO-01 메모를 누르면 상세로 가고 FAB은 새 메모로 간다`() = runTest {
        repo.setList(MemoView.NEWEST, listOf(makeMemo("m1")))
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(MemoListAction.ItemClicked(vm.uiState.value.items.single()))
            assertEquals(MemoListSideEffect.OpenDetail("m1"), awaitItem())
            vm.onAction(MemoListAction.AddClicked)
            assertEquals(MemoListSideEffect.OpenCreate, awaitItem())
        }
    }
}
