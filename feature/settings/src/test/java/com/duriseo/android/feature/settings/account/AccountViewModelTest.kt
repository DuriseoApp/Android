package com.duriseo.android.feature.settings.account

import app.cash.turbine.test
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.settings.FakeAuthRepository
import com.duriseo.android.feature.settings.FakeMeRepository
import com.duriseo.android.feature.settings.GuestId
import com.duriseo.android.feature.settings.apiError
import com.duriseo.android.feature.settings.guestMe
import com.duriseo.android.feature.settings.memberMe
import com.duriseo.android.feature.settings.socialToken
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
class AccountViewModelTest {

    private val me = FakeMeRepository(guestMe())
    private val auth = FakeAuthRepository(me)
    private val seoul = Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneId.of("Asia/Seoul"))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(clock: Clock = seoul): AccountViewModel {
        val vm = AccountViewModel(auth, me, clock)
        // stateIn(WhileSubscribed)라서 값을 읽는 동안 구독해 둔다
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    private fun AccountViewModel.received(provider: SocialProvider, result: SocialTokenResult) =
        onAction(AccountAction.SocialTokenReceived(provider, result))

    /** 연동을 눌러 SDK 요청 효과를 받고, 토큰을 돌려주는 데까지 */
    private suspend fun app.cash.turbine.ReceiveTurbine<AccountSideEffect>.linkWith(
        vm: AccountViewModel,
        provider: SocialProvider,
        token: String = "social-token",
    ) {
        vm.onAction(AccountAction.LinkClicked(provider))
        assertEquals(AccountSideEffect.RequestSocialToken(provider), awaitItem())
        vm.received(provider, socialToken(provider, token))
    }

    // ---- 진입 시 내 정보 갱신 (FR-AUTH-03, FR-SET-01) ----

    @Test
    fun `FR-AUTH-03 계정 화면에 들어오면 내 정보를 서버에서 다시 읽는다`() = runTest {
        viewModel()

        assertEquals(1, me.refreshCalls)
    }

    // ---- 게스트 연동 (FR-AUTH-03 AC1) ----

    @Test
    fun `FR-AUTH-03 AC1 연동이 끝나면 내 정보를 다시 읽어 회원 계정 화면이 된다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            assertEquals(AccountContent.Guest(GuestId), vm.uiState.value.content)

            linkWith(vm, SocialProvider.GOOGLE)

            assertEquals(listOf(SocialProvider.GOOGLE to "social-token"), auth.linkCalls)
            val state = vm.uiState.value
            assertEquals(
                AccountContent.Member(SocialProvider.GOOGLE, "게스트", LocalDate.of(2026, 10, 12)),
                state.content,
            )
            assertTrue(state.justLinked)
            assertFalse(state.requesting)
        }
    }

    @Test
    fun `FR-AUTH-03 AC1 카카오로 연동하면 카카오 회원 화면이 된다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.KAKAO)

            assertEquals(SocialProvider.KAKAO, (vm.uiState.value.content as AccountContent.Member).provider)
        }
    }

    @Test
    fun `FR-SET-01 가입일은 기기 시간대의 날짜로 바꿔 보여 준다`() = runTest {
        // 2026-10-11 16:00 UTC = 서울 2026-10-12 01:00
        me.me.value = memberMe(createdAt = Instant.parse("2026-10-11T16:00:00Z"))
        val vm = viewModel(Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneId.of("Asia/Seoul")))

        assertEquals(LocalDate.of(2026, 10, 12), (vm.uiState.value.content as AccountContent.Member).joinedDate)
    }

    @Test
    fun `FR-SET-01 가입일은 시간대가 다르면 날짜도 달라진다`() = runTest {
        me.me.value = memberMe(createdAt = Instant.parse("2026-10-11T16:00:00Z"))
        val vm = viewModel(Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneId.of("UTC")))

        assertEquals(LocalDate.of(2026, 10, 11), (vm.uiState.value.content as AccountContent.Member).joinedDate)
    }

    @Test
    fun `FR-SET-01 게스트 ID는 줄이지 않고 Me id 그대로 보여 준다`() = runTest {
        val vm = viewModel()

        assertEquals(36, GuestId.length)
        assertEquals(GuestId, (vm.uiState.value.content as AccountContent.Guest).guestId)
    }

    @Test
    fun `FR-AUTH-03 소셜 로그인을 취소하면 요청하지 않고 게스트 화면에 머문다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.LinkClicked(SocialProvider.GOOGLE))
            awaitItem()

            vm.received(SocialProvider.GOOGLE, SocialTokenResult.Cancelled)

            expectNoEvents()
            assertTrue(auth.linkCalls.isEmpty())
            assertFalse(vm.uiState.value.requesting)
            assertTrue(vm.uiState.value.content is AccountContent.Guest)
        }
    }

    @Test
    fun `D-88 소셜 SDK 실패와 SOCIAL_TOKEN_INVALID와 GUEST_ONLY는 공통 오류 안내를 보여 준다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.LinkClicked(SocialProvider.GOOGLE))
            awaitItem()
            vm.received(SocialProvider.GOOGLE, SocialTokenResult.Failed())
            assertEquals(AccountSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())

            listOf("SOCIAL_TOKEN_INVALID", "GUEST_ONLY").forEach { code ->
                auth.linkResult = apiError(code)
                linkWith(vm, SocialProvider.GOOGLE)
                assertEquals(code, AccountSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
                assertFalse(vm.uiState.value.requesting)
            }
        }
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 연동이 실패하고 인터넷 연결 안내를 보여 준다`() = runTest {
        auth.linkResult = DataResult.Failure(DataError.Offline)
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE)

            assertEquals(AccountSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            assertTrue(vm.uiState.value.content is AccountContent.Guest)
        }
    }

    @Test
    fun `D-88 요청 중에는 다른 시작 버튼을 눌러도 새 요청을 만들지 않는다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.LinkClicked(SocialProvider.GOOGLE))
            awaitItem()
            assertTrue(vm.uiState.value.requesting)

            vm.onAction(AccountAction.LinkClicked(SocialProvider.KAKAO))
            vm.onAction(AccountAction.WithdrawClicked)

            expectNoEvents()
            assertEquals(null, vm.uiState.value.dialog)
        }
    }

    // ---- 이미 가입된 계정 (FR-AUTH-03 AC2·AC3) ----

    @Test
    fun `FR-AUTH-03 AC2 SOCIAL_ALREADY_USED면 이미 가입된 계정 다이얼로그를 보여 준다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE)

            val state = vm.uiState.value
            assertEquals(AccountDialog.AlreadyUsed, state.dialog)
            assertFalse(state.requesting)
            // 연동은 하지 않았다: 게스트 그대로
            assertTrue(state.content is AccountContent.Guest)
            assertFalse(state.justLinked)
        }
    }

    @Test
    fun `FR-AUTH-03 AC3 그 계정으로 로그인을 누르면 삭제 확인 다이얼로그를 보여 준다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE)

            vm.onAction(AccountAction.ExistingLoginClicked)

            assertEquals(AccountDialog.SwitchConfirm, vm.uiState.value.dialog)
            assertTrue(auth.switchCalls.isEmpty())
        }
    }

    @Test
    fun `FR-AUTH-03 AC3 삭제하고 로그인을 누르면 같은 소셜 토큰으로 switch를 보낸다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.KAKAO, token = "kakao-access")
            vm.onAction(AccountAction.ExistingLoginClicked)

            vm.onAction(AccountAction.SwitchConfirmed)

            assertEquals(listOf(SocialProvider.KAKAO to "kakao-access"), auth.switchCalls)
            assertEquals(AccountSideEffect.NavigateToHome, awaitItem())
            assertEquals(null, vm.uiState.value.dialog)
        }
    }

    @Test
    fun `FR-AUTH-03 AC3 두 다이얼로그에서 취소하면 아무 요청도 보내지 않는다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE)
            // 다이얼로그 ①에서 취소
            vm.onAction(AccountAction.DialogDismissed)
            assertEquals(null, vm.uiState.value.dialog)

            // 다이얼로그 ②에서 취소
            linkWith(vm, SocialProvider.GOOGLE)
            vm.onAction(AccountAction.ExistingLoginClicked)
            vm.onAction(AccountAction.DialogDismissed)
            assertEquals(null, vm.uiState.value.dialog)

            // 취소한 뒤에는 switch를 보낼 수 없다 (토큰을 버렸다)
            vm.onAction(AccountAction.SwitchConfirmed)

            assertTrue(auth.switchCalls.isEmpty())
            expectNoEvents()
            assertTrue(vm.uiState.value.content is AccountContent.Guest)
        }
    }

    @Test
    fun `D-67 switch가 SOCIAL_TOKEN_INVALID면 토큰을 한 번 다시 받아 다시 보낸다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        auth.switchResults += apiError("SOCIAL_TOKEN_INVALID")
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE, token = "old-token")
            vm.onAction(AccountAction.ExistingLoginClicked)

            vm.onAction(AccountAction.SwitchConfirmed)

            // 사용자 동작 없이 SDK에서 새 토큰을 요청한다
            assertEquals(AccountSideEffect.RequestSocialToken(SocialProvider.GOOGLE), awaitItem())
            assertTrue(vm.uiState.value.requesting)
            vm.received(SocialProvider.GOOGLE, socialToken(SocialProvider.GOOGLE, "new-token"))

            assertEquals(
                listOf(SocialProvider.GOOGLE to "old-token", SocialProvider.GOOGLE to "new-token"),
                auth.switchCalls,
            )
            assertEquals(AccountSideEffect.NavigateToHome, awaitItem())
        }
    }

    @Test
    fun `D-67 switch가 다시 SOCIAL_TOKEN_INVALID면 공통 오류를 보여 주고 게스트로 남는다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        auth.switchResults += apiError("SOCIAL_TOKEN_INVALID")
        auth.switchResults += apiError("SOCIAL_TOKEN_INVALID")
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE)
            vm.onAction(AccountAction.ExistingLoginClicked)
            vm.onAction(AccountAction.SwitchConfirmed)
            awaitItem() // RequestSocialToken
            vm.received(SocialProvider.GOOGLE, socialToken(SocialProvider.GOOGLE, "new-token"))

            assertEquals(AccountSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(2, auth.switchCalls.size)
            assertEquals(null, vm.uiState.value.dialog)
            assertTrue(vm.uiState.value.content is AccountContent.Guest)
        }
    }

    @Test
    fun `D-67 SDK가 새 토큰을 못 받으면 공통 오류를 보여 주고 다이얼로그를 닫는다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        auth.switchResults += apiError("SOCIAL_TOKEN_INVALID")
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE)
            vm.onAction(AccountAction.ExistingLoginClicked)
            vm.onAction(AccountAction.SwitchConfirmed)
            awaitItem()

            vm.received(SocialProvider.GOOGLE, SocialTokenResult.Failed())

            assertEquals(AccountSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(null, vm.uiState.value.dialog)
            assertEquals(1, auth.switchCalls.size)
        }
    }

    @Test
    fun `D-67 switch가 SOCIAL_NOT_REGISTERED면 공통 오류를 보여 주고 다이얼로그를 닫고 자동 연동은 하지 않는다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        auth.switchResults += apiError("SOCIAL_NOT_REGISTERED")
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE)
            vm.onAction(AccountAction.ExistingLoginClicked)

            vm.onAction(AccountAction.SwitchConfirmed)

            assertEquals(AccountSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(null, vm.uiState.value.dialog)
            // 처음 연동 한 번뿐이다 (자동으로 link를 다시 보내지 않는다)
            assertEquals(1, auth.linkCalls.size)
            assertTrue(vm.uiState.value.content is AccountContent.Guest)
        }
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 switch가 실패하고 인터넷 연결 안내를 보여 준다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        auth.switchResults += DataResult.Failure(DataError.Offline)
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE)
            vm.onAction(AccountAction.ExistingLoginClicked)

            vm.onAction(AccountAction.SwitchConfirmed)

            assertEquals(AccountSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            assertEquals(null, vm.uiState.value.dialog)
        }
    }

    // ---- 로그아웃 (FR-AUTH-05) ----

    @Test
    fun `FR-AUTH-05 AC2 게스트 계정 화면에는 로그아웃이 없고 로그아웃 다이얼로그도 열리지 않는다`() = runTest {
        val vm = viewModel()

        // 게스트 화면은 로그아웃 버튼을 그리지 않는다 (AccountScreen GuestContent)
        assertTrue(vm.uiState.value.content is AccountContent.Guest)
    }

    @Test
    fun `FR-AUTH-05 AC1 계정 화면에서 로그아웃을 확인하면 시작 화면으로 간다`() = runTest {
        me.me.value = memberMe()
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.LogoutClicked)
            assertEquals(AccountDialog.Logout, vm.uiState.value.dialog)

            vm.onAction(AccountAction.LogoutConfirmed)

            assertEquals(AccountSideEffect.NavigateToStart, awaitItem())
            assertEquals(1, auth.logoutCalls)
            assertEquals(null, vm.uiState.value.dialog)
        }
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 계정 화면 로그아웃이 실패하고 인터넷 연결 안내를 보여 준다`() = runTest {
        me.me.value = memberMe()
        auth.logoutResult = DataResult.Failure(DataError.Offline)
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.LogoutClicked)
            vm.onAction(AccountAction.LogoutConfirmed)

            assertEquals(AccountSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            assertEquals(null, vm.uiState.value.dialog)
        }
    }

    // ---- 탈퇴 (FR-AUTH-06) ----

    @Test
    fun `FR-AUTH-06 AC1 개인 공간이면 바로 삭제된다는 탈퇴 문구를 쓴다`() = runTest {
        me.me.value = memberMe(partnerNickname = null)
        val vm = viewModel()

        assertEquals(WithdrawMessage.Personal, vm.uiState.value.withdrawMessage)
    }

    @Test
    fun `FR-AUTH-06 AC1 게스트도 개인 공간 탈퇴 문구를 쓴다`() = runTest {
        val vm = viewModel()

        assertEquals(WithdrawMessage.Personal, vm.uiState.value.withdrawMessage)
    }

    @Test
    fun `FR-AUTH-06 AC2 커플 공간이면 상대 이름이 들어간 탈퇴 문구를 쓴다`() = runTest {
        me.me.value = memberMe(partnerNickname = "하늘")
        val vm = viewModel()

        assertEquals(WithdrawMessage.Couple("하늘"), vm.uiState.value.withdrawMessage)
    }

    @Test
    fun `FR-AUTH-06 AC1 탈퇴를 확인하면 탈퇴하고 시작 화면으로 간다`() = runTest {
        me.me.value = memberMe()
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.WithdrawClicked)
            assertEquals(AccountDialog.Withdraw, vm.uiState.value.dialog)

            vm.onAction(AccountAction.WithdrawConfirmed)

            assertEquals(AccountSideEffect.NavigateToStart, awaitItem())
            assertEquals(1, auth.withdrawCalls)
        }
    }

    @Test
    fun `FR-AUTH-06 AC1 게스트도 계정 화면에서 탈퇴할 수 있다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.WithdrawClicked)
            vm.onAction(AccountAction.WithdrawConfirmed)

            assertEquals(AccountSideEffect.NavigateToStart, awaitItem())
        }
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 탈퇴가 실패하고 인터넷 연결 안내를 보여 주며 로그인 상태를 유지한다`() = runTest {
        auth.withdrawResult = DataResult.Failure(DataError.Offline)
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.WithdrawClicked)
            vm.onAction(AccountAction.WithdrawConfirmed)

            assertEquals(AccountSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            assertEquals(null, vm.uiState.value.dialog)
            assertTrue(vm.uiState.value.content is AccountContent.Guest)
        }
    }

    @Test
    fun `FR-AUTH-06 AC5 탈퇴가 서버 오류로 실패하면 공통 오류를 보여 주고 다이얼로그를 닫는다`() = runTest {
        auth.withdrawResult = apiError("INTERNAL_ERROR")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.WithdrawClicked)
            vm.onAction(AccountAction.WithdrawConfirmed)

            assertEquals(AccountSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(null, vm.uiState.value.dialog)
        }
    }

    @Test
    fun `FR-AUTH-06 AC5 이미 끊긴 세션이라는 응답이면 안내 없이 세션 종료 흐름에 맡긴다`() = runTest {
        auth.withdrawResult = apiError("SESSION_REPLACED")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(AccountAction.WithdrawClicked)
            vm.onAction(AccountAction.WithdrawConfirmed)

            expectNoEvents()
            assertEquals(null, vm.uiState.value.dialog)
        }
    }

    @Test
    fun `FR-AUTH-06 AC1 취소하면 탈퇴를 요청하지 않는다`() = runTest {
        val vm = viewModel()
        vm.onAction(AccountAction.WithdrawClicked)

        vm.onAction(AccountAction.DialogDismissed)

        assertEquals(null, vm.uiState.value.dialog)
        assertEquals(0, auth.withdrawCalls)
    }

    @Test
    fun `FR-AUTH-06 AC1 탈퇴 다이얼로그가 없으면 탈퇴 확인을 무시한다`() = runTest {
        val vm = viewModel()

        vm.onAction(AccountAction.WithdrawConfirmed)

        assertEquals(0, auth.withdrawCalls)
    }

    @Test
    fun `FR-AUTH-03 AC3 switch 결과는 LoginResult를 쓰지 않고 계정의 현재 상태로 이동한다`() = runTest {
        auth.linkResult = apiError("SOCIAL_ALREADY_USED")
        auth.switchResults += DataResult.Success(LoginResult(isNewUser = false))
        val vm = viewModel()
        vm.effects.test {
            linkWith(vm, SocialProvider.GOOGLE)
            vm.onAction(AccountAction.ExistingLoginClicked)
            vm.onAction(AccountAction.SwitchConfirmed)

            assertEquals(AccountSideEffect.NavigateToHome, awaitItem())
        }
    }
}
