package com.duriseo.android.feature.couple.linkedsheet

import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.me.ImportCounts
import com.duriseo.android.domain.me.SpaceChange
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.couple.FakeCoupleRepository
import com.duriseo.android.feature.couple.FakeMeRepository
import com.duriseo.android.feature.couple.SchedulerClock
import com.duriseo.android.feature.couple.collectInto
import com.duriseo.android.feature.couple.failure
import com.duriseo.android.feature.couple.offline
import com.duriseo.android.feature.couple.testMe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** SCR-COUPLE-04 연결 완료 시트 (FR-COUPLE-03, FR-COUPLE-04 AC3·AC4, FR-COUPLE-05 AC5, D-93, D-98) */
@OptIn(ExperimentalCoroutinesApi::class)
class LinkedSheetViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val couple = FakeCoupleRepository()
    private val counts = ImportCounts(events = 3, anniversaries = 0, memos = 2)

    /** 오늘은 2026-10-31 */
    private val clock = SchedulerClock(scheduler, Instant.parse("2026-10-31T03:00:00Z"))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun meWithRecords() = testMe(type = SpaceType.COUPLE, importPending = true, importCounts = counts)

    private fun meWithoutRecords(startDate: LocalDate? = null) = testMe(type = SpaceType.COUPLE, startDate = startDate)

    private fun viewModel(me: FakeMeRepository) = LinkedSheetViewModel(couple, me, clock)

    @Test
    fun `D-93 제목에 상대 이름이 들어가고 혼자 쓴 기록 개수를 0개인 종류까지 보여 준다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithRecords()))

        val state = vm.uiState.value

        assertEquals("둘", state.partnerName)
        assertTrue(state.hasRecords)
        assertEquals(ImportCounts(events = 3, anniversaries = 0, memos = 2), state.counts)
    }

    @Test
    fun `FR-COUPLE-03 AC5 시트를 처음 열면 가져오기가 선택돼 있다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithRecords()))

        assertEquals(ImportChoice.Keep, vm.uiState.value.choice)
    }

    @Test
    fun `FR-COUPLE-03 AC4 가져올 기록이 있으면 시작하기 전에는 시트를 닫을 수 없다`() = runTest {
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)

        vm.onAction(LinkedSheetAction.CloseClicked)
        vm.onAction(LinkedSheetAction.BackPressed)

        assertFalse(vm.uiState.value.canDismiss)
        assertEquals(0, me.acknowledgeCalls)
    }

    @Test
    fun `FR-COUPLE-03 AC4 기록이 없으면 닫을 수 있고 닫으면 확인한 공간을 저장한다`() = runTest {
        val me = FakeMeRepository(meWithoutRecords())
        val vm = viewModel(me)

        assertTrue(vm.uiState.value.canDismiss)
        vm.onAction(LinkedSheetAction.CloseClicked)

        assertEquals(1, me.acknowledgeCalls)
        assertTrue(couple.writeOrder.isEmpty())
    }

    @Test
    fun `FR-COUPLE-03 AC4 기록이 없을 때 뒤로 가기는 시트를 닫는다`() = runTest {
        val me = FakeMeRepository(meWithoutRecords())
        val vm = viewModel(me)

        vm.onAction(LinkedSheetAction.BackPressed)

        assertEquals(1, me.acknowledgeCalls)
    }

    @Test
    fun `FR-COUPLE-03 AC1 가져오기로 시작하면 import true를 보내고 시트를 닫는다`() = runTest {
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)

        vm.onAction(LinkedSheetAction.StartClicked)

        assertEquals(listOf(true), couple.importCalls)
        assertEquals(1, me.acknowledgeCalls)
        assertFalse(vm.uiState.value.requesting)
    }

    @Test
    fun `FR-COUPLE-03 AC2 가져오지 않기로 시작하면 확인 다이얼로그를 먼저 보여 준다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithRecords()))
        vm.onAction(LinkedSheetAction.ChoiceSelected(ImportChoice.Drop))

        vm.onAction(LinkedSheetAction.StartClicked)

        assertTrue(vm.uiState.value.dropDialogVisible)
        assertTrue(couple.importCalls.isEmpty())
    }

    @Test
    fun `FR-COUPLE-03 AC2 삭제하고 시작을 누르면 import false를 보낸다`() = runTest {
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        vm.onAction(LinkedSheetAction.ChoiceSelected(ImportChoice.Drop))
        vm.onAction(LinkedSheetAction.StartClicked)

        vm.onAction(LinkedSheetAction.DropConfirmed)

        assertEquals(listOf(false), couple.importCalls)
        assertFalse(vm.uiState.value.dropDialogVisible)
        assertEquals(1, me.acknowledgeCalls)
    }

    @Test
    fun `FR-COUPLE-03 AC2 다이얼로그에서 취소하면 아무것도 보내지 않는다`() = runTest {
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        vm.onAction(LinkedSheetAction.ChoiceSelected(ImportChoice.Drop))
        vm.onAction(LinkedSheetAction.StartClicked)

        vm.onAction(LinkedSheetAction.DropCancelled)

        assertFalse(vm.uiState.value.dropDialogVisible)
        assertTrue(couple.writeOrder.isEmpty())
        assertEquals(0, me.acknowledgeCalls)
    }

    @Test
    fun `FR-COUPLE-03 AC2 다이얼로그가 떠 있을 때 뒤로 가기는 다이얼로그만 닫는다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithRecords()))
        vm.onAction(LinkedSheetAction.ChoiceSelected(ImportChoice.Drop))
        vm.onAction(LinkedSheetAction.StartClicked)

        vm.onAction(LinkedSheetAction.BackPressed)

        assertFalse(vm.uiState.value.dropDialogVisible)
    }

    @Test
    fun `FR-COUPLE-04 AC3 선택 시트에서 오늘보다 뒤 날짜는 고를 수 없다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithoutRecords()))
        vm.onAction(LinkedSheetAction.DateFieldClicked)

        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(2026, 11, 1)))

        assertNull(vm.uiState.value.datePicker!!.selected)
    }

    @Test
    fun `FR-COUPLE-04 AC3 선택 시트에서 1950년 1월 1일보다 앞 날짜는 고를 수 없다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithoutRecords()))
        vm.onAction(LinkedSheetAction.DateFieldClicked)

        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(1949, 12, 31)))
        assertNull(vm.uiState.value.datePicker!!.selected)
        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(1950, 1, 1)))

        assertEquals(LocalDate.of(1950, 1, 1), vm.uiState.value.datePicker!!.selected)
    }

    @Test
    fun `FR-COUPLE-04 AC3 연도 월 선택으로 고른 달로 달력이 바뀌고 미래 달과 1950년 전 달은 받지 않는다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithoutRecords()))
        vm.onAction(LinkedSheetAction.DateFieldClicked)

        vm.onAction(LinkedSheetAction.MonthSelected(YearMonth.of(2019, 3)))
        assertEquals(YearMonth.of(2019, 3), vm.uiState.value.datePicker!!.visibleMonth)

        vm.onAction(LinkedSheetAction.MonthSelected(YearMonth.of(2026, 11)))
        vm.onAction(LinkedSheetAction.MonthSelected(YearMonth.of(1949, 12)))
        assertEquals(YearMonth.of(2019, 3), vm.uiState.value.datePicker!!.visibleMonth)
    }

    @Test
    fun `FR-COUPLE-04 AC3 선택 시트는 오늘이 든 달에서 열리고 다음 달로 갈 수 없다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithoutRecords()))
        vm.onAction(LinkedSheetAction.DateFieldClicked)

        assertEquals(YearMonth.of(2026, 10), vm.uiState.value.datePicker!!.visibleMonth)
        vm.onAction(LinkedSheetAction.NextMonth)
        assertEquals(YearMonth.of(2026, 10), vm.uiState.value.datePicker!!.visibleMonth)
        vm.onAction(LinkedSheetAction.PreviousMonth)
        assertEquals(YearMonth.of(2026, 9), vm.uiState.value.datePicker!!.visibleMonth)
    }

    @Test
    fun `FR-COUPLE-04 AC4 선택 시트의 저장은 칸만 채우고 서버를 부르지 않는다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithoutRecords()))
        vm.onAction(LinkedSheetAction.DateFieldClicked)
        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(2026, 3, 14)))

        vm.onAction(LinkedSheetAction.DateSaved)

        assertEquals(LocalDate.of(2026, 3, 14), vm.uiState.value.startDate)
        assertNull(vm.uiState.value.datePicker)
        assertTrue(couple.startDateCalls.isEmpty())
    }

    @Test
    fun `FR-COUPLE-04 AC4 나중에 하기는 고른 날짜를 비운다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithoutRecords()))
        vm.onAction(LinkedSheetAction.DateFieldClicked)
        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(2026, 3, 14)))
        vm.onAction(LinkedSheetAction.DateSaved)

        vm.onAction(LinkedSheetAction.LaterClicked)

        assertNull(vm.uiState.value.startDate)
    }

    @Test
    fun `FR-COUPLE-04 AC4 처음 만난 날이 이미 있으면 칸에 보이고 나중에 하기가 없다`() = runTest {
        val vm = viewModel(FakeMeRepository(meWithoutRecords(startDate = LocalDate.of(2026, 3, 14))))

        val state = vm.uiState.value

        assertEquals(LocalDate.of(2026, 3, 14), state.startDate)
        assertFalse(state.showLater)
    }

    @Test
    fun `FR-COUPLE-04 AC4 시작하기는 처음 만난 날 다음 기록 선택 순서로 보낸다`() = runTest {
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        vm.onAction(LinkedSheetAction.DateFieldClicked)
        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(2026, 3, 14)))
        vm.onAction(LinkedSheetAction.DateSaved)

        vm.onAction(LinkedSheetAction.StartClicked)

        assertEquals(listOf("startDate", "import:true"), couple.writeOrder)
        assertEquals(listOf(LocalDate.of(2026, 3, 14)), couple.startDateCalls)
        assertEquals(1, me.acknowledgeCalls)
    }

    @Test
    fun `FR-COUPLE-04 AC4 날짜를 고르지 않고 시작해도 된다`() = runTest {
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)

        vm.onAction(LinkedSheetAction.StartClicked)

        assertTrue(couple.startDateCalls.isEmpty())
        assertEquals(listOf("import:true"), couple.writeOrder)
    }

    @Test
    fun `FR-COUPLE-04 AC4 이미 서버에 있는 날짜는 다시 보내지 않는다`() = runTest {
        val me = FakeMeRepository(
            testMe(
                type = SpaceType.COUPLE,
                startDate = LocalDate.of(2026, 3, 14),
                importPending = true,
                importCounts = counts,
            ),
        )
        val vm = viewModel(me)

        vm.onAction(LinkedSheetAction.StartClicked)

        assertTrue(couple.startDateCalls.isEmpty())
    }

    @Test
    fun `FR-COUPLE-03 AC6 기록이 없어도 날짜만 골라 시작하면 날짜를 저장하고 시트를 닫는다`() = runTest {
        val me = FakeMeRepository(meWithoutRecords())
        val vm = viewModel(me)
        vm.onAction(LinkedSheetAction.DateFieldClicked)
        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(2026, 3, 14)))
        vm.onAction(LinkedSheetAction.DateSaved)

        vm.onAction(LinkedSheetAction.StartClicked)

        assertEquals(listOf("startDate"), couple.writeOrder)
        assertEquals(1, me.acknowledgeCalls)
    }

    @Test
    fun `D-98 처음 만난 날 저장이 실패하면 거기서 멈추고 공통 오류를 보여 주며 시트는 그대로다`() = runTest {
        couple.startDateResult = failure("INTERNAL_ERROR")
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        val effects = collectInto(vm.effects)
        vm.onAction(LinkedSheetAction.DateFieldClicked)
        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(2026, 3, 14)))
        vm.onAction(LinkedSheetAction.DateSaved)

        vm.onAction(LinkedSheetAction.StartClicked)

        assertEquals(listOf<LinkedSheetSideEffect>(LinkedSheetSideEffect.ShowMessage(DesignR.string.error_common)), effects)
        assertEquals(listOf("startDate"), couple.writeOrder) // 기록 선택은 보내지 않는다
        assertEquals(0, me.acknowledgeCalls)
        assertFalse(vm.uiState.value.requesting)
        assertEquals(LocalDate.of(2026, 3, 14), vm.uiState.value.startDate)
    }

    @Test
    fun `D-98 기록 선택이 실패하면 시트는 그대로고 다시 누르면 처음부터 한다`() = runTest {
        couple.importResult = failure("INTERNAL_ERROR")
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        vm.onAction(LinkedSheetAction.StartClicked)
        assertEquals(0, me.acknowledgeCalls)

        couple.importResult = DataResult.Success(Unit)
        vm.onAction(LinkedSheetAction.StartClicked)

        assertEquals(listOf("import:true", "import:true"), couple.writeOrder)
        assertEquals(1, me.acknowledgeCalls)
    }

    @Test
    fun `D-98 NO_IMPORT_PENDING이면 이미 정한 것으로 보고 시트를 닫은 뒤 내 정보를 다시 읽는다`() = runTest {
        couple.importResult = failure("NO_IMPORT_PENDING")
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        val effects = collectInto(vm.effects)

        vm.onAction(LinkedSheetAction.StartClicked)

        assertEquals(1, me.acknowledgeCalls)
        assertEquals(1, me.refreshCalls)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `FR-COUPLE-05 AC5 시작하기 중 NOT_COUPLE이면 오류를 보여 주지 않고 시트도 닫지 않는다`() = runTest {
        couple.importResult = failure("NOT_COUPLE")
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        val effects = collectInto(vm.effects)

        vm.onAction(LinkedSheetAction.StartClicked)

        assertTrue(effects.isEmpty())
        assertEquals(0, me.acknowledgeCalls)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 시작하기가 안내로 끝나고 시트는 그대로다`() = runTest {
        couple.importResult = offline()
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        val effects = collectInto(vm.effects)

        vm.onAction(LinkedSheetAction.StartClicked)

        assertEquals(
            listOf<LinkedSheetSideEffect>(LinkedSheetSideEffect.ShowMessage(DesignR.string.offline_required)),
            effects,
        )
        assertEquals(0, me.acknowledgeCalls)
    }

    @Test
    fun `D-88 요청 중에는 시작하기가 다시 눌리지 않고 선택도 바뀌지 않는다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        couple.importGate = gate
        val vm = viewModel(FakeMeRepository(meWithRecords()))

        vm.onAction(LinkedSheetAction.StartClicked)
        vm.onAction(LinkedSheetAction.StartClicked)
        vm.onAction(LinkedSheetAction.ChoiceSelected(ImportChoice.Drop))

        assertTrue(vm.uiState.value.requesting)
        assertEquals(ImportChoice.Keep, vm.uiState.value.choice)
        assertEquals(listOf("import:true"), couple.writeOrder)
        gate.complete(Unit)
    }

    @Test
    fun `D-115 연결 완료 시트는 요청 중에는 loading이 true이고 끝나면 false다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        couple.importGate = gate
        val vm = viewModel(FakeMeRepository(meWithRecords()))
        assertFalse(vm.uiState.value.loading)

        vm.onAction(LinkedSheetAction.StartClicked)
        assertTrue(vm.uiState.value.loading)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `FR-COUPLE-03 AC4 요청 중에는 개수와 기록 영역이 그대로 보인다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        couple.importGate = gate
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)

        vm.onAction(LinkedSheetAction.StartClicked)
        // 가져오기 응답 뒤 GET /me가 importPending을 false로 바꿔도 화면이 깜박이지 않는다
        me.me.value = testMe(type = SpaceType.COUPLE)

        assertTrue(vm.uiState.value.hasRecords)
        assertEquals(counts, vm.uiState.value.counts)
        gate.complete(Unit)
    }

    /** 이전 연결에서 고른 것들(날짜, 가져오지 않기, 날짜 시트)을 남긴 상태를 만든다 */
    private fun dirty(vm: LinkedSheetViewModel) {
        vm.onAction(LinkedSheetAction.ChoiceSelected(ImportChoice.Drop))
        vm.onAction(LinkedSheetAction.DateFieldClicked)
        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(2026, 3, 14)))
        vm.onAction(LinkedSheetAction.DateSaved)
        vm.onAction(LinkedSheetAction.DateFieldClicked)
    }

    private fun assertPristine(vm: LinkedSheetViewModel) {
        val state = vm.uiState.value
        assertEquals(ImportChoice.Keep, state.choice)
        assertNull(state.datePicker)
        assertNull(state.startDate)
    }

    @Test
    fun `FR-COUPLE-03 AC5 D-76 새 커플 공간이 열리면 이전 선택을 버리고 가져오기를 미리 선택한다`() = runTest {
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        dirty(vm)

        me.me.value = meWithRecords().let { it.copy(space = it.space.copy(id = "44444444-4444-4444-4444-444444444444")) }

        assertPristine(vm)
    }

    @Test
    fun `FR-COUPLE-05 AC5 D-103 시작하기가 NOT_COUPLE로 끝나면 선택을 처음 상태로 되돌린다`() = runTest {
        couple.importResult = failure("NOT_COUPLE")
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        vm.onAction(LinkedSheetAction.ChoiceSelected(ImportChoice.Drop))
        vm.onAction(LinkedSheetAction.DateFieldClicked)
        vm.onAction(LinkedSheetAction.DateSelected(LocalDate.of(2026, 3, 14)))
        vm.onAction(LinkedSheetAction.DateSaved)

        vm.onAction(LinkedSheetAction.StartClicked)
        vm.onAction(LinkedSheetAction.DropConfirmed)

        assertPristine(vm)
        assertFalse(vm.uiState.value.requesting)
    }

    @Test
    fun `FR-COUPLE-05 AC3 D-103 앱 초기화로 Room이 비워지면 선택을 처음 상태로 되돌린다`() = runTest {
        val me = FakeMeRepository(meWithRecords())
        val vm = viewModel(me)
        dirty(vm)

        me.me.value = null

        assertPristine(vm)
    }

    @Test
    fun `FR-COUPLE-05 AC3 spaceChanges 구독자는 메인 하나뿐이라 시트가 이벤트를 가로채지 않는다`() = runTest {
        val me = FakeMeRepository(meWithRecords())
        viewModel(me)

        me.changes.trySend(SpaceChange.UNLINKED)

        // 시트가 먼저 받았다면 한 번만 전달되는 이 Channel에서 메인이 받을 이벤트가 없다
        assertEquals(SpaceChange.UNLINKED, withTimeout(1_000) { me.spaceChanges.first() })
    }
}
