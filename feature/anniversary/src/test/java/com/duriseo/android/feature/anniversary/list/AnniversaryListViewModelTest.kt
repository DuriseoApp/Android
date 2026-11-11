package com.duriseo.android.feature.anniversary.list

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.feature.anniversary.FakeAnniversaryRepository
import com.duriseo.android.feature.anniversary.FakeCoupleRepository
import com.duriseo.android.feature.anniversary.FakeMeRepository
import com.duriseo.android.feature.anniversary.MyId
import com.duriseo.android.feature.anniversary.PartnerId
import com.duriseo.android.feature.anniversary.Offline
import com.duriseo.android.feature.anniversary.auto
import com.duriseo.android.feature.anniversary.coupleMe
import com.duriseo.android.feature.anniversary.custom
import com.duriseo.android.feature.anniversary.past
import com.duriseo.android.feature.anniversary.soloMe
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AnniversaryListViewModelTest {

    /** 2026-10-12(월) 오전 10:20, 서울. 처음 만난 날 2026-04-07이면 D+189 */
    private val clock = Clock.fixed(Instant.parse("2026-10-12T01:20:00Z"), ZoneId.of("Asia/Seoul"))
    private val me = FakeMeRepository(coupleMe())
    private val couple = FakeCoupleRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: FakeAnniversaryRepository) = AnniversaryListViewModel(repo, me, couple, clock)

    private val d1023 = LocalDate.of(2026, 10, 23)

    @Test
    fun `FR-ANNIV-01 AC1 커플은 함께한 날 카드에 D+N을 보인다`() = runTest {
        val vm = viewModel(FakeAnniversaryRepository(listOf(auto("200일", d1023, 11))))

        val card = vm.uiState.value.together
        assertNotNull(card)
        assertEquals(189L, card!!.dayNumber)
        assertEquals(LocalDate.of(2026, 4, 7), card.startDate)
        assertEquals("pick-orange", card.meColorKey)
        assertEquals("pick-blue", card.partnerColorKey)
    }

    @Test
    fun `FR-ANNIV-01 AC3 처음 만난 날이 오늘보다 뒤로 보이면 D+1이다`() = runTest {
        me.me.value = coupleMe(startDate = LocalDate.of(2026, 10, 13))

        assertEquals(1L, viewModel(FakeAnniversaryRepository()).uiState.value.together!!.dayNumber)
    }

    @Test
    fun `FR-ANNIV-04 AC2 개인 공간은 카드 없이 직접 기념일만 보인다`() = runTest {
        me.me.value = soloMe()
        val vm = viewModel(FakeAnniversaryRepository(listOf(custom("a1", "엄마 생신", LocalDate.of(2026, 11, 12), 31))))

        val state = vm.uiState.value
        assertFalse(state.isCouple)
        assertNull(state.together)
        // 개인 공간에는 처음 만난 날 안내도 없다
        assertFalse(state.showStartDatePrompt)
        assertEquals(listOf("엄마 생신"), state.items.map { it.name })
        assertFalse(state.isEmpty)
    }

    @Test
    fun `FR-ANNIV-04 같은 날 같은 이름의 기념일이 있어도 목록 key는 겹치지 않는다`() = runTest {
        val vm = viewModel(FakeAnniversaryRepository(listOf(auto("우리 기념일", d1023), auto("우리 기념일", d1023))))

        val keys = vm.uiState.value.items.map { it.key }

        assertEquals(2, keys.size)
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `FR-ANNIV-04 AC2 개인 공간에 기념일이 없으면 빈 화면이다`() = runTest {
        me.me.value = soloMe()

        assertTrue(viewModel(FakeAnniversaryRepository()).uiState.value.isEmpty)
    }

    @Test
    fun `FR-ANNIV-04 AC1 daysLeft 0은 D-day이고 30일 이내는 강조한다`() = runTest {
        val vm = viewModel(
            FakeAnniversaryRepository(
                listOf(
                    custom("a1", "오늘", LocalDate.of(2026, 10, 12), daysLeft = 0),
                    auto("200일", d1023, daysLeft = 30),
                    custom("a2", "먼 날", LocalDate.of(2026, 12, 5), daysLeft = 54),
                ),
            ),
        )

        val items = vm.uiState.value.items
        assertTrue(items[0].isToday)
        assertTrue(items[0].isNear)
        assertTrue(items[1].isNear)
        assertFalse(items[1].isToday)
        assertFalse(items[2].isNear)
    }

    @Test
    fun `FR-ANNIV-04 AC5 같은 날 순서는 서버 응답 순서 그대로다`() = runTest {
        val vm = viewModel(
            FakeAnniversaryRepository(
                listOf(
                    auto("200일", d1023),
                    custom("z", "가", d1023),
                    custom("a", "A", d1023),
                ),
            ),
        )

        assertEquals(listOf("200일", "가", "A"), vm.uiState.value.items.map { it.name })
    }

    @Test
    fun `FR-ANNIV-02 AC3 자동 기념일은 눌러도 이동하지 않는다`() = runTest {
        val vm = viewModel(FakeAnniversaryRepository(listOf(auto("200일", d1023))))

        vm.effects.test {
            vm.onAction(AnniversaryListAction.ItemClicked(vm.uiState.value.items.single()))
            expectNoEvents()
        }
        assertFalse(vm.uiState.value.items.single().editable)
    }

    @Test
    fun `FR-ANNIV-02 AC3 처음 만난 날이 바뀌면 목록을 다시 불러온다`() = runTest {
        val repo = FakeAnniversaryRepository(listOf(auto("200일", d1023)))
        viewModel(repo)
        assertEquals(0, repo.refreshAllCalls)

        me.me.value = coupleMe(startDate = LocalDate.of(2026, 5, 1))

        assertEquals(1, repo.refreshAllCalls)
    }

    @Test
    fun `FR-ANNIV-04 화면에 들어가면 첫 페이지부터 다시 불러온다`() = runTest {
        val repo = FakeAnniversaryRepository()
        viewModel(repo)

        assertEquals(1, repo.refreshUpcomingCalls)
        assertEquals(1, repo.refreshPastCalls)
    }

    @Test
    fun `FR-ANNIV-03 AC2 상대가 만든 기념일은 눌러도 이동하지 않는다`() = runTest {
        val vm = viewModel(
            FakeAnniversaryRepository(
                listOf(
                    custom("m1", "내 기념일", LocalDate.of(2026, 11, 20), authorId = MyId),
                    custom("p1", "처음 여행 간 날", LocalDate.of(2026, 12, 5), authorId = PartnerId),
                ),
            ),
        )
        val (mine, partners) = vm.uiState.value.items

        assertTrue(mine.editable)
        assertFalse(partners.editable)
        assertEquals(AnniversaryAuthor.PARTNER, partners.author)
        vm.effects.test {
            vm.onAction(AnniversaryListAction.ItemClicked(partners))
            expectNoEvents()
            vm.onAction(AnniversaryListAction.ItemClicked(mine))
            assertEquals(AnniversaryListSideEffect.OpenEdit("m1", LocalDate.of(2026, 11, 20)), awaitItem())
        }
    }

    @Test
    fun `FR-ANNIV-04 AC3 지난 기념일은 접혀 있고 내 항목만 수정으로 간다`() = runTest {
        val vm = viewModel(
            FakeAnniversaryRepository(
                pastItems = listOf(
                    past("p1", "제주 여행 출발", LocalDate.of(2026, 8, 1), authorId = PartnerId),
                    past("p2", "처음 같이 본 영화", LocalDate.of(2026, 6, 14), authorId = MyId),
                ),
            ),
        )

        val closed = vm.uiState.value
        assertFalse(closed.pastOpen)
        assertEquals(2, closed.pastCount)
        assertTrue(closed.showPastRow)

        vm.onAction(AnniversaryListAction.PastToggled)
        val open = vm.uiState.value
        assertTrue(open.pastOpen)
        val (partners, mine) = open.pastItems
        assertFalse(partners.editable)
        assertTrue(mine.editable)

        vm.effects.test {
            vm.onAction(AnniversaryListAction.PastItemClicked(partners))
            expectNoEvents()
            vm.onAction(AnniversaryListAction.PastItemClicked(mine))
            assertEquals(AnniversaryListSideEffect.OpenEdit("p2", LocalDate.of(2026, 6, 14)), awaitItem())
        }
    }

    @Test
    fun `FR-ANNIV-04 AC3 지난 기념일이 없으면 행을 숨긴다`() = runTest {
        assertFalse(viewModel(FakeAnniversaryRepository()).uiState.value.showPastRow)
    }

    @Test
    fun `FR-ANNIV-04 AC4 처음 만난 날이 없는 커플은 안내 카드를 보인다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        val vm = viewModel(FakeAnniversaryRepository(listOf(custom("a1", "처음 여행 간 날", LocalDate.of(2026, 12, 5)))))

        val state = vm.uiState.value
        assertTrue(state.isCouple)
        assertNull(state.together)
        assertTrue(state.showStartDatePrompt)
        assertEquals(listOf("처음 여행 간 날"), state.items.map { it.name })
    }

    @Test
    fun `FR-ANNIV-04 AC4 처음 만난 날 입력을 누르면 시트가 열리고 저장하면 PATCH space를 보낸다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        val vm = viewModel(FakeAnniversaryRepository())

        vm.onAction(AnniversaryListAction.StartDateInputClicked)
        val sheet = vm.uiState.value.startDateSheet
        assertNotNull(sheet)
        assertEquals(YearMonth.of(2026, 10), sheet!!.visibleMonth)
        assertNull(sheet.selected)

        vm.onAction(AnniversaryListAction.StartDateSelected(LocalDate.of(2026, 4, 7)))
        // 오늘(2026-10-12) 이후는 고를 수 없다
        vm.onAction(AnniversaryListAction.StartDateSelected(LocalDate.of(2026, 10, 13)))
        assertEquals(LocalDate.of(2026, 4, 7), vm.uiState.value.startDateSheet!!.selected)

        vm.onAction(AnniversaryListAction.StartDateSaved)

        assertEquals(listOf(LocalDate.of(2026, 4, 7)), couple.startDateCalls)
        assertNull(vm.uiState.value.startDateSheet)
    }

    @Test
    fun `D-115 기념일 목록은 요청 중에는 loading이 true이고 끝나면 false다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        val gate = CompletableDeferred<Unit>()
        couple.startDateGate = gate
        val vm = viewModel(FakeAnniversaryRepository())
        vm.onAction(AnniversaryListAction.StartDateInputClicked)
        vm.onAction(AnniversaryListAction.StartDateSelected(LocalDate.of(2026, 4, 7)))
        assertFalse(vm.uiState.value.loading)

        vm.onAction(AnniversaryListAction.StartDateSaved)
        assertTrue(vm.uiState.value.loading)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `FR-ANNIV-04 AC4 처음 만난 날 저장이 실패하면 시트를 두고 안내한다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        couple.startDateResult = Offline
        val vm = viewModel(FakeAnniversaryRepository())
        vm.onAction(AnniversaryListAction.StartDateInputClicked)
        vm.onAction(AnniversaryListAction.StartDateSelected(LocalDate.of(2026, 4, 7)))

        vm.effects.test {
            vm.onAction(AnniversaryListAction.StartDateSaved)
            assertEquals(AnniversaryListSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
        val sheet = vm.uiState.value.startDateSheet
        assertNotNull(sheet)
        assertFalse(sheet!!.saving)
    }

    @Test
    fun `FR-ANNIV-04 AC1 목록 끝까지 스크롤하면 다음 페이지를 이어 붙인다`() = runTest {
        val repo = FakeAnniversaryRepository(listOf(auto("200일", d1023)), hasMoreUpcoming = true)
        val vm = viewModel(repo)

        vm.onAction(AnniversaryListAction.UpcomingEndReached)
        assertEquals(1, repo.loadMoreUpcomingCalls)

        // 다음 페이지가 없으면 요청하지 않는다
        repo.upcoming.value = repo.upcoming.value.copy(hasMore = false)
        vm.onAction(AnniversaryListAction.UpcomingEndReached)
        assertEquals(1, repo.loadMoreUpcomingCalls)
    }

    @Test
    fun `SCR-ANNIV-01 추가 버튼과 뒤로 가기는 이동 요청을 낸다`() = runTest {
        val vm = viewModel(FakeAnniversaryRepository())

        vm.effects.test {
            vm.onAction(AnniversaryListAction.AddClicked)
            assertEquals(AnniversaryListSideEffect.OpenCreate, awaitItem())
            vm.onAction(AnniversaryListAction.BackClicked)
            assertEquals(AnniversaryListSideEffect.Back, awaitItem())
        }
    }
}
