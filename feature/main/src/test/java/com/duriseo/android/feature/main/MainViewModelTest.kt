package com.duriseo.android.feature.main

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.theme.DefaultPersonColors
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MePartner
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.me.SpaceChange
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

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

    private class FakeMeRepository : MeRepository {
        val me = MutableStateFlow<Me?>(null)
        val changes = MutableSharedFlow<SpaceChange>(extraBufferCapacity = 4)
        val sheetNeeded = MutableStateFlow(false)

        override fun observeMe(): Flow<Me?> = me
        override suspend fun refreshMe(): DataResult<Unit> = DataResult.Success(Unit)
        override val spaceChanges: Flow<SpaceChange> = changes
        override fun observeLinkedSheetNeeded(): Flow<Boolean> = sheetNeeded
        override suspend fun acknowledgeSpace() = Unit
        override suspend fun resetAfterUnlink() = Unit
        override suspend fun syncTimeZone(zoneId: String): DataResult<Unit> = DataResult.Success(Unit)
    }

    private val meRepository = FakeMeRepository()

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
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)

        assertEquals(MainStart.Home, vm.start.value)
    }

    @Test
    fun `FR-AUTH-01 AC3 저장된 토큰이 없으면 시작 화면을 보여 준다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedOut), meRepository)

        assertEquals(MainStart.Start, vm.start.value)
    }

    @Test
    fun `FR-AUTH-01 토큰 저장소를 읽는 동안은 빈 화면이다`() = runTest {
        val auth = FakeAuthRepository(AuthState.Unknown)
        val vm = MainViewModel(auth, meRepository)
        assertEquals(MainStart.Loading, vm.start.value)

        auth.authState.value = AuthState.SignedIn

        assertEquals(MainStart.Home, vm.start.value)
    }

    @Test
    fun `FR-AUTH-01 시작 분기는 처음 읽은 결과로 정하고 그 뒤 로그아웃에도 바뀌지 않는다`() = runTest {
        val auth = FakeAuthRepository(AuthState.SignedIn)
        val vm = MainViewModel(auth, meRepository)

        auth.authState.value = AuthState.SignedOut

        // 이후 이동은 내비게이션이 한다 (NavHost의 시작 화면이 바뀌면 안 된다)
        assertEquals(MainStart.Home, vm.start.value)
    }

    @Test
    fun `FR-AUTH-02 AC3 SESSION_REPLACED로 끊기면 안내가 있는 시작 화면으로 보낸다`() = runTest {
        val auth = FakeAuthRepository(AuthState.SignedIn)
        val vm = MainViewModel(auth, meRepository)
        vm.effects.test {
            auth.events.emit(SessionEndReason.REPLACED)

            assertEquals(MainEffect.SessionEnded(replaced = true), awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-02 AC3 SESSION_INVALID면 안내 없이 시작 화면으로 간다`() = runTest {
        val auth = FakeAuthRepository(AuthState.SignedIn)
        val vm = MainViewModel(auth, meRepository)
        vm.effects.test {
            auth.events.emit(SessionEndReason.INVALID)

            assertEquals(MainEffect.SessionEnded(replaced = false), awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-02 AC3 화면이 아직 구독하기 전에 끊겨도 알림이 사라지지 않는다`() = runTest {
        val auth = FakeAuthRepository(AuthState.SignedIn)
        val vm = MainViewModel(auth, meRepository)

        // 화면(MainScreen)이 효과를 구독하기 전에 세션이 끊긴 경우
        auth.events.emit(SessionEndReason.REPLACED)

        vm.effects.test {
            assertEquals(MainEffect.SessionEnded(replaced = true), awaitItem())
        }
    }

    @Test
    fun `FR-COUPLE-05 AC3 연결 해제를 알면 초기화가 끝난 뒤 커플 연결 화면으로 가라고 알린다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        vm.effects.test {
            meRepository.changes.emit(SpaceChange.UNLINKED)

            assertEquals(MainEffect.SpaceUnlinked, awaitItem())
        }
    }

    @Test
    fun `FR-COUPLE-05 AC3 화면이 아직 구독하기 전에 해제를 알아도 알림이 사라지지 않는다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)

        meRepository.changes.emit(SpaceChange.UNLINKED)

        vm.effects.test {
            assertEquals(MainEffect.SpaceUnlinked, awaitItem())
        }
    }

    @Test
    fun `D-125 초기화 뒤 이미 다른 사람과 연결돼 있으면 캘린더로 가서 연결 완료 시트를 띄운다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        vm.effects.test {
            // 초기화가 GET /me로 받아 저장한 내 정보가 커플 공간이다 (시트가 필요하다고 저장소가 알린다)
            meRepository.me.value = meWithColors("pick-orange", "pick-blue", "pick-purple")
            meRepository.sheetNeeded.value = true
            meRepository.changes.emit(SpaceChange.UNLINKED)

            assertEquals(MainEffect.SpaceRelinked, awaitItem())
            assertTrue(vm.linkedSheetNeeded.value)
        }
    }

    @Test
    fun `D-125 초기화 뒤 내 정보가 개인 공간이면 커플 연결 화면으로 간다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        vm.effects.test {
            meRepository.me.value = meWithColors("pick-orange", null, null)
            meRepository.changes.emit(SpaceChange.UNLINKED)

            assertEquals(MainEffect.SpaceUnlinked, awaitItem())
        }
    }

    @Test
    fun `D-125 초기화 뒤 GET me가 실패해 내 정보가 없으면 기존처럼 커플 연결 화면으로 간다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        vm.effects.test {
            meRepository.me.value = null
            meRepository.changes.emit(SpaceChange.UNLINKED)

            assertEquals(MainEffect.SpaceUnlinked, awaitItem())
        }
    }

    @Test
    fun `FR-COUPLE-03 AC4 연결 완료 시트가 필요한지는 내 정보 리포지토리가 알려 준 대로다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        assertFalse(vm.linkedSheetNeeded.value)

        meRepository.sheetNeeded.value = true
        assertTrue(vm.linkedSheetNeeded.value)

        meRepository.sheetNeeded.value = false
        assertFalse(vm.linkedSheetNeeded.value)
    }

    private fun meWithColors(my: String, partner: String?, together: String?) = Me(
        id = "m1",
        accountType = AccountType.MEMBER,
        nickname = "민지",
        timeZone = "Asia/Seoul",
        providers = emptyList(),
        hasProfileImage = false,
        createdAt = Instant.EPOCH,
        space = MeSpace(
            id = "s1",
            type = if (partner == null) SpaceType.PERSONAL else SpaceType.COUPLE,
            myColor = my,
            partner = partner?.let { MePartner("p1", "지우", it, false) },
            togetherColor = together,
            startDate = null,
            importPending = false,
        ),
    )

    @Test
    fun `FR-CAL-04 AC1 내 정보가 오기 전에는 기본 사람 색이다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        assertEquals(DefaultPersonColors, vm.personColors.value)
    }

    @Test
    fun `FR-CAL-04 AC1 내 정보의 색으로 DuriseoTheme에 PersonColors를 넣는다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        meRepository.me.value = meWithColors("pick-rose", "pick-blue", "pick-purple")

        val colors = vm.personColors.value
        assertEquals(DuriseoPickPalette.colorOf("pick-rose"), colors.me)
        assertEquals(DuriseoPickPalette.colorOf("pick-blue"), colors.partner)
        assertEquals(DuriseoPickPalette.colorOf("pick-purple"), colors.together)
    }

    @Test
    fun `FR-CAL-04 AC1 색을 바꾸면 캘린더 일정 색이 바뀐다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        meRepository.me.value = meWithColors("pick-rose", "pick-blue", "pick-purple")
        meRepository.me.value = meWithColors("pick-green", "pick-blue", "pick-purple")

        assertEquals(DuriseoPickPalette.colorOf("pick-green"), vm.personColors.value.me)
    }

    @Test
    fun `FR-CAL-04 AC1 개인 공간은 나 색만 바꾸고 나머지는 기본 사람 색이다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        meRepository.me.value = meWithColors("pick-teal", null, null)

        val colors = vm.personColors.value
        assertEquals(DuriseoPickPalette.colorOf("pick-teal"), colors.me)
        assertEquals(DefaultPersonColors.partner, colors.partner)
        assertEquals(DefaultPersonColors.together, colors.together)
    }

    @Test
    fun `FR-CAL-04 AC1 모르는 팔레트 키는 기본 사람 색으로 그린다`() = runTest {
        val vm = MainViewModel(FakeAuthRepository(AuthState.SignedIn), meRepository)
        meRepository.me.value = meWithColors("pick-unknown", "pick-blue", "nope")

        assertEquals(DefaultPersonColors.me, vm.personColors.value.me)
        assertEquals(DefaultPersonColors.together, vm.personColors.value.together)
    }
}
