package com.duriseo.android.feature.settings.menu

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.settings.FakeAuthRepository
import com.duriseo.android.feature.settings.FakeMeRepository
import com.duriseo.android.feature.settings.apiError
import com.duriseo.android.feature.settings.guestMe
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val me = FakeMeRepository()
    private val auth = FakeAuthRepository(me)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): SettingsViewModel {
        val vm = SettingsViewModel(auth, me)
        // stateIn(WhileSubscribed)라서 값을 읽는 동안 구독해 둔다
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    @Test
    fun `FR-SET-01 전체 메뉴에 들어오면 내 정보를 서버에서 다시 읽는다`() = runTest {
        viewModel()

        assertEquals(1, me.refreshCalls)
    }

    @Test
    fun `FR-SET-01 내 정보 갱신이 실패해도 Room의 값으로 메뉴를 그린다`() = runTest {
        me.me.value = memberMe()
        me.refreshResult = apiError("INTERNAL_ERROR")
        val vm = viewModel()

        vm.effects.test {
            // 조용히 실패한다: 안내를 띄우지 않고 화면은 Room 값 그대로다
            expectNoEvents()
            assertEquals(SettingsAccount.Member(SocialProvider.GOOGLE), vm.uiState.value.account)
        }
    }

    @Test
    fun `FR-AUTH-04 AC1 게스트는 전체 메뉴 맨 위에 안내 배너와 로그인 버튼이 보인다`() = runTest {
        me.me.value = guestMe()
        val vm = viewModel()

        // 배너와 [로그인해서 기록 지키기]는 account가 Guest일 때 그린다 (SettingsScreen)
        assertEquals(SettingsAccount.Guest, vm.uiState.value.account)
    }

    @Test
    fun `FR-AUTH-04 AC1 회원에게는 게스트 배너가 없다`() = runTest {
        me.me.value = memberMe()
        val vm = viewModel()

        assertEquals(SettingsAccount.Member(SocialProvider.GOOGLE), vm.uiState.value.account)
    }

    @Test
    fun `FR-SET-01 AC1 계정 행은 회원이면 로그인 수단을 게스트면 게스트 로그인하기를 보여 준다`() = runTest {
        me.me.value = memberMe(provider = SocialProvider.KAKAO)
        val vm = viewModel()
        assertEquals(SettingsAccount.Member(SocialProvider.KAKAO), vm.uiState.value.account)

        me.me.value = guestMe()
        assertEquals(SettingsAccount.Guest, vm.uiState.value.account)
    }

    @Test
    fun `FR-AUTH-05 AC2 게스트에게는 로그아웃이 보이지 않는다`() = runTest {
        me.me.value = guestMe()
        val vm = viewModel()
        assertFalse(vm.uiState.value.canLogout)

        me.me.value = memberMe()
        assertTrue(vm.uiState.value.canLogout)
    }

    @Test
    fun `FR-AUTH-05 AC1 로그아웃을 확인하면 로그아웃하고 시작 화면으로 간다`() = runTest {
        me.me.value = memberMe()
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(SettingsAction.LogoutClicked)
            assertTrue(vm.uiState.value.logoutDialogVisible)

            vm.onAction(SettingsAction.LogoutConfirmed)

            assertEquals(SettingsSideEffect.NavigateToStart, awaitItem())
            assertEquals(1, auth.logoutCalls)
        }
    }

    @Test
    fun `FR-AUTH-05 AC1 취소하면 로그아웃을 요청하지 않는다`() = runTest {
        me.me.value = memberMe()
        val vm = viewModel()
        vm.onAction(SettingsAction.LogoutClicked)

        vm.onAction(SettingsAction.DialogDismissed)

        assertFalse(vm.uiState.value.logoutDialogVisible)
        assertEquals(0, auth.logoutCalls)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 로그아웃하지 않고 인터넷 연결 안내를 보여 준다`() = runTest {
        me.me.value = memberMe()
        auth.logoutResult = DataResult.Failure(DataError.Offline)
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(SettingsAction.LogoutClicked)
            vm.onAction(SettingsAction.LogoutConfirmed)

            // 안내를 보여 주고 다이얼로그를 닫는다. 로그인 상태 그대로다 (D-86)
            assertEquals(SettingsSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            assertFalse(vm.uiState.value.logoutDialogVisible)
            assertFalse(vm.uiState.value.requesting)
            assertTrue(vm.uiState.value.canLogout)
        }
    }

    @Test
    fun `FR-AUTH-05 AC4 서버 오류면 공통 오류를 보여 주고 로그인 상태를 유지한다`() = runTest {
        me.me.value = memberMe()
        auth.logoutResult = apiError("INTERNAL_ERROR")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(SettingsAction.LogoutClicked)
            vm.onAction(SettingsAction.LogoutConfirmed)

            assertEquals(SettingsSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertFalse(vm.uiState.value.logoutDialogVisible)
        }
    }

    @Test
    fun `FR-AUTH-05 AC4 이미 끊긴 세션이라는 응답이면 안내 없이 세션 종료 흐름에 맡긴다`() = runTest {
        me.me.value = memberMe()
        auth.logoutResult = apiError("SESSION_INVALID")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(SettingsAction.LogoutClicked)
            vm.onAction(SettingsAction.LogoutConfirmed)

            expectNoEvents()
            assertFalse(vm.uiState.value.logoutDialogVisible)
        }
    }

    @Test
    fun `D-88 로그아웃 확인은 다이얼로그가 떠 있을 때 한 번만 요청한다`() = runTest {
        me.me.value = memberMe()
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(SettingsAction.LogoutClicked)
            vm.onAction(SettingsAction.LogoutConfirmed)
            awaitItem()
            // 다이얼로그가 이미 닫혔으니 다시 확인해도 새 요청을 만들지 않는다
            vm.onAction(SettingsAction.LogoutConfirmed)

            assertEquals(1, auth.logoutCalls)
        }
    }
    @Test
    fun `FR-CAL-04 AC1 커플이면 우리 색상 행에 나 상대 함께 색 점 셋이 보인다`() = runTest {
        me.me.value = memberMe(partnerNickname = "둘").let {
            it.copy(space = it.space.copy(myColor = "pick-rose", partner = it.space.partner?.copy(color = "pick-teal"), togetherColor = "pick-ink"))
        }
        val vm = viewModel()

        assertEquals(listOf("pick-rose", "pick-teal", "pick-ink"), vm.uiState.value.colorKeys)
    }

    @Test
    fun `FR-CAL-04 AC5 개인 공간이면 우리 색상 행에 나 색 점 하나만 보인다`() = runTest {
        me.me.value = memberMe().let { it.copy(space = it.space.copy(myColor = "pick-rose")) }
        val vm = viewModel()

        assertEquals(listOf("pick-rose"), vm.uiState.value.colorKeys)
    }
}
