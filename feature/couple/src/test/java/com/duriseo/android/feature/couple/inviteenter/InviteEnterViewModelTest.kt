package com.duriseo.android.feature.couple.inviteenter

import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.couple.FakeCoupleRepository
import com.duriseo.android.feature.couple.R
import com.duriseo.android.feature.couple.collectInto
import com.duriseo.android.feature.couple.failure
import com.duriseo.android.feature.couple.offline
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

        assertEquals(R.string.invite_enter_error_expired, vm.uiState.value.errorRes)
        assertTrue(effects.isEmpty())
        assertFalse(vm.uiState.value.requesting)
    }

    @Test
    fun `FR-COUPLE-02 AC2 글자를 고치면 이전 오류 문구가 사라진다`() = runTest {
        couple.joinResult = failure("INVITE_EXPIRED")
        val vm = viewModel()
        vm.type("K7Q29X")
        vm.onAction(InviteEnterAction.ConnectClicked)

        vm.type("K7Q29")

        assertNull(vm.uiState.value.errorRes)
    }

    @Test
    fun `FR-COUPLE-02 AC2 문구가 정해지지 않은 코드 오류는 공통 오류를 보여 주고 화면은 그대로다`() = runTest {
        // INVITE_NOT_FOUND·INVITE_USED·INVITE_SELF·INVITE_UNAVAILABLE의 문구는 v0.9 반영 대기
        listOf("INVITE_NOT_FOUND", "INVITE_USED", "INVITE_SELF", "INVITE_UNAVAILABLE").forEach { code ->
            couple.joinResult = failure(code)
            val vm = viewModel()
            val effects = collectInto(vm.effects)
            vm.type("K7Q29X")

            vm.onAction(InviteEnterAction.ConnectClicked)

            assertEquals(listOf<InviteEnterSideEffect>(InviteEnterSideEffect.ShowMessage(DesignR.string.error_common)), effects)
            assertNull(vm.uiState.value.errorRes)
            assertEquals("K7Q29X", vm.uiState.value.code)
        }
    }

    @Test
    fun `FR-COUPLE-02 AC4 입력 제한 오류는 공통 오류로 두고 화면은 그대로다`() = runTest {
        // 입력 제한 안내 문구와 풀리는 시각 형식은 v0.9 반영 대기
        couple.joinResult = failure("INVITE_RATE_LIMITED", ErrorDetails.RetryAfter(1800))
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.type("K7Q29X")

        vm.onAction(InviteEnterAction.ConnectClicked)

        assertEquals(listOf<InviteEnterSideEffect>(InviteEnterSideEffect.ShowMessage(DesignR.string.error_common)), effects)
        assertEquals("K7Q29X", vm.uiState.value.code)
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
        assertNull(vm.uiState.value.errorRes)
    }
}
