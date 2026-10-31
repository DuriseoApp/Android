package com.duriseo.android.feature.main

import app.cash.turbine.test
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private class FakeAuthRepository(initial: AuthState) : AuthRepository {
        override val authState = MutableStateFlow(initial)
        val events = MutableSharedFlow<SessionEndReason>(extraBufferCapacity = 4)
        override val sessionEvents: Flow<SessionEndReason> = events

        override suspend fun restoreSession() = Unit
        override suspend fun startAsGuest(): DataResult<LoginResult> = error("쓰지 않아요")
        override suspend fun signIn(provider: SocialProvider, token: String): DataResult<LoginResult> = error("쓰지 않아요")
        override suspend fun link(provider: SocialProvider, token: String): DataResult<Unit> = error("쓰지 않아요")
        override suspend fun switchAccount(provider: SocialProvider, token: String): DataResult<LoginResult> =
            error("쓰지 않아요")
        override suspend fun logout(): DataResult<Unit> = error("쓰지 않아요")
        override suspend fun withdraw(): DataResult<Unit> = error("쓰지 않아요")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FR-AUTH-01 AC2 저장된 토큰이 있으면 시작 화면 없이 캘린더로 간다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn))

        assertEquals(MainStart.Home, vm.start.value)
    }

    @Test
    fun `FR-AUTH-01 AC3 저장된 토큰이 없으면 시작 화면을 보여 준다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedOut))

        assertEquals(MainStart.Start, vm.start.value)
    }

    @Test
    fun `FR-AUTH-01 토큰 저장소를 읽는 동안은 빈 화면이다`() = runTest {
        val auth = FakeAuthRepository(AuthState.Unknown)
        val vm = MainViewModel(auth)
        assertEquals(MainStart.Loading, vm.start.value)

        auth.authState.value = AuthState.SignedIn

        assertEquals(MainStart.Home, vm.start.value)
    }

    @Test
    fun `FR-AUTH-01 시작 분기는 처음 읽은 결과로 정하고 그 뒤 로그아웃에도 바뀌지 않는다`() = runTest {
        val auth = FakeAuthRepository(AuthState.SignedIn)
        val vm = MainViewModel(auth)

        auth.authState.value = AuthState.SignedOut

        // 이후 이동은 내비게이션이 한다 (NavHost의 시작 화면이 바뀌면 안 된다)
        assertEquals(MainStart.Home, vm.start.value)
    }

    @Test
    fun `FR-AUTH-02 AC3 SESSION_REPLACED로 끊기면 안내가 있는 시작 화면으로 보낸다`() = runTest {
        val auth = FakeAuthRepository(AuthState.SignedIn)
        val vm = MainViewModel(auth)
        vm.effects.test {
            auth.events.emit(SessionEndReason.REPLACED)

            assertEquals(MainEffect.SessionEnded(replaced = true), awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-02 AC3 SESSION_INVALID면 안내 없이 시작 화면으로 간다`() = runTest {
        val auth = FakeAuthRepository(AuthState.SignedIn)
        val vm = MainViewModel(auth)
        vm.effects.test {
            auth.events.emit(SessionEndReason.INVALID)

            assertEquals(MainEffect.SessionEnded(replaced = false), awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-02 AC3 화면이 아직 구독하기 전에 끊겨도 알림이 사라지지 않는다`() = runTest {
        val auth = FakeAuthRepository(AuthState.SignedIn)
        val vm = MainViewModel(auth)

        // 화면(MainScreen)이 효과를 구독하기 전에 세션이 끊긴 경우
        auth.events.emit(SessionEndReason.REPLACED)

        vm.effects.test {
            assertEquals(MainEffect.SessionEnded(replaced = true), awaitItem())
        }
    }
}
