package com.duriseo.android.feature.settings.colors

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.MemberColor
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.settings.FakeCoupleRepository
import com.duriseo.android.feature.settings.GuestId
import com.duriseo.android.feature.settings.apiError
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

/** SCR-SET-03 (FR-CAL-04, D-100, D-101). 고른 색은 초안으로 들고 있다가 [완료]에서 바뀐 색만 한 번에 저장한다 */
@OptIn(ExperimentalCoroutinesApi::class)
class ColorsViewModelTest {

    private val me = FakeMeRepository()
    private val couple = FakeCoupleRepository()

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
        val vm = ColorsViewModel(me, couple, clock)
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
    fun `FR-CAL-04 AC2 D-100 색을 고르는 동안은 서버로 보내지 않는다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()

        vm.onAction(ColorsAction.ColorPicked("pick-rose"))

        assertTrue(couple.colorCalls.isEmpty())
    }

    @Test
    fun `FR-CAL-04 AC2 D-100 완료는 바뀐 색만 한 번에 보내고 성공하면 닫는다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()
        vm.onAction(ColorsAction.ColorPicked("pick-rose"))
        vm.onAction(ColorsAction.TargetSelected(ColorTarget.Together))
        vm.onAction(ColorsAction.ColorPicked("pick-green"))

        vm.effects.test {
            vm.onAction(ColorsAction.DoneClicked)

            assertEquals(ColorsSideEffect.Close, awaitItem())
        }
        // 상대 색은 그대로라 담지 않는다. 내 색은 내 userId, 함께는 together
        assertEquals(
            listOf(ColorChange(members = listOf(MemberColor(GuestId, "pick-rose")), together = "pick-green")),
            couple.colorCalls,
        )
    }

    @Test
    fun `FR-CAL-04 AC2 D-100 상대 색을 바꾸면 상대 userId로 보낸다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()
        vm.onAction(ColorsAction.TargetSelected(ColorTarget.Partner))
        vm.onAction(ColorsAction.ColorPicked("pick-teal"))

        vm.onAction(ColorsAction.DoneClicked)

        assertEquals(
            listOf(ColorChange(members = listOf(MemberColor("33333333-3333-3333-3333-333333333333", "pick-teal")))),
            couple.colorCalls,
        )
    }

    @Test
    fun `FR-CAL-04 AC2 D-100 바뀐 것이 없으면 요청하지 않고 닫는다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(ColorsAction.DoneClicked)

            assertEquals(ColorsSideEffect.Close, awaitItem())
        }
        assertTrue(couple.colorCalls.isEmpty())
    }

    @Test
    fun `FR-CAL-04 AC2 D-100 골랐다가 원래 색으로 돌려놓으면 바뀐 것이 없다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()
        vm.onAction(ColorsAction.ColorPicked("pick-rose"))
        vm.onAction(ColorsAction.ColorPicked("pick-orange"))

        vm.onAction(ColorsAction.DoneClicked)

        assertTrue(couple.colorCalls.isEmpty())
    }

    @Test
    fun `FR-CAL-04 AC2 D-100 저장에 실패하면 공통 오류를 보여 주고 화면과 고른 색을 그대로 둔다`() = runTest {
        me.me.value = coupleMe()
        couple.colorResult = apiError("INTERNAL_ERROR")
        val vm = viewModel()
        vm.onAction(ColorsAction.ColorPicked("pick-rose"))

        vm.effects.test {
            vm.onAction(ColorsAction.DoneClicked)

            assertEquals(ColorsSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            expectNoEvents()
        }
        assertEquals("pick-rose", vm.uiState.value.myColor)
        assertFalse(vm.uiState.value.saving)
    }

    @Test
    fun `NFR-OFF-02 D-100 오프라인이면 인터넷 연결이 필요하다고 안내하고 화면을 그대로 둔다`() = runTest {
        me.me.value = coupleMe()
        couple.colorResult = DataResult.Failure(DataError.Offline)
        val vm = viewModel()
        vm.onAction(ColorsAction.ColorPicked("pick-rose"))

        vm.effects.test {
            vm.onAction(ColorsAction.DoneClicked)

            assertEquals(ColorsSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
        assertEquals("pick-rose", vm.uiState.value.myColor)
    }

    @Test
    fun `FR-COUPLE-05 AC5 D-103 저장이 NOT_COUPLE이면 오류도 닫기도 하지 않는다`() = runTest {
        me.me.value = coupleMe()
        couple.colorResult = apiError("NOT_COUPLE")
        val vm = viewModel()
        vm.onAction(ColorsAction.ColorPicked("pick-rose"))

        vm.effects.test {
            vm.onAction(ColorsAction.DoneClicked)

            expectNoEvents()
        }
    }

    @Test
    fun `FR-CAL-04 AC5 D-100 개인 공간은 내 색만 보내고 함께와 상대는 담지 않는다`() = runTest {
        me.me.value = soloMe()
        val vm = viewModel()
        vm.onAction(ColorsAction.ColorPicked("pick-rose"))
        // 되돌리기는 상대·함께 초안도 채우지만 개인 공간에서는 보내지 않는다
        vm.onAction(ColorsAction.ResetClicked)
        vm.onAction(ColorsAction.ColorPicked("pick-green"))

        vm.onAction(ColorsAction.DoneClicked)

        assertEquals(listOf(ColorChange(members = listOf(MemberColor(GuestId, "pick-green")))), couple.colorCalls)
    }

    @Test
    fun `FR-CAL-04 AC7 D-101 되돌리기는 화면에서 바뀐 것만 저장 대상으로 만든다`() = runTest {
        // 코드를 입력한 사람은 처음 기본값과 나·상대 색이 반대일 수 있다: 나 파랑, 상대 주황이 Room에 있으면 둘 다 바뀐다
        me.me.value = coupleMe(myColor = "pick-blue", partnerColor = "pick-orange", togetherColor = "pick-purple")
        val vm = viewModel()
        vm.onAction(ColorsAction.ResetClicked)

        vm.onAction(ColorsAction.DoneClicked)

        assertEquals(
            listOf(
                ColorChange(
                    members = listOf(
                        MemberColor(GuestId, "pick-orange"),
                        MemberColor("33333333-3333-3333-3333-333333333333", "pick-blue"),
                    ),
                ),
            ),
            couple.colorCalls,
        )
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
        val vm = ColorsViewModel(me, couple, sunday)
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
