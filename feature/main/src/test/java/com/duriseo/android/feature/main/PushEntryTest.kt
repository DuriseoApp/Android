package com.duriseo.android.feature.main

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.me.SpaceChange
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import com.duriseo.android.domain.notification.PushTapTarget
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.sync.SpaceSyncSignal
import com.duriseo.android.domain.sync.SyncSpaceSignals
import com.duriseo.android.feature.main.component.MainTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 푸시를 눌러 들어갈 때 (FR-NOTI-03, D-142·D-143). 읽음 처리와 홈을 뒤에 둔 이동 계획은 `MainViewModel`이 세운다 */
@OptIn(ExperimentalCoroutinesApi::class)
class PushEntryTest {

    private class FakeAuth(initial: AuthState) : AuthRepository {
        override val authState = MutableStateFlow(initial)
        override val sessionEvents: Flow<SessionEndReason> = emptyFlow()
        override suspend fun restoreSession() = Unit
        override suspend fun startAsGuest(): DataResult<LoginResult> = error("쓰지 않아요")
        override suspend fun signIn(provider: SocialProvider, token: String): DataResult<LoginResult> = error("쓰지 않아요")
        override suspend fun link(provider: SocialProvider, token: String): DataResult<Unit> = error("쓰지 않아요")
        override suspend fun switchAccount(provider: SocialProvider, token: String): DataResult<LoginResult> = error("쓰지 않아요")
        override suspend fun logout(): DataResult<Unit> = error("쓰지 않아요")
        override suspend fun withdraw(): DataResult<Unit> = error("쓰지 않아요")
    }

    private class FakeMe : MeRepository {
        override fun observeMe(): Flow<Me?> = MutableStateFlow(null)
        override suspend fun refreshMe(): DataResult<Unit> = DataResult.Success(Unit)
        override val spaceChanges: Flow<SpaceChange> = emptyFlow()
        override fun observeLinkedSheetNeeded(): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun acknowledgeSpace() = Unit
        override suspend fun resetAfterUnlink() = Unit
        override suspend fun syncTimeZone(zoneId: String): DataResult<Unit> = DataResult.Success(Unit)
    }

    private val signals = object : SyncSpaceSignals {
        override val spaceSignals: Flow<SpaceSyncSignal> = MutableSharedFlow()
    }
    private val tapHandler = FakeNotificationTapHandler()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(auth: AuthState = AuthState.SignedIn) = MainViewModel(FakeAuth(auth), FakeMe(), signals, tapHandler)

    private fun tap(kind: NotificationKind, type: NotificationTargetType?, id: String? = null, notificationId: String = "n1") =
        PushTapTarget(notificationId, kind, type?.let { NotificationTarget(it, id) })

    @Test
    fun `FR-NOTI-03 AC1 종류마다 정해진 화면으로 간다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onPushTapped(tap(NotificationKind.EVENT_REMINDER, NotificationTargetType.EVENT, "e1"))
            assertEquals(
                PushEntry(NotificationDestination.EventDetail("e1"), MainTab.CALENDAR, null),
                (awaitItem() as MainEffect.PushOpen).entry,
            )

            vm.onPushTapped(tap(NotificationKind.PARTNER_CREATED, NotificationTargetType.EVENT, "e2"))
            assertEquals(NotificationDestination.EventDetail("e2"), (awaitItem() as MainEffect.PushOpen).entry.destination)

            // 상대 메모 추가·메모 고정은 메모 상세이고 메모 탭을 뒤에 둔다 (확인 필요 3)
            vm.onPushTapped(tap(NotificationKind.PARTNER_CREATED, NotificationTargetType.MEMO, "m1"))
            assertEquals(
                PushEntry(NotificationDestination.MemoDetail("m1"), MainTab.MEMO, null),
                (awaitItem() as MainEffect.PushOpen).entry,
            )
            vm.onPushTapped(tap(NotificationKind.MEMO_PINNED, NotificationTargetType.MEMO, "m2"))
            assertEquals(NotificationDestination.MemoDetail("m2"), (awaitItem() as MainEffect.PushOpen).entry.destination)

            vm.onPushTapped(tap(NotificationKind.ANNIVERSARY, NotificationTargetType.ANNIVERSARY))
            assertEquals(
                PushEntry(NotificationDestination.Anniversaries, MainTab.CALENDAR, null),
                (awaitItem() as MainEffect.PushOpen).entry,
            )

            // 연결·해제·탈퇴는 설정 (D-85)
            vm.onPushTapped(tap(NotificationKind.COUPLE_LINKED, NotificationTargetType.SETTINGS))
            assertEquals(NotificationDestination.Settings, (awaitItem() as MainEffect.PushOpen).entry.destination)
            vm.onPushTapped(tap(NotificationKind.COUPLE_UNLINKED, NotificationTargetType.SETTINGS))
            assertEquals(NotificationDestination.Settings, (awaitItem() as MainEffect.PushOpen).entry.destination)
        }
    }

    @Test
    fun `FR-NOTI-03 AC2 푸시를 눌러도 그 알림을 읽음 처리한다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onPushTapped(tap(NotificationKind.MEMO_PINNED, NotificationTargetType.MEMO, "m1", notificationId = "n7"))
            awaitItem()
        }
        assertEquals(listOf("n7"), tapHandler.opened)
    }

    @Test
    fun `FR-NOTI-03 AC3 일정이 삭제됐으면 캘린더에 머물며 삭제된 일정이에요를 보인다`() = runTest {
        tapHandler.override = NotificationDestination.EventDeleted
        val vm = viewModel()
        vm.effects.test {
            vm.onPushTapped(tap(NotificationKind.EVENT_REMINDER, NotificationTargetType.EVENT, "e1"))

            val entry = (awaitItem() as MainEffect.PushOpen).entry
            assertEquals(MainTab.CALENDAR, entry.homeTab)
            assertEquals(DesignR.string.notification_event_deleted, entry.messageRes)
        }
        // 읽음 처리는 했다
        assertEquals(1, tapHandler.opened.size)
    }

    @Test
    fun `FR-NOTI-03 AC3 메모가 삭제됐으면 메모 탭에 머물며 삭제된 메모예요를 보인다`() = runTest {
        tapHandler.override = NotificationDestination.MemoDeleted
        val vm = viewModel()
        vm.effects.test {
            vm.onPushTapped(tap(NotificationKind.MEMO_PINNED, NotificationTargetType.MEMO, "m1"))

            val entry = (awaitItem() as MainEffect.PushOpen).entry
            assertEquals(MainTab.MEMO, entry.homeTab)
            assertEquals(DesignR.string.notification_memo_deleted, entry.messageRes)
        }
    }

    @Test
    fun `FR-NOTI-03 AC3 세션이 없으면 시작 화면으로 간다`() = runTest {
        val vm = viewModel(AuthState.SignedOut)
        vm.effects.test {
            vm.onPushTapped(tap(NotificationKind.EVENT_REMINDER, NotificationTargetType.EVENT, "e1"))

            assertEquals(MainEffect.PushToStart, awaitItem())
        }
        // 세션이 없으면 읽음 처리도 하지 않는다
        assertTrue(tapHandler.opened.isEmpty())
    }

    @Test
    fun `FR-NOTI-03 AC3 앱을 처음 켤 때는 토큰 저장소를 다 읽은 뒤에 세션을 판단한다`() = runTest {
        val auth = FakeAuth(AuthState.Unknown)
        val vm = MainViewModel(auth, FakeMe(), signals, tapHandler)
        vm.effects.test {
            vm.onPushTapped(tap(NotificationKind.ANNIVERSARY, NotificationTargetType.ANNIVERSARY))
            expectNoEvents()

            auth.authState.value = AuthState.SignedIn

            assertEquals(NotificationDestination.Anniversaries, (awaitItem() as MainEffect.PushOpen).entry.destination)
        }
    }
}
