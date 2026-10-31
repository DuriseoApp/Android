package com.duriseo.android.feature.couple.connect

import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.feature.couple.FakeAuthRepository
import com.duriseo.android.feature.couple.FakeMeRepository
import com.duriseo.android.feature.couple.collectInto
import com.duriseo.android.feature.couple.failure
import com.duriseo.android.feature.couple.offline
import com.duriseo.android.feature.couple.testMe
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SCR-COUPLE-01 (FR-COUPLE-01, FR-AUTH-04 AC2, FR-AUTH-05 AC3) */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectViewModelTest {

    private val auth = FakeAuthRepository()
    private val me = FakeMeRepository(testMe())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): ConnectViewModel {
        val vm = ConnectViewModel(auth, me)
        // stateIn(WhileSubscribed)라서 값을 읽는 동안 구독해 둔다
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    @Test
    fun `FR-COUPLE-01 회원에게는 만들기와 입력하기와 혼자 시작하기가 보인다`() = runTest {
        val vm = viewModel()

        assertFalse(vm.uiState.value.isGuest)
    }

    @Test
    fun `FR-COUPLE-01 초대코드 만들기를 누르면 코드 만들기 화면으로 간다`() = runTest {
        val vm = viewModel()
        val effects = collectInto(vm.effects)

        vm.onAction(ConnectAction.MakeCodeClicked)

        assertEquals(listOf<ConnectSideEffect>(ConnectSideEffect.NavigateToInviteCreate), effects)
    }

    @Test
    fun `FR-COUPLE-01 초대코드 입력하기를 누르면 코드 입력 화면으로 간다`() = runTest {
        val vm = viewModel()
        val effects = collectInto(vm.effects)

        vm.onAction(ConnectAction.EnterCodeClicked)

        assertEquals(listOf<ConnectSideEffect>(ConnectSideEffect.NavigateToInviteEnter), effects)
    }

    @Test
    fun `FR-COUPLE-01 혼자 시작하기를 누르면 캘린더로 간다`() = runTest {
        val vm = viewModel()
        val effects = collectInto(vm.effects)

        vm.onAction(ConnectAction.SoloClicked)

        assertEquals(listOf<ConnectSideEffect>(ConnectSideEffect.NavigateToHome), effects)
    }

    @Test
    fun `FR-AUTH-04 AC2 게스트에게는 카드 대신 로그인 안내가 보인다`() = runTest {
        me.me.value = testMe(accountType = AccountType.GUEST)
        val vm = viewModel()

        assertTrue(vm.uiState.value.isGuest)
    }

    @Test
    fun `FR-AUTH-04 AC2 게스트가 혼자 계속 쓰기를 누르면 캘린더로 간다`() = runTest {
        me.me.value = testMe(accountType = AccountType.GUEST)
        val vm = viewModel()
        val effects = collectInto(vm.effects)

        vm.onAction(ConnectAction.SoloClicked)

        assertEquals(listOf<ConnectSideEffect>(ConnectSideEffect.NavigateToHome), effects)
    }

    @Test
    fun `FR-AUTH-04 AC2 게스트가 로그인하고 연결하기를 누르면 계정 화면으로 간다`() = runTest {
        me.me.value = testMe(accountType = AccountType.GUEST)
        val vm = viewModel()
        val effects = collectInto(vm.effects)

        vm.onAction(ConnectAction.GuestLoginClicked)

        assertEquals(listOf<ConnectSideEffect>(ConnectSideEffect.NavigateToAccount), effects)
    }

    @Test
    fun `FR-AUTH-05 AC3 다른 계정으로 로그인을 누르면 로그아웃 확인을 보여 준다`() = runTest {
        val vm = viewModel()

        vm.onAction(ConnectAction.OtherAccountClicked)

        assertTrue(vm.uiState.value.logoutDialogVisible)
        assertEquals(0, auth.logoutCalls)
    }

    @Test
    fun `FR-AUTH-05 AC3 로그아웃 확인에서 취소하면 아무것도 보내지 않고 닫는다`() = runTest {
        val vm = viewModel()
        vm.onAction(ConnectAction.OtherAccountClicked)

        vm.onAction(ConnectAction.LogoutDismissed)

        assertFalse(vm.uiState.value.logoutDialogVisible)
        assertEquals(0, auth.logoutCalls)
    }

    @Test
    fun `FR-AUTH-05 AC3 확인하면 로그아웃하고 시작 화면으로 간다`() = runTest {
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(ConnectAction.OtherAccountClicked)

        vm.onAction(ConnectAction.LogoutConfirmed)

        assertEquals(1, auth.logoutCalls)
        assertEquals(listOf<ConnectSideEffect>(ConnectSideEffect.NavigateToStart), effects)
        assertFalse(vm.uiState.value.logoutDialogVisible)
    }

    @Test
    fun `FR-AUTH-05 AC3 로그아웃 요청 중에는 확인 버튼이 꺼지고 다시 누를 수 없다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        auth.logoutGate = gate
        val vm = viewModel()
        vm.onAction(ConnectAction.OtherAccountClicked)

        vm.onAction(ConnectAction.LogoutConfirmed)
        vm.onAction(ConnectAction.LogoutConfirmed)
        vm.onAction(ConnectAction.LogoutDismissed)

        assertTrue(vm.uiState.value.requesting)
        assertTrue(vm.uiState.value.logoutDialogVisible)
        assertEquals(1, auth.logoutCalls)
        gate.complete(Unit)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 다른 계정으로 로그인의 로그아웃 실패를 인터넷 연결 안내로 보여 준다`() = runTest {
        auth.logoutResult = offline()
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(ConnectAction.OtherAccountClicked)

        vm.onAction(ConnectAction.LogoutConfirmed)

        assertEquals(listOf<ConnectSideEffect>(ConnectSideEffect.ShowMessage(DesignR.string.offline_required)), effects)
        assertFalse(vm.uiState.value.logoutDialogVisible)
        assertFalse(vm.uiState.value.requesting)
    }

    @Test
    fun `D-88 로그아웃이 서버 오류면 공통 오류를 보여 주고 화면은 그대로다`() = runTest {
        auth.logoutResult = failure("INTERNAL_ERROR")
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(ConnectAction.OtherAccountClicked)

        vm.onAction(ConnectAction.LogoutConfirmed)

        assertEquals(listOf<ConnectSideEffect>(ConnectSideEffect.ShowMessage(DesignR.string.error_common)), effects)
        assertFalse(vm.uiState.value.logoutDialogVisible)
        assertFalse(vm.uiState.value.requesting)
    }

    @Test
    fun `FR-AUTH-05 AC3 세션이 이미 끊긴 응답이면 안내 없이 머문다`() = runTest {
        auth.logoutResult = failure("SESSION_INVALID")
        val vm = viewModel()
        val effects = collectInto(vm.effects)
        vm.onAction(ConnectAction.OtherAccountClicked)

        vm.onAction(ConnectAction.LogoutConfirmed)

        assertTrue(effects.isEmpty())
    }
}
