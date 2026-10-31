package com.duriseo.android.feature.settings.colors

import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.feature.settings.FakeMeRepository
import com.duriseo.android.feature.settings.memberMe
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** SCR-SET-03 (FR-CAL-04). 색을 서버로 보내는 [완료]는 v0.9 반영 대기라 이 화면은 고른 색을 초안으로만 들고 있다 */
@OptIn(ExperimentalCoroutinesApi::class)
class ColorsViewModelTest {

    private val me = FakeMeRepository()

    /** 2026-10-14 수요일 (한국 시간) */
    private val clock = Clock.fixed(Instant.parse("2026-10-14T03:00:00Z"), ZoneId.of("Asia/Seoul"))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun coupleMe(
        myColor: String = "pick-orange",
        partnerColor: String = "pick-blue",
        togetherColor: String = "pick-purple",
    ): Me = memberMe(partnerNickname = "둘").let {
        it.copy(
            space = it.space.copy(
                myColor = myColor,
                partner = it.space.partner?.copy(color = partnerColor),
                togetherColor = togetherColor,
            ),
        )
    }

    private fun soloMe(myColor: String = "pick-orange"): Me = memberMe().let { it.copy(space = it.space.copy(myColor = myColor)) }

    private fun TestScope.viewModel(): ColorsViewModel {
        val vm = ColorsViewModel(me, clock)
        // stateIn(WhileSubscribed)라서 값을 읽는 동안 구독해 둔다
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    @Test
    fun `FR-CAL-04 AC1 내 정보를 읽기 전에는 아무것도 그리지 않는다`() = runTest {
        val vm = viewModel()

        assertFalse(vm.uiState.value.loaded)
    }

    @Test
    fun `FR-CAL-04 AC1 내 정보의 나 상대 함께 색이 그대로 보인다`() = runTest {
        me.me.value = coupleMe(myColor = "pick-rose", partnerColor = "pick-teal", togetherColor = "pick-ink")
        val vm = viewModel()

        val state = vm.uiState.value
        assertTrue(state.loaded)
        assertTrue(state.isCouple)
        assertEquals("pick-rose", state.myColor)
        assertEquals("pick-teal", state.partnerColor)
        assertEquals("pick-ink", state.togetherColor)
        assertEquals(listOf(ColorTarget.Me, ColorTarget.Partner, ColorTarget.Together), state.targets)
    }

    @Test
    fun `FR-CAL-04 AC2 대상을 고르고 색을 고르면 그 대상의 색만 바뀐다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()

        vm.onAction(ColorsAction.TargetSelected(ColorTarget.Partner))
        vm.onAction(ColorsAction.ColorPicked("pick-teal"))

        val state = vm.uiState.value
        assertEquals(ColorTarget.Partner, state.target)
        assertEquals("pick-teal", state.partnerColor)
        assertEquals("pick-orange", state.myColor)
        assertEquals("pick-purple", state.togetherColor)
    }

    @Test
    fun `FR-CAL-04 AC2 팔레트 12색 밖의 키는 고를 수 없다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()

        vm.onAction(ColorsAction.ColorPicked("#123456"))

        assertEquals("pick-orange", vm.uiState.value.myColor)
    }

    @Test
    fun `FR-CAL-04 AC2 완료는 저장 시점이 정해질 때까지 아무것도 바꾸지 않는다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()
        vm.onAction(ColorsAction.ColorPicked("pick-rose"))
        val before = vm.uiState.value

        vm.onAction(ColorsAction.DoneClicked)

        assertEquals(before, vm.uiState.value)
    }

    @Test
    fun `FR-CAL-04 AC3 기본 색으로 되돌리기는 주황 파랑 보라로 돌린다`() = runTest {
        me.me.value = coupleMe(myColor = "pick-rose", partnerColor = "pick-teal", togetherColor = "pick-ink")
        val vm = viewModel()

        vm.onAction(ColorsAction.ResetClicked)

        val state = vm.uiState.value
        assertEquals("pick-orange", state.myColor)
        assertEquals("pick-blue", state.partnerColor)
        assertEquals("pick-purple", state.togetherColor)
    }

    @Test
    fun `FR-CAL-04 AC4 같은 색을 쓰는 대상이 있으면 경고를 보여 주고 고르는 것은 막지 않는다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()
        assertFalse(vm.uiState.value.hasDuplicate)

        vm.onAction(ColorsAction.TargetSelected(ColorTarget.Partner))
        vm.onAction(ColorsAction.ColorPicked("pick-orange"))

        assertTrue(vm.uiState.value.hasDuplicate)
        assertEquals("pick-orange", vm.uiState.value.partnerColor)
    }

    @Test
    fun `FR-CAL-04 AC4 개인 공간에는 경고가 없다`() = runTest {
        me.me.value = soloMe()
        val vm = viewModel()

        vm.onAction(ColorsAction.ColorPicked("pick-blue"))

        assertFalse(vm.uiState.value.isCouple)
        assertFalse(vm.uiState.value.hasDuplicate)
    }

    @Test
    fun `FR-CAL-04 AC5 개인 공간에서는 바꿀 대상이 나뿐이다`() = runTest {
        me.me.value = soloMe(myColor = "pick-rose")
        val vm = viewModel()

        vm.onAction(ColorsAction.TargetSelected(ColorTarget.Partner))
        vm.onAction(ColorsAction.TargetSelected(ColorTarget.Together))

        val state = vm.uiState.value
        assertEquals(SpaceType.PERSONAL, me.me.value?.space?.type)
        assertEquals(listOf(ColorTarget.Me), state.targets)
        assertEquals(ColorTarget.Me, state.target)
        assertEquals("pick-rose", state.myColor)
    }

    @Test
    fun `FR-CAL-04 AC1 미리보기 한 주는 오늘이 든 일요일부터 토요일까지다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()

        val state = vm.uiState.value
        assertEquals(LocalDate.of(2026, 10, 14), state.today)
        assertEquals(LocalDate.of(2026, 10, 11), state.weekDays.first())
        assertEquals(LocalDate.of(2026, 10, 17), state.weekDays.last())
        assertEquals(7, state.weekDays.size)
    }

    @Test
    fun `FR-CAL-04 AC1 오늘이 일요일이면 그날부터 한 주가 시작한다`() = runTest {
        me.me.value = coupleMe()
        val sunday = Clock.fixed(Instant.parse("2026-10-11T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        val vm = ColorsViewModel(me, sunday)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        assertEquals(LocalDate.of(2026, 10, 11), vm.uiState.value.weekDays.first())
    }

    @Test
    fun `FR-CAL-04 AC1 팔레트에 없는 색 키도 화면이 막히지 않고 그대로 담긴다`() = runTest {
        // 서버가 모르는 키를 보내도 키를 그대로 들고 있고 그리는 쪽이 기본 색으로 대신한다 (unit-3.md 확인 필요 18)
        me.me.value = coupleMe(myColor = "orange", partnerColor = "blue", togetherColor = "purple")
        val vm = viewModel()

        assertTrue(vm.uiState.value.loaded)
        assertEquals("orange", vm.uiState.value.myColor)
    }
}
