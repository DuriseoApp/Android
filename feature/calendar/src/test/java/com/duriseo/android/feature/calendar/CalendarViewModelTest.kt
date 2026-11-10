package com.duriseo.android.feature.calendar

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.event.EventTarget
import com.duriseo.android.domain.event.EventTime
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val me = FakeMeRepository(coupleMe())
    private val couple = FakeCoupleRepository()
    private val events = FakeEventRepository()
    private val anniversaries = FakeAnniversaryRepository()

    /** 2026-10-12(월) 오전 10:20, 서울 */
    private val clock = Clock.fixed(Instant.parse("2026-10-12T01:20:00Z"), ZoneId.of("Asia/Seoul"))
    private val today = LocalDate.of(2026, 10, 12)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): CalendarViewModel {
        val vm = CalendarViewModel(events, anniversaries, me, couple, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    @Test
    fun `FR-CAL-01 AC1 처음 열면 오늘이 속한 달이 보이고 오늘이 선택돼 있다`() = runTest {
        val state = viewModel().uiState.value
        assertEquals(YearMonth.of(2026, 10), state.visibleMonth)
        assertEquals(today, state.selectedDate)
        assertEquals(today, state.today)
    }

    @Test
    fun `FR-CAL-01 AC2 일정이 있는 날에 사람 색 점이 보인다`() = runTest {
        events.events.value = listOf(sampleEvent(id = "a"), sampleEvent(id = "b", isTogether = true, authorId = PartnerId))
        val state = viewModel().uiState.value
        val dots = state.eventsByDay[today].orEmpty().map { it.target }
        assertEquals(setOf(EventTarget.ME, EventTarget.TOGETHER), dots.toSet())
        assertNull(state.eventsByDay[today.plusDays(1)])
    }

    @Test
    fun `FR-CAL-01 AC2 날짜를 누르면 그 날의 일정이 종일 먼저 시작 시간순으로 나온다`() = runTest {
        val tenth = LocalDate.of(2026, 10, 14)
        events.events.value = listOf(
            sampleEvent(id = "late", title = "늦은", time = timed("2026-10-14T11:00:00Z", "2026-10-14T12:00:00Z")),
            sampleEvent(id = "early", title = "이른", time = timed("2026-10-14T01:00:00Z", "2026-10-14T02:00:00Z")),
            sampleEvent(id = "all", title = "종일", time = allDay(tenth)),
        )
        val vm = viewModel()
        vm.onAction(CalendarAction.DateSelected(tenth))
        assertEquals(listOf("all", "early", "late"), vm.uiState.value.selectedEvents.map { it.id })
    }

    @Test
    fun `FR-CAL-01 AC2 선택한 날에 일정이 없으면 빈 안내가 보인다`() = runTest {
        val vm = viewModel()
        assertTrue(vm.uiState.value.selectedEvents.isEmpty())
    }

    @Test
    fun `FR-CAL-02 AC5 여러 날 일정은 걸친 모든 날에 표시된다`() = runTest {
        events.events.value = listOf(sampleEvent(id = "trip", time = allDay(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 19))))
        val byDay = viewModel().uiState.value.eventsByDay
        assertEquals(listOf(17, 18, 19), byDay.keys.map { it.dayOfMonth }.sorted())
    }

    @Test
    fun `FR-CAL-04 AC6 일정 카드에 대상 라벨이 함께 보인다`() = runTest {
        events.events.value = listOf(sampleEvent(id = "p", authorId = PartnerId))
        assertEquals(EventTarget.PARTNER, viewModel().uiState.value.selectedEvents.single().target)
    }

    @Test
    fun `FR-CAL-01 AC3 커플이고 처음 만난 날이 있으면 D+N이 보인다`() = runTest {
        me.me.value = coupleMe(startDate = LocalDate.of(2026, 4, 7))
        val header = viewModel().uiState.value.header as CalendarHeader.Couple
        assertEquals(189L, header.dayNumber)
    }

    @Test
    fun `FR-ANNIV-01 AC1 처음 만난 날 당일은 D+1이다`() = runTest {
        me.me.value = coupleMe(startDate = today)
        val header = viewModel().uiState.value.header as CalendarHeader.Couple
        assertEquals(1L, header.dayNumber)
    }

    @Test
    fun `FR-CAL-01 AC5 개인 공간은 내 아바타와 나의 캘린더가 보인다`() = runTest {
        me.me.value = soloMe()
        assertEquals(CalendarHeader.Solo("pick-orange"), viewModel().uiState.value.header)
    }

    @Test
    fun `FR-ANNIV-01 AC2 개인 공간에는 D-day가 없다`() = runTest {
        me.me.value = soloMe()
        assertFalse(viewModel().uiState.value.header is CalendarHeader.Couple)
    }

    @Test
    fun `FR-COUPLE-04 AC2 처음 만난 날이 없는 커플은 앱바에 입력 안내가 보인다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        val header = viewModel().uiState.value.header as CalendarHeader.Couple
        assertNull(header.dayNumber)
    }

    @Test
    fun `FR-COUPLE-04 AC2 입력 안내를 누르면 처음 만난 날 시트를 연다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        val vm = viewModel()
        vm.onAction(CalendarAction.StartDatePromptClicked)
        assertNotNull(vm.uiState.value.startDateSheet)
    }

    @Test
    fun `FR-COUPLE-04 AC2 시트에서 저장하면 PATCH space를 보내고 D+N이 보인다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        val vm = viewModel()
        vm.onAction(CalendarAction.StartDatePromptClicked)
        vm.onAction(CalendarAction.StartDateSelected(LocalDate.of(2026, 4, 7)))
        vm.onAction(CalendarAction.StartDateSaved)
        assertEquals(listOf(LocalDate.of(2026, 4, 7)), couple.startDateCalls)
        assertNull(vm.uiState.value.startDateSheet)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 처음 만난 날 저장을 안내하고 시트를 그대로 둔다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        couple.startDateResult = Offline
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(CalendarAction.StartDatePromptClicked)
            vm.onAction(CalendarAction.StartDateSelected(LocalDate.of(2026, 4, 7)))
            vm.onAction(CalendarAction.StartDateSaved)
            assertEquals(CalendarSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
        assertNotNull(vm.uiState.value.startDateSheet)
        assertFalse(vm.uiState.value.startDateSheet!!.saving)
    }

    @Test
    fun `FR-CAL-01 AC6 1950년 1월에서는 이전 달 버튼이 꺼지고 2050년 12월에서는 다음 달 버튼이 꺼진다`() = runTest {
        val vm = viewModel()
        vm.onAction(CalendarAction.DateSelected(LocalDate.of(1950, 1, 15)))
        assertFalse(vm.uiState.value.canGoPrevious)
        assertTrue(vm.uiState.value.canGoNext)
        vm.onAction(CalendarAction.DateSelected(LocalDate.of(2050, 12, 15)))
        assertFalse(vm.uiState.value.canGoNext)
        vm.onAction(CalendarAction.PreviousMonth)
        assertTrue(vm.uiState.value.canGoNext)
    }

    @Test
    fun `D-112 화면에 들어오면 보이는 달을 다시 불러온다`() = runTest {
        val vm = viewModel()
        vm.onAction(CalendarAction.Shown)
        // 앞뒤 하루 여유 (Q-19 ②)
        assertEquals(listOf(LocalDate.of(2026, 9, 30) to LocalDate.of(2026, 11, 1)), events.refreshRanges)
        assertEquals(listOf(LocalDate.of(2026, 9, 30) to LocalDate.of(2026, 11, 1)), anniversaries.refreshRanges)
    }

    @Test
    fun `D-112 FR-CAL-01 AC7 불러오기가 실패하거나 오프라인이어도 안내 없이 저장된 일정을 그대로 보여 준다`() = runTest {
        events.events.value = listOf(sampleEvent(time = allDay(LocalDate.of(2026, 10, 12))))
        events.refreshRangeResult = Offline
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(CalendarAction.Shown)
            vm.onAction(CalendarAction.NextMonth)
            vm.onAction(CalendarAction.PreviousMonth)
            expectNoEvents()
        }
        assertEquals(1, vm.uiState.value.eventsByDay.size)
    }

    @Test
    fun `D-113 FR-CAL-02 AC5 여러 날 시간 일정은 걸친 모든 날에 시작 시각이 보인다`() = runTest {
        // 2026-10-17 10:00 ~ 10-19 12:00 (서울)
        val trip = EventTime.Timed(Instant.parse("2026-10-17T01:00:00Z"), Instant.parse("2026-10-19T03:00:00Z"))
        events.events.value = listOf(sampleEvent(id = "trip", time = trip))
        val byDay = viewModel().uiState.value.eventsByDay
        assertEquals(listOf(17, 18, 19), byDay.keys.map { it.dayOfMonth }.sorted())
        assertEquals(listOf(LocalTime.of(10, 0)), byDay.values.flatten().map { it.startTime }.distinct())
    }

    @Test
    fun `NFR-SYNC-02 월을 넘기면 그 월을 다시 불러온다`() = runTest {
        val vm = viewModel()
        vm.onAction(CalendarAction.Shown)
        vm.onAction(CalendarAction.NextMonth)
        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 30) to LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 10, 31) to LocalDate.of(2026, 12, 1),
            ),
            events.refreshRanges,
        )
        assertEquals(events.refreshRanges, anniversaries.refreshRanges)
        assertEquals(YearMonth.of(2026, 11), vm.uiState.value.visibleMonth)
    }

    @Test
    fun `FR-CAL-02 AC1 날짜 목록의 더하기는 선택한 날로 작성 화면을 연다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(CalendarAction.AddClicked)
            assertEquals(CalendarSideEffect.OpenCreate(today), awaitItem())
        }
    }

    @Test
    fun `FR-CAL-03 일정을 누르면 상세를 연다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(CalendarAction.EventClicked("e1"))
            assertEquals(CalendarSideEffect.OpenEvent("e1"), awaitItem())
        }
    }

    @Test
    fun `FR-CAL-01 크게 보기 토글은 펼침과 접힘을 바꾼다`() = runTest {
        val vm = viewModel()
        vm.onAction(CalendarAction.ToggleExpanded)
        assertTrue(vm.uiState.value.expanded)
        vm.onAction(CalendarAction.ToggleExpanded)
        assertFalse(vm.uiState.value.expanded)
    }

    // --- 단위 5 묶음 B: 기념일 표시 ---

    @Test
    fun `FR-CAL-01 AC2 날짜 목록은 기념일 종일 시간 순이다`() = runTest {
        val day = LocalDate.of(2026, 10, 14)
        anniversaries.occurrences.value = listOf(
            customAnniversary("내 기념일", day),
            autoAnniversary("100일", day),
        )
        events.events.value = listOf(
            sampleEvent(id = "late", time = timed("2026-10-14T11:00:00Z", "2026-10-14T12:00:00Z")),
            sampleEvent(id = "early", time = timed("2026-10-14T01:00:00Z", "2026-10-14T02:00:00Z")),
            sampleEvent(id = "all-b", time = allDay(day)),
            // 10-13 오후 6시(서울)에 시작해 14일로 이어지는 일정: 14일 목록에는 18:00으로 보이므로 early·late 사이에 온다
            sampleEvent(id = "trip", time = timed("2026-10-13T09:00:00Z", "2026-10-14T12:00:00Z")),
            sampleEvent(id = "all-a", time = allDay(LocalDate.of(2026, 10, 13), day)),
        )
        val vm = viewModel()
        vm.onAction(CalendarAction.DateSelected(day))
        val keys = vm.uiState.value.selectedItems.map {
            when (it) {
                is CalendarListItem.Anniversary -> "기념일:${it.value.name}"
                is CalendarListItem.Event -> it.value.id
            }
        }
        // 기념일은 저장소 순서 그대로(앱이 다시 정렬하지 않는다), 종일은 시작 날짜순, 시간은 보이는 시각순
        assertEquals(listOf("기념일:내 기념일", "기념일:100일", "all-a", "all-b", "early", "trip", "late"), keys)
    }

    @Test
    fun `FR-CAL-01 AC2 자동 기념일 카드는 그날의 D+N이다`() = runTest {
        // 처음 만난 날 2026-04-07: 2026-10-23은 199일 뒤라 D+200 (D-117, 오늘 기준이 아니다)
        me.me.value = coupleMe(startDate = LocalDate.of(2026, 4, 7))
        anniversaries.occurrences.value = listOf(
            autoAnniversary("200일", LocalDate.of(2026, 10, 23)),
            customAnniversary("처음 손잡은 날", LocalDate.of(2026, 10, 23), authorId = PartnerId),
        )
        val vm = viewModel()
        vm.onAction(CalendarAction.DateSelected(LocalDate.of(2026, 10, 23)))
        val (auto, custom) = vm.uiState.value.selectedAnniversaries
        assertTrue(auto.isAuto)
        assertEquals(200L, auto.dayNumber)
        assertNull(auto.author)
        assertFalse(custom.isAuto)
        assertNull(custom.dayNumber)
        assertEquals(EventTarget.PARTNER, custom.author)
    }

    @Test
    fun `FR-CAL-01 AC2 직접 기념일은 내가 만들었으면 나 상대가 만들었으면 상대 색이다`() = runTest {
        val day = LocalDate.of(2026, 10, 20)
        anniversaries.occurrences.value = listOf(
            customAnniversary("내 것", day, authorId = MyId),
            customAnniversary("상대 것", day, authorId = PartnerId),
        )
        val vm = viewModel()
        vm.onAction(CalendarAction.DateSelected(day))
        assertEquals(listOf(EventTarget.ME, EventTarget.PARTNER), vm.uiState.value.selectedAnniversaries.map { it.author })
    }

    @Test
    fun `FR-CAL-01 AC2 앞뒤 하루 여유로 받은 이웃 달 기념일은 달력에 넣지 않는다`() = runTest {
        anniversaries.occurrences.value = listOf(
            customAnniversary("전달 말일", LocalDate.of(2026, 9, 30)),
            customAnniversary("이번 달", LocalDate.of(2026, 10, 1)),
            customAnniversary("다음 달 1일", LocalDate.of(2026, 11, 1)),
        )
        val byDay = viewModel().uiState.value.anniversariesByDay
        assertEquals(listOf(LocalDate.of(2026, 10, 1)), byDay.keys.toList())
    }

    @Test
    fun `FR-CAL-01 AC2 기념일 카드를 누르면 기념일 화면을 연다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(CalendarAction.AnniversariesClicked)
            assertEquals(CalendarSideEffect.OpenAnniversaries, awaitItem())
        }
    }

    @Test
    fun `FR-ANNIV-01 AC3 앱바 D+N은 최소 D+1이다`() = runTest {
        // 시간대가 달라 처음 만난 날이 오늘보다 하루 뒤로 계산되는 경우: 계산값 0이 아니라 1
        me.me.value = coupleMe(startDate = today.plusDays(1))
        val header = viewModel().uiState.value.header as CalendarHeader.Couple
        assertEquals(1L, header.dayNumber)
    }

    @Test
    fun `FR-ANNIV-02 AC3 처음 만난 날 저장 뒤 기념일을 다시 불러온다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        val vm = viewModel()
        vm.onAction(CalendarAction.StartDatePromptClicked)
        vm.onAction(CalendarAction.StartDateSelected(LocalDate.of(2026, 4, 7)))
        assertTrue(anniversaries.refreshRanges.isEmpty())
        vm.onAction(CalendarAction.StartDateSaved)
        assertEquals(listOf(LocalDate.of(2026, 9, 30) to LocalDate.of(2026, 11, 1)), anniversaries.refreshRanges)
    }

    @Test
    fun `FR-ANNIV-02 AC3 처음 만난 날 저장이 실패하면 기념일을 다시 불러오지 않는다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        couple.startDateResult = Offline
        val vm = viewModel()
        vm.onAction(CalendarAction.StartDatePromptClicked)
        vm.onAction(CalendarAction.StartDateSelected(LocalDate.of(2026, 4, 7)))
        vm.onAction(CalendarAction.StartDateSaved)
        assertTrue(anniversaries.refreshRanges.isEmpty())
    }

    @Test
    fun `NFR-SYNC-02 당겨서 새로고침하면 보이는 달을 다시 불러온다`() = runTest {
        val vm = viewModel()
        vm.onAction(CalendarAction.Refresh)
        val range = LocalDate.of(2026, 9, 30) to LocalDate.of(2026, 11, 1)
        assertEquals(listOf(range), events.refreshRanges)
        assertEquals(listOf(range), anniversaries.refreshRanges)
        assertFalse(vm.uiState.value.refreshing)
    }

    @Test
    fun `Q-16 당겨서 새로고침하는 동안에만 refreshing이 true다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        anniversaries.refreshGate = gate
        val vm = viewModel()
        vm.onAction(CalendarAction.Refresh)
        assertTrue(vm.uiState.value.refreshing)
        // 요청 중에 또 당겨도 다시 요청하지 않는다
        vm.onAction(CalendarAction.Refresh)
        assertEquals(1, anniversaries.refreshRanges.size)
        gate.complete(Unit)
        assertFalse(vm.uiState.value.refreshing)
    }

    @Test
    fun `Q-16 화면에 들어올 때와 달을 넘길 때의 다시 불러오기는 새로고침 표시를 켜지 않는다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        anniversaries.refreshGate = gate
        val vm = viewModel()
        vm.onAction(CalendarAction.Shown)
        vm.onAction(CalendarAction.NextMonth)
        assertFalse(vm.uiState.value.refreshing)
        gate.complete(Unit)
    }

    @Test
    fun `Q-16 오프라인이어도 당겨서 새로고침은 안내 없이 끝난다`() = runTest {
        events.refreshRangeResult = Offline
        anniversaries.refreshRangeResult = Offline
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(CalendarAction.Refresh)
            expectNoEvents()
        }
        assertFalse(vm.uiState.value.refreshing)
    }

    @Test
    fun `Q-19 월 조회는 앞뒤 하루를 더해 요청한다`() = runTest {
        val vm = viewModel()
        vm.onAction(CalendarAction.Shown)
        // 10월: 9월 30일 ~ 11월 1일, 일정(GET events)과 기념일(GET anniversaries) 모두
        assertEquals(listOf(LocalDate.of(2026, 9, 30) to LocalDate.of(2026, 11, 1)), events.refreshRanges)
        assertEquals(events.refreshRanges, anniversaries.refreshRanges)
        // 3개월 상한 안이다
        val (from, to) = events.refreshRanges.single()
        assertTrue(java.time.Period.between(from, to).toTotalMonths() < 3)
    }

    private fun timed(start: String, end: String) =
        com.duriseo.android.domain.event.EventTime.Timed(Instant.parse(start), Instant.parse(end))
}
