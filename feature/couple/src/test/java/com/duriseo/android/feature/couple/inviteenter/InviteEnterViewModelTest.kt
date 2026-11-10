package com.duriseo.android.feature.couple.inviteenter

import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.couple.FakeCoupleRepository
import com.duriseo.android.feature.couple.collectInto
import com.duriseo.android.feature.couple.failure
import com.duriseo.android.feature.couple.offline
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SCR-COUPLE-03 (FR-COUPLE-02, FR-COUPLE-01 AC4) */
@OptIn(ExperimentalCoroutinesApi::class)
class InviteEnterViewModelTest {

    private val couple = FakeCoupleRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = InviteEnterViewModel(couple)

    private fun InviteEnterViewModel.type(raw: String) = onAction(InviteEnterAction.CodeChanged(raw))

    @Test
    fun `FR-COUPLE-01 AC4 소문자로 입력하면 대문자로 보인다`() = runTest {
        val vm = viewModel()

        vm.type("k7q29x")

        assertEquals("K7Q29X", vm.uiState.value.code)
    }

    @Test
    fun `FR-COUPLE-01 AC4 영문과 숫자가 아닌 글자는 넣지 않고 6자리까지만 받는다`() = runTest {
        val vm = viewModel()

        vm.type("k7-q 29xzz")

        assertEquals("K7Q29X", vm.uiState.value.code)
    }

    @Test
    fun `FR-COUPLE-02 AC1 6자리가 차기 전에는 연결 버튼이 꺼져 있다`() = runTest {
        val vm = viewModel()

        vm.type("K7Q29")
        assertFalse(vm.uiState.value.canConnect)
        vm.onAction(InviteEnterAction.ConnectClicked)

        assertTrue(couple.joinCalls.isEmpty())
    }

    @Test
    fun `FR-COUPLE-02 AC1 6자리가 차면 바로 연결 버튼이 켜진다`() = runTest {
        val vm = viewModel()

        vm.type("K7Q29X")

        assertTrue(vm.uiState.value.canConnect)
    }

    @Test
    fun `FR-COUPLE-01 붙여넣기는 클립보드의 코드를 같은 규칙으로 넣는다`() = runTest {
        val vm = viewModel()

        vm.onAction(InviteEnterAction.Pasted(" k7q29x \n"))

        assertEquals("K7Q29X", vm.uiState.value.code)
    }

    @Test
    fun `FR-COUPLE-01 클립보드가 비어 있으면 붙여넣기는 아무 일도 없다`() = runTest {
        val vm = viewModel()
        vm.type("K7Q")

        vm.onAction(InviteEnterAction.Pasted(null))
        vm.onAction(InviteEnterAction.Pasted("  "))

        assertEquals("K7Q", vm.uiState.value.code)
    }

    @Test
    fun `FR-COUPLE-02 AC1 연결이 되면 캘린더로 간다`() = runTest {
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.type("k7q29x")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(listOf("K7Q29X"), couple.joinCalls)
        assertEquals(listOf<InviteEnterSideEffect>(InviteEnterSideEffect.NavigateToHome), effects)
        assertFalse(vm.uiState.value.requesting)
    }

    @Test
    fun `FR-COUPLE-06 AC2 복구된 연결도 같은 방식으로 캘린더로 간다`() = runTest {
        couple.joinResult = DataResult.Success(CoupleJoinResult("space-1", restored = true, importPending = true))
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(listOf<InviteEnterSideEffect>(InviteEnterSideEffect.NavigateToHome), effects)
    }

    @Test
    fun `FR-COUPLE-02 요청 중에는 연결 버튼이 꺼지고 다시 보내지 않는다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        couple.joinGate = gate
        val vm = viewModel()
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)
        vm.onAction(InviteEnterAction.ConnectClicked)
        vm.type("ABCDEF")

        assertTrue(vm.uiState.value.requesting)
        assertFalse(vm.uiState.value.canConnect)
        assertEquals("K7Q29X", vm.uiState.value.code)
        assertEquals(1, couple.joinCalls.size)
        gate.complete(Unit)
    }

    @Test
    fun `FR-COUPLE-02 AC2 만료된 코드는 새 코드를 받아 달라는 문구다`() = runTest {
        couple.joinResult = failure("INVITE_EXPIRED")
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(InviteEnterError.Expired, vm.uiState.value.error)
        assertTrue(effects.isEmpty())
        assertFalse(vm.uiState.value.requesting)
    }

    @Test
    fun `D-95 코드 오류는 종류마다 다른 오류로 코드 칸 아래에 보인다`() = runTest {
        val expected = mapOf(
            "INVITE_NOT_FOUND" to InviteEnterError.NotFound,
            "INVITE_EXPIRED" to InviteEnterError.Expired,
            "INVITE_USED" to InviteEnterError.Used,
            "INVITE_SELF" to InviteEnterError.Self,
            "INVITE_UNAVAILABLE" to InviteEnterError.Unavailable,
        )
        expected.forEach { (code, error) ->
            couple.joinResult = failure(code)
            val vm = viewModel()
            val effects = collectInto(vm.effects)
            vm.type("K7Q29X")

            vm.onAction(InviteEnterAction.ConnectClicked)

            assertEquals(code, error, vm.uiState.value.error)
            // 코드 오류는 공통 오류 스낵바를 띄우지 않고 입력한 코드도 그대로다
            assertTrue(code, effects.isEmpty())
            assertEquals("K7Q29X", vm.uiState.value.code)
            // 코드 오류가 있어도 코드를 고치면 다시 보낼 수 있다
            assertTrue(code, vm.uiState.value.canConnect)
        }
    }

    @Test
    fun `FR-COUPLE-02 AC3 사용할 수 없는 코드는 새 코드를 받아 달라는 문구다`() = runTest {
        couple.joinResult = failure("INVITE_UNAVAILABLE")
        val vm = viewModel()
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(InviteEnterError.Unavailable, vm.uiState.value.error)
    }

    @Test
    fun `D-95 글자를 고치면 이전 코드 오류 문구가 사라진다`() = runTest {
        couple.joinResult = failure("INVITE_EXPIRED")
        val vm = viewModel()
        vm.type("K7Q29X")
        vm.onAction(InviteEnterAction.ConnectClicked)

        vm.type("K7Q29")

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `D-95 다시 보내기 시작하면 이전 코드 오류 문구가 사라진다`() = runTest {
        couple.joinResult = failure("INVITE_USED")
        val vm = viewModel()
        vm.type("K7Q29X")
        vm.onAction(InviteEnterAction.ConnectClicked)
        val gate = CompletableDeferred<Unit>()
        couple.joinGate = gate

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.requesting)
        gate.complete(Unit)
    }

    @Test
    fun `D-115 코드 입력 화면은 요청 중에는 loading이 true이고 끝나면 false다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        couple.joinGate = gate
        val vm = viewModel()
        vm.type("K7Q29X")
        assertFalse(vm.uiState.value.loading)

        vm.onAction(InviteEnterAction.ConnectClicked)
        assertTrue(vm.uiState.value.loading)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `FR-COUPLE-02 AC4 입력 제한이면 올림한 분 수로 안내하고 연결 버튼이 꺼진다`() = runTest {
        // 721초 = 12분 1초 -> 13분 (올림)
        couple.joinResult = failure("INVITE_RATE_LIMITED", ErrorDetails.RetryAfter(721))
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(InviteEnterError.RateLimited(minutes = 13), vm.uiState.value.error)
        assertFalse(vm.uiState.value.canConnect)
        assertTrue(effects.isEmpty())
        assertEquals("K7Q29X", vm.uiState.value.code)
    }

    @Test
    fun `FR-COUPLE-02 AC4 retryAfterSeconds가 1초여도 안내는 1분이다`() = runTest {
        couple.joinResult = failure("INVITE_RATE_LIMITED", ErrorDetails.RetryAfter(1))
        val vm = viewModel()
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(InviteEnterError.RateLimited(minutes = 1), vm.uiState.value.error)
    }

    @Test
    fun `FR-COUPLE-02 AC4 입력 제한 문구는 코드를 고쳐도 남고 연결은 시도하지 않는다`() = runTest {
        couple.joinResult = failure("INVITE_RATE_LIMITED", ErrorDetails.RetryAfter(600))
        val vm = viewModel()
        vm.type("K7Q29X")
        vm.onAction(InviteEnterAction.ConnectClicked)

        vm.type("ABCDEF")
        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(InviteEnterError.RateLimited(minutes = 10), vm.uiState.value.error)
        assertEquals("ABCDEF", vm.uiState.value.code)
        assertEquals(1, couple.joinCalls.size)
    }

    @Test
    fun `FR-COUPLE-02 AC4 입력 제한 문구의 분 수는 시간이 지나도 줄어들지 않는다`() = runTest {
        couple.joinResult = failure("INVITE_RATE_LIMITED", ErrorDetails.RetryAfter(600))
        val vm = viewModel()
        vm.type("K7Q29X")
        vm.onAction(InviteEnterAction.ConnectClicked)

        advanceTimeBy(599_000)
        runCurrent()

        assertEquals(InviteEnterError.RateLimited(minutes = 10), vm.uiState.value.error)
        assertFalse(vm.uiState.value.canConnect)
    }

    @Test
    fun `FR-COUPLE-02 AC4 retryAfterSeconds가 지나면 문구를 지우고 연결 버튼을 다시 켠다`() = runTest {
        couple.joinResult = failure("INVITE_RATE_LIMITED", ErrorDetails.RetryAfter(600))
        val vm = viewModel()
        vm.type("K7Q29X")
        vm.onAction(InviteEnterAction.ConnectClicked)

        advanceTimeBy(600_000)
        runCurrent()

        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.canConnect)
    }

    @Test
    fun `FR-COUPLE-02 AC4 풀렸을 때 코드가 6자리가 아니면 버튼은 계속 꺼져 있다`() = runTest {
        couple.joinResult = failure("INVITE_RATE_LIMITED", ErrorDetails.RetryAfter(60))
        val vm = viewModel()
        vm.type("K7Q29X")
        vm.onAction(InviteEnterAction.ConnectClicked)
        vm.type("K7Q")

        advanceTimeBy(60_000)
        runCurrent()

        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.canConnect)
    }

    @Test
    fun `FR-COUPLE-02 AC4 화면을 나갔다 들어오면 제한 상태가 남지 않는다`() = runTest {
        couple.joinResult = failure("INVITE_RATE_LIMITED", ErrorDetails.RetryAfter(600))
        val first = viewModel()
        first.type("K7Q29X")
        first.onAction(InviteEnterAction.ConnectClicked)

        val second = viewModel() // 화면을 다시 열면 새 ViewModel

        assertNull(second.uiState.value.error)
        assertEquals("", second.uiState.value.code)
    }

    @Test
    fun `FR-COUPLE-02 AC4 풀리는 시간이 없는 입력 제한 응답은 공통 오류로 본다`() = runTest {
        couple.joinResult = failure("INVITE_RATE_LIMITED")
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(listOf<InviteEnterSideEffect>(InviteEnterSideEffect.ShowMessage(DesignR.string.error_common)), effects)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `D-95 ALREADY_COUPLED와 GUEST_NOT_ALLOWED는 공통 오류를 보여 주고 화면은 그대로다`() = runTest {
        listOf("ALREADY_COUPLED", "GUEST_NOT_ALLOWED", "VALIDATION_FAILED").forEach { code ->
            couple.joinResult = failure(code)
            val vm = viewModel()
            val effects = collectInto(vm.effects)
            vm.type("K7Q29X")

            vm.onAction(InviteEnterAction.ConnectClicked)

            assertEquals(code, listOf<InviteEnterSideEffect>(InviteEnterSideEffect.ShowMessage(DesignR.string.error_common)), effects)
            assertNull(code, vm.uiState.value.error)
            assertEquals("K7Q29X", vm.uiState.value.code)
        }
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 연결 요청이 안내로 끝나고 입력은 그대로다`() = runTest {
        couple.joinResult = offline()
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(listOf<InviteEnterSideEffect>(InviteEnterSideEffect.ShowMessage(DesignR.string.offline_required)), effects)
        assertEquals("K7Q29X", vm.uiState.value.code)
        assertFalse(vm.uiState.value.requesting)
    }

    @Test
    fun `D-88 연결 요청이 서버 오류면 공통 오류를 보여 주고 화면은 그대로다`() = runTest {
        couple.joinResult = failure("INTERNAL_ERROR")
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(listOf<InviteEnterSideEffect>(InviteEnterSideEffect.ShowMessage(DesignR.string.error_common)), effects)
        assertEquals("K7Q29X", vm.uiState.value.code)
        assertNull(vm.uiState.value.error)
    }
}
