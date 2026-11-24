package com.duriseo.android.feature.calendar.tutorial

import com.duriseo.android.feature.calendar.FakeCalendarTutorialRepository
import com.duriseo.android.feature.calendar.FakeMeRepository
import com.duriseo.android.feature.calendar.coupleMe
import com.duriseo.android.feature.calendar.soloMe
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** SCR-CAL-01 캘린더 튜토리얼의 표시 규칙과 단계 진행 (FR-CAL-06, FR-SET-02 AC5) */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarTutorialViewModelTest {

    private val repository = FakeCalendarTutorialRepository()
    private val me = FakeMeRepository(coupleMe())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(repo: FakeCalendarTutorialRepository = repository): CalendarTutorialViewModel {
        val vm = CalendarTutorialViewModel(repo, me)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    private fun CalendarTutorialViewModel.environment(
        onCalendar: Boolean = true,
        targetsReady: Boolean = true,
        blocked: Boolean = false,
    ) = onAction(CalendarTutorialAction.EnvironmentChanged(onCalendar, targetsReady, blocked))

    // --- 표시 규칙 (AC1·AC4) ---

    @Test
    fun `FR-CAL-06 AC1 이 기기에서 처음 캘린더에 들어오면 1단계부터 뜬다`() = runTest {
        val vm = viewModel()
        assertNull(vm.uiState.value.step)

        vm.environment()

        assertEquals(CalendarTutorialStep.Calendar, vm.uiState.value.step)
        assertTrue(vm.uiState.value.visible)
    }

    @Test
    fun `FR-CAL-06 AC4 처음 뜰 때 본 것으로 저장한다`() = runTest {
        val vm = viewModel()

        // 조건이 갖춰지기 전에는 저장하지 않는다
        vm.environment(targetsReady = false)
        assertFalse(repository.shown)

        vm.environment()

        assertTrue(repository.shown)
    }

    @Test
    fun `FR-CAL-06 AC4 이미 본 기기에서는 캘린더에 들어와도 뜨지 않는다`() = runTest {
        val vm = viewModel(FakeCalendarTutorialRepository(shown = true))

        vm.environment()

        assertNull(vm.uiState.value.step)
    }

    @Test
    fun `FR-CAL-06 AC4 한 번 보고 닫은 뒤 다른 화면에 갔다 돌아와도 다시 뜨지 않는다`() = runTest {
        val vm = viewModel()
        vm.environment()
        vm.onAction(CalendarTutorialAction.SkipClicked)

        vm.environment(onCalendar = false)
        vm.environment(onCalendar = true)

        assertNull(vm.uiState.value.step)
        assertEquals(1, repository.markCalls)
    }

    @Test
    fun `FR-CAL-06 AC4 로그아웃 뒤 같은 기기로 다시 로그인해도(새 ViewModel도) 저장된 기록이 있으면 뜨지 않는다`() = runTest {
        // 기록은 기기 단위라 로그아웃·탈퇴·앱 초기화가 지우지 않는다 (저장소 테스트 CalendarTutorialRepositoryTest)
        val first = viewModel()
        first.environment()
        assertEquals(CalendarTutorialStep.Calendar, first.uiState.value.step)

        val second = viewModel()
        second.environment()

        assertNull(second.uiState.value.step)
    }

    @Test
    fun `FR-CAL-06 AC1 대상 위치를 다 재기 전에는 뜨지 않고 다 재면 뜬다`() = runTest {
        val vm = viewModel()

        vm.environment(targetsReady = false)
        assertNull(vm.uiState.value.step)

        vm.environment(targetsReady = true)
        assertEquals(CalendarTutorialStep.Calendar, vm.uiState.value.step)
    }

    @Test
    fun `FR-CAL-06 AC1 연결 완료 시트 같은 다른 시트가 떠 있으면 닫힌 뒤에 뜨고 그때 저장한다`() = runTest {
        val vm = viewModel()

        vm.environment(blocked = true)
        assertNull(vm.uiState.value.step)
        assertFalse(repository.shown)

        vm.environment(blocked = false)

        assertEquals(CalendarTutorialStep.Calendar, vm.uiState.value.step)
        assertTrue(repository.shown)
    }

    @Test
    fun `FR-CAL-06 AC1 튜토리얼이 뜬 뒤 다른 시트가 열리면 가렸다가 닫히면 같은 단계로 이어진다`() = runTest {
        val vm = viewModel()
        vm.environment()
        vm.onAction(CalendarTutorialAction.NextClicked)

        vm.environment(blocked = true)
        assertFalse(vm.uiState.value.visible)

        vm.environment(blocked = false)
        assertTrue(vm.uiState.value.visible)
        assertEquals(CalendarTutorialStep.Fab, vm.uiState.value.step)
    }

    @Test
    fun `FR-CAL-06 AC1 캘린더 탭이 아니면(로그인 화면 포함) 뜨지 않고 보이던 것도 닫는다`() = runTest {
        val vm = viewModel()
        vm.environment(onCalendar = false)
        assertNull(vm.uiState.value.step)
        assertFalse(repository.shown)

        vm.environment()
        assertEquals(CalendarTutorialStep.Calendar, vm.uiState.value.step)

        vm.environment(onCalendar = false)
        assertNull(vm.uiState.value.step)
    }

    @Test
    fun `FR-CAL-06 AC4 본 기록을 저장하지 못해도 튜토리얼은 보여 준다`() = runTest {
        val repo = FakeCalendarTutorialRepository().apply { markFailure = IOException("디스크 오류") }
        val vm = viewModel(repo)

        vm.environment()

        assertEquals(CalendarTutorialStep.Calendar, vm.uiState.value.step)
    }

    // --- 단계 진행 (AC2·AC3) ---

    @Test
    fun `FR-CAL-06 AC3 화면 아무 곳이나 누르면 1단계에서 5단계까지 차례로 넘어간다`() = runTest {
        val vm = viewModel()
        vm.environment()

        val seen = mutableListOf(vm.uiState.value.step)
        repeat(4) {
            vm.onAction(CalendarTutorialAction.NextClicked)
            seen += vm.uiState.value.step
        }

        assertEquals(
            listOf(
                CalendarTutorialStep.Calendar,
                CalendarTutorialStep.Fab,
                CalendarTutorialStep.Anniversary,
                CalendarTutorialStep.MemoTab,
                CalendarTutorialStep.MenuButton,
            ),
            seen,
        )
        assertEquals(listOf(1, 2, 3, 4, 5), seen.map { it!!.number })
        assertEquals(5, CalendarTutorialStep.count)
    }

    @Test
    fun `FR-CAL-06 AC3 5단계에서 누르면 닫힌다`() = runTest {
        val vm = viewModel()
        vm.environment()
        repeat(4) { vm.onAction(CalendarTutorialAction.NextClicked) }
        assertEquals(CalendarTutorialStep.MenuButton, vm.uiState.value.step)

        vm.onAction(CalendarTutorialAction.NextClicked)

        assertNull(vm.uiState.value.step)
        assertFalse(vm.uiState.value.visible)
    }

    @Test
    fun `FR-CAL-06 AC3 건너뛰기와 뒤로 가기는 중간 단계에서도 바로 닫는다`() = runTest {
        val vm = viewModel()
        vm.environment()
        vm.onAction(CalendarTutorialAction.NextClicked)
        vm.onAction(CalendarTutorialAction.NextClicked)

        vm.onAction(CalendarTutorialAction.SkipClicked)

        assertNull(vm.uiState.value.step)
    }

    @Test
    fun `FR-CAL-06 AC3 닫은 뒤에는 조건이 다시 바뀌어도 되살아나지 않는다`() = runTest {
        val vm = viewModel()
        vm.environment()
        vm.onAction(CalendarTutorialAction.SkipClicked)

        vm.environment(targetsReady = false)
        vm.environment(targetsReady = true)
        vm.environment(blocked = true)
        vm.environment(blocked = false)

        assertNull(vm.uiState.value.step)
    }

    @Test
    fun `FR-CAL-06 AC2 3단계 문구는 커플과 개인 공간이 다르다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()
        assertFalse(vm.uiState.value.solo)

        me.me.value = soloMe()
        assertTrue(vm.uiState.value.solo)
        assertTrue(
            CalendarTutorialStep.Anniversary.messageRes(solo = true) !=
                CalendarTutorialStep.Anniversary.messageRes(solo = false),
        )
        // 3단계 말고는 같은 문구다
        assertEquals(
            CalendarTutorialStep.Fab.messageRes(solo = true),
            CalendarTutorialStep.Fab.messageRes(solo = false),
        )
    }

    // --- 다시 보기 (FR-SET-02 AC5, D-169) ---

    @Test
    fun `FR-SET-02 AC5 이미 본 기기에서도 다시 보기를 요청하면 캘린더에서 1단계부터 뜬다`() = runTest {
        val repo = FakeCalendarTutorialRepository(shown = true)
        val vm = viewModel(repo)
        vm.environment()
        assertNull(vm.uiState.value.step)

        repo.requestReplay()

        assertEquals(CalendarTutorialStep.Calendar, vm.uiState.value.step)
        // 요청은 가져간 것으로 처리된다 (닫은 뒤 되살아나지 않는다)
        assertFalse(repo.replayRequested.value)
        vm.onAction(CalendarTutorialAction.SkipClicked)
        assertNull(vm.uiState.value.step)
    }

    @Test
    fun `FR-SET-02 AC5 전체 메뉴에 있는 동안 받은 요청은 캘린더로 돌아오면 1단계부터 뜬다`() = runTest {
        val repo = FakeCalendarTutorialRepository(shown = true)
        val vm = viewModel(repo)
        // 전체 메뉴(캘린더 탭이 아님)에서 눌렀다
        vm.environment(onCalendar = false)
        repo.requestReplay()
        assertNull(vm.uiState.value.step)
        assertTrue(repo.replayRequested.value)

        // 메뉴를 닫고 캘린더로 돌아와 위치를 다 쟀다
        vm.environment(onCalendar = true, targetsReady = false)
        assertNull(vm.uiState.value.step)
        vm.environment(onCalendar = true, targetsReady = true)

        assertEquals(CalendarTutorialStep.Calendar, vm.uiState.value.step)
    }

    @Test
    fun `FR-SET-02 AC5 다시 보기는 진행 중이던 단계와 상관없이 1단계부터다`() = runTest {
        val repo = FakeCalendarTutorialRepository(shown = true)
        val vm = viewModel(repo)
        vm.environment()
        repo.requestReplay()
        repeat(2) { vm.onAction(CalendarTutorialAction.NextClicked) }
        assertEquals(CalendarTutorialStep.Anniversary, vm.uiState.value.step)
        vm.onAction(CalendarTutorialAction.SkipClicked)

        repo.requestReplay()

        assertEquals(CalendarTutorialStep.Calendar, vm.uiState.value.step)
    }

    @Test
    fun `FR-CAL-06 AC5 서버를 쓰지 않아 내 정보가 없어도(오프라인 첫 실행) 튜토리얼은 같다`() = runTest {
        me.me.value = null
        val vm = viewModel()

        vm.environment()

        assertEquals(CalendarTutorialStep.Calendar, vm.uiState.value.step)
    }
}
