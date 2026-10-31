package com.duriseo.android.feature.couple.invitecreate

import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.couple.FakeCoupleRepository
import com.duriseo.android.feature.couple.FakeMeRepository
import com.duriseo.android.feature.couple.R
import com.duriseo.android.feature.couple.SchedulerClock
import com.duriseo.android.feature.couple.collectInto
import com.duriseo.android.feature.couple.failure
import com.duriseo.android.feature.couple.offline
import com.duriseo.android.feature.couple.testMe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * SCR-COUPLE-02 (FR-COUPLE-01, FR-COUPLE-02 AC5). 가상 시간과 같이 흐르는 시계를 쓴다.
 * 효과는 본문을 멈추지 않는 수집 목록으로 확인한다 (가상 시간이 저절로 흐르지 않게).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InviteCreateViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val start = Instant.parse("2026-10-31T03:00:00Z")
    private val clock = SchedulerClock(scheduler, start)
    private val couple = FakeCoupleRepository()
    private val me = FakeMeRepository(testMe())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = InviteCreateViewModel(couple, me, clock)

    /** 지금 만든 코드: 10분 뒤 만료, 1분 뒤 재요청 가능 (API.md 3.2 `InviteCode`) */
    private fun invite(code: String = "K7Q29X"): InviteCode {
        val now = clock.instant()
        return InviteCode(code, expiresAt = now.plusSeconds(600), reissueAvailableAt = now.plusSeconds(60))
    }

    @Test
    fun `FR-COUPLE-01 AC1 들어오면 유효한 코드가 있으면 그대로 보여 준다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()

        vm.onAction(InviteCreateAction.Shown)

        val state = vm.uiState.value
        assertEquals("K7Q29X", state.code)
        assertEquals(600, state.remainingSeconds)
        assertEquals(0, couple.issueCalls)
        assertFalse(state.renewedNotice)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC1 유효한 코드가 없으면 새 코드를 만든다`() = runTest {
        couple.currentResults += DataResult.Success(null)
        couple.issueResults += DataResult.Success(invite("P4WM7R"))
        val vm = viewModel()

        vm.onAction(InviteCreateAction.Shown)

        assertEquals("P4WM7R", vm.uiState.value.code)
        assertEquals(1, couple.issueCalls)
        assertFalse(vm.uiState.value.renewedNotice)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC1 공유하기는 공유 시트에 코드를 넘긴다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(InviteCreateAction.Shown)

        vm.onAction(InviteCreateAction.ShareClicked)

        assertEquals(listOf<InviteCreateSideEffect>(InviteCreateSideEffect.ShareCode("K7Q29X")), effects)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC1 코드 복사는 클립보드로 코드를 넘기고 복사됨으로 바뀐다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(InviteCreateAction.Shown)

        vm.onAction(InviteCreateAction.CopyClicked)

        assertEquals(listOf<InviteCreateSideEffect>(InviteCreateSideEffect.CopyCode("K7Q29X")), effects)
        assertTrue(vm.uiState.value.copied)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC3 재요청이 되면 새 코드를 보여 준다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        vm.onAction(InviteCreateAction.Shown)
        advanceTimeBy(61_000)
        runCurrent()
        couple.issueResults += DataResult.Success(invite("P4WM7R"))

        vm.onAction(InviteCreateAction.ReissueClicked)

        assertEquals("P4WM7R", vm.uiState.value.code)
        assertEquals(1, couple.issueCalls)
        assertFalse(vm.uiState.value.copied)
        assertFalse(vm.uiState.value.requesting)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC3 1분이 지나기 전에는 재요청 버튼이 꺼지고 남은 초를 보여 준다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        vm.onAction(InviteCreateAction.Shown)
        advanceTimeBy(42_000)
        runCurrent()

        val state = vm.uiState.value
        assertEquals(18, state.reissueWaitSeconds)
        assertFalse(state.canReissue)

        vm.onAction(InviteCreateAction.ReissueClicked)
        assertEquals(0, couple.issueCalls)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC3 1분이 지나면 재요청 버튼이 켜진다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        vm.onAction(InviteCreateAction.Shown)

        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(0, vm.uiState.value.reissueWaitSeconds)
        assertTrue(vm.uiState.value.canReissue)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC3 429 INVITE_REISSUE_TOO_SOON이면 retryAfterSeconds만큼 기다린다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(InviteCreateAction.Shown)
        advanceTimeBy(61_000)
        runCurrent()
        couple.issueResults += failure("INVITE_REISSUE_TOO_SOON", ErrorDetails.RetryAfter(23))

        vm.onAction(InviteCreateAction.ReissueClicked)

        assertEquals(23, vm.uiState.value.reissueWaitSeconds)
        assertFalse(vm.uiState.value.canReissue)
        assertEquals("K7Q29X", vm.uiState.value.code)
        assertTrue(effects.isEmpty())
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC5 남은 시간이 0이 되면 새 코드와 안내를 보여 준다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        couple.currentResults += DataResult.Success(null)
        val vm = viewModel()
        vm.onAction(InviteCreateAction.Shown)
        advanceTimeBy(599_000)
        runCurrent()
        assertEquals("K7Q29X", vm.uiState.value.code)
        assertFalse(vm.uiState.value.renewedNotice)
        couple.issueResults += DataResult.Success(invite("P4WM7R"))

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals("P4WM7R", vm.uiState.value.code)
        assertTrue(vm.uiState.value.renewedNotice)
        assertEquals(1, couple.issueCalls)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC5 다시 들어왔을 때 만료돼 있어도 새 코드를 보여 준다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        couple.currentResults += DataResult.Success(null)
        val vm = viewModel()
        vm.onAction(InviteCreateAction.Shown)
        vm.onAction(InviteCreateAction.Hidden)
        advanceTimeBy(700_000)
        runCurrent()
        couple.issueResults += DataResult.Success(invite("P4WM7R"))

        vm.onAction(InviteCreateAction.Shown)

        assertEquals("P4WM7R", vm.uiState.value.code)
        assertTrue(vm.uiState.value.renewedNotice)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-01 AC5 백그라운드에서는 새 코드를 만들지 않는다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        vm.onAction(InviteCreateAction.Shown)
        vm.onAction(InviteCreateAction.Hidden)

        advanceTimeBy(700_000)
        runCurrent()

        assertEquals(1, couple.currentCalls)
        assertEquals(0, couple.issueCalls)
    }

    @Test
    fun `FR-COUPLE-01 AC5 서버는 아직 유효한 코드라고 하면 기기 시계가 만료로 봐도 그 코드를 유지한다`() = runTest {
        val stillValid = invite("K7Q29X")
        couple.currentResults += DataResult.Success(stillValid)
        couple.currentResults += DataResult.Success(stillValid)
        val vm = viewModel()
        vm.onAction(InviteCreateAction.Shown)

        // 기기 시계로는 만료 시각이 지났지만 서버의 current는 같은 코드를 준다
        advanceTimeBy(601_000)
        runCurrent()

        assertEquals("K7Q29X", vm.uiState.value.code)
        assertEquals(0, couple.issueCalls)
        assertFalse(vm.uiState.value.renewedNotice)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-02 AC5 5초마다 내 정보를 확인한다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        vm.onAction(InviteCreateAction.Shown)
        assertEquals(0, me.refreshCalls)

        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(1, me.refreshCalls)

        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(2, me.refreshCalls)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-02 AC5 화면이 보이지 않는 동안에는 확인하지 않는다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        vm.onAction(InviteCreateAction.Shown)
        vm.onAction(InviteCreateAction.Hidden)

        advanceTimeBy(30_000)
        runCurrent()

        assertEquals(0, me.refreshCalls)
    }

    @Test
    fun `FR-COUPLE-02 AC5 공간이 커플이 되면 캘린더로 간다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(InviteCreateAction.Shown)

        me.me.value = testMe(SpaceType.COUPLE)

        assertEquals(listOf<InviteCreateSideEffect>(InviteCreateSideEffect.NavigateToHome), effects)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-02 AC5 5초 확인에서 연결이 확인되면 캘린더로 간다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        me.onRefresh = { it.me.value = testMe(SpaceType.COUPLE) }
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(InviteCreateAction.Shown)

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(listOf<InviteCreateSideEffect>(InviteCreateSideEffect.NavigateToHome), effects)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-02 AC5 연결 확인하기를 누르면 바로 확인하고 아직이면 안내한다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(InviteCreateAction.Shown)

        vm.onAction(InviteCreateAction.CheckClicked)

        assertEquals(1, me.refreshCalls)
        assertEquals(
            listOf<InviteCreateSideEffect>(InviteCreateSideEffect.ShowMessage(R.string.invite_create_not_yet)),
            effects,
        )
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `FR-COUPLE-02 AC5 연결 확인하기로 연결을 확인하면 안내 없이 캘린더로 간다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        me.onRefresh = { it.me.value = testMe(SpaceType.COUPLE) }
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(InviteCreateAction.Shown)

        vm.onAction(InviteCreateAction.CheckClicked)

        assertEquals(listOf<InviteCreateSideEffect>(InviteCreateSideEffect.NavigateToHome), effects)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 코드를 받지 못하고 인터넷 연결 안내를 한 번만 보여 준다`() = runTest {
        couple.defaultCurrent = offline()
        val vm = viewModel()
        val effects = collectInto(vm.effects)

        vm.onAction(InviteCreateAction.Shown)
        advanceTimeBy(12_000)
        runCurrent()

        assertEquals(null, vm.uiState.value.code)
        assertEquals(
            listOf<InviteCreateSideEffect>(InviteCreateSideEffect.ShowMessage(DesignR.string.offline_required)),
            effects,
        )
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 코드 새로 만들기가 안내로 끝나고 기존 코드는 그대로다`() = runTest {
        couple.currentResults += DataResult.Success(invite("K7Q29X"))
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(InviteCreateAction.Shown)
        advanceTimeBy(61_000)
        runCurrent()
        couple.issueResults += offline()

        vm.onAction(InviteCreateAction.ReissueClicked)

        assertEquals("K7Q29X", vm.uiState.value.code)
        assertEquals(
            listOf<InviteCreateSideEffect>(InviteCreateSideEffect.ShowMessage(DesignR.string.offline_required)),
            effects,
        )
        assertFalse(vm.uiState.value.requesting)
        vm.onAction(InviteCreateAction.Hidden)
    }

    @Test
    fun `D-88 코드를 만드는 요청이 서버 오류면 공통 오류를 보여 준다`() = runTest {
        couple.currentResults += DataResult.Success(null)
        couple.issueResults += failure("INTERNAL_ERROR")
        val vm = viewModel()
        val effects = collectInto(vm.effects)

        vm.onAction(InviteCreateAction.Shown)

        assertEquals(
            listOf<InviteCreateSideEffect>(InviteCreateSideEffect.ShowMessage(DesignR.string.error_common)),
            effects,
        )
        vm.onAction(InviteCreateAction.Hidden)
    }
}
