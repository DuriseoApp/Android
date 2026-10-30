package com.duriseo.android.feature.auth.start

import app.cash.turbine.test
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.auth.R
import java.io.File
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {

    private val auth = FakeAuthRepository()
    private val me = FakeMeRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = StartViewModel(auth, me, UnusedSocialTokenProvider)

    private fun googleToken(token: String = "google-id-token") =
        StartAction.SocialTokenReceived(SocialProvider.GOOGLE, SocialTokenResult.Success(SocialProvider.GOOGLE, token))

    @Test
    fun `FR-AUTH-01 AC1 게스트로 시작하면 캘린더로 간다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(StartAction.GuestClicked)

            assertEquals(StartSideEffect.LoggedIn(AfterLogin.Home), awaitItem())
            assertEquals(1, auth.guestCalls)
            assertFalse(vm.uiState.value.requesting)
        }
    }

    @Test
    fun `FR-AUTH-02 AC1 isNewUser가 true면 커플 연결로 갈 곳을 고른다`() = runTest {
        auth.signInResult = DataResult.Success(LoginResult(isNewUser = true))
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(googleToken())

            assertEquals(StartSideEffect.LoggedIn(AfterLogin.CoupleConnect), awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-02 AC2 기존 회원은 로그인 뒤 내 정보를 보고 캘린더로 간다`() = runTest {
        me.me.value = testMe(importPending = false)
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(googleToken())

            assertEquals(StartSideEffect.LoggedIn(AfterLogin.Home), awaitItem())
            assertEquals(listOf(SocialProvider.GOOGLE to "google-id-token"), auth.signInCalls)
        }
    }

    @Test
    fun `FR-AUTH-02 AC2 가져올 기록이 남은 기존 회원은 가져오기로 갈 곳을 고른다`() = runTest {
        me.me.value = testMe(importPending = true)
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(googleToken())

            assertEquals(StartSideEffect.LoggedIn(AfterLogin.ImportPending), awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-02 카카오 버튼은 카카오 로그인 창을 열도록 요청한다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(StartAction.SocialClicked(SocialProvider.KAKAO))

            assertEquals(StartSideEffect.RequestSocialToken(SocialProvider.KAKAO), awaitItem())
            assertTrue(vm.uiState.value.requesting)
        }
    }

    @Test
    fun `FR-AUTH-02 소셜 로그인을 취소하면 요청하지 않고 화면에 머문다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(StartAction.SocialClicked(SocialProvider.GOOGLE))
            assertEquals(StartSideEffect.RequestSocialToken(SocialProvider.GOOGLE), awaitItem())

            vm.onAction(StartAction.SocialTokenReceived(SocialProvider.GOOGLE, SocialTokenResult.Cancelled))

            // 안내도 이동도 없다 (D-88)
            expectNoEvents()
            assertTrue(auth.signInCalls.isEmpty())
            assertFalse(vm.uiState.value.requesting)
        }
    }

    @Test
    fun `D-88 소셜 SDK가 실패하면 공통 오류 안내를 보여 주고 화면에 머문다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(StartAction.SocialClicked(SocialProvider.GOOGLE))
            awaitItem() // RequestSocialToken

            vm.onAction(StartAction.SocialTokenReceived(SocialProvider.GOOGLE, SocialTokenResult.Failed()))

            assertEquals(StartSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertTrue(auth.signInCalls.isEmpty())
            assertFalse(vm.uiState.value.requesting)
        }
    }

    @Test
    fun `D-88 SOCIAL_TOKEN_INVALID는 공통 오류 안내를 보여 주고 화면에 머문다`() = runTest {
        auth.signInResult = apiError("SOCIAL_TOKEN_INVALID")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(googleToken())

            assertEquals(StartSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertFalse(vm.uiState.value.requesting)
        }
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 게스트 시작이 실패하고 인터넷 연결 안내를 보여 준다`() = runTest {
        auth.guestResult = DataResult.Failure(DataError.Offline)
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(StartAction.GuestClicked)

            assertEquals(StartSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            assertFalse(vm.uiState.value.requesting)
        }
    }

    @Test
    fun `D-88 요청 중에는 시작 버튼을 다시 눌러도 요청하지 않는다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        auth.signInGate = gate
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(googleToken())
            assertTrue(vm.uiState.value.requesting)

            // 같은 화면의 다른 시작 버튼도 눌리지 않는다
            vm.onAction(StartAction.GuestClicked)
            vm.onAction(StartAction.SocialClicked(SocialProvider.KAKAO))
            assertEquals(0, auth.guestCalls)
            expectNoEvents()

            gate.complete(Unit)
            assertEquals(StartSideEffect.LoggedIn(AfterLogin.Home), awaitItem())
            assertFalse(vm.uiState.value.requesting)
        }
    }

    @Test
    fun `FR-AUTH-02 AC3 REPLACED로 시작 화면에 오면 안내 스낵바를 한 번 보여 준다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(StartAction.SessionReplacedArrived)
            assertEquals(StartSideEffect.ShowMessage(R.string.start_session_replaced), awaitItem())

            // 화면이 다시 그려져 같은 알림이 와도 두 번 보여 주지 않는다
            vm.onAction(StartAction.SessionReplacedArrived)
            expectNoEvents()
        }
    }

    @Test
    fun `FR-AUTH-08 AC1 동의 문구는 strings의 만 14세 문구다`() {
        val strings = File("src/main/res/values/strings.xml").readText()

        assertTrue(strings.contains("시작하면 만 14세 이상이며, %1\$s과 %2\$s에 동의하는 것으로 봐요."))
        assertTrue(strings.contains(">이용약관<"))
        assertTrue(strings.contains(">개인정보처리방침<"))
    }

    @Test
    fun `FR-AUTH-02 AC3 안내 문구는 SCREENS 그대로다`() {
        val strings = File("src/main/res/values/strings.xml").readText()

        assertTrue(strings.contains("다른 기기에서 로그인해서 로그아웃됐어요"))
    }
}
