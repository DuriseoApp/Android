package com.duriseo.android.feature.notification.list

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationList
import com.duriseo.android.domain.notification.NotificationTargetType
import com.duriseo.android.feature.notification.FakeNotificationRepository
import com.duriseo.android.feature.notification.FakeNotificationTapHandler
import com.duriseo.android.feature.notification.notification
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationListViewModelTest {

    /** 2026-10-12(월) 오전 10:20, 서울 */
    private val clock = Clock.fixed(Instant.parse("2026-10-12T01:20:00Z"), ZoneId.of("Asia/Seoul"))
    private val tap = FakeNotificationTapHandler()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: FakeNotificationRepository) = NotificationListViewModel(repo, tap, clock)

    @Test
    fun `FR-NOTI-05 AC2 목록은 서버 순서이고 받은 시각은 D-129 표기다`() = runTest {
        val repo = FakeNotificationRepository(
            listOf(
                notification("n3", createdAt = Instant.parse("2026-10-12T00:12:00Z")), // 오늘 오전 9:12
                notification("n2", createdAt = Instant.parse("2026-10-11T01:00:00Z")), // 어제
                notification("n1", createdAt = Instant.parse("2026-10-06T01:00:00Z")), // 올해
                notification("n0", createdAt = Instant.parse("2025-09-20T01:00:00Z")), // 다른 해
            ),
        )

        val items = viewModel(repo).uiState.value.items

        assertEquals(listOf("n3", "n2", "n1", "n0"), items.map { it.id })
        assertEquals(WrittenAt.Today(isPm = false, hour12 = 9, minute = 12), items[0].writtenAt)
        assertEquals(WrittenAt.Yesterday, items[1].writtenAt)
        assertEquals(WrittenAt.ThisYear(10, 6), items[2].writtenAt)
        assertEquals(WrittenAt.OtherYear(2025, 9, 20), items[3].writtenAt)
    }

    @Test
    fun `FR-NOTI-05 AC3 안 읽은 알림만 안 읽음 표시가 있고 상대 메모와 일정은 종류가 다르다`() = runTest {
        val repo = FakeNotificationRepository(
            listOf(
                notification("a", read = false),
                notification("b", kind = NotificationKind.PARTNER_CREATED, targetType = NotificationTargetType.MEMO, read = true),
                notification("c", kind = NotificationKind.PARTNER_CREATED, targetType = NotificationTargetType.EVENT, read = true),
            ),
        )

        val items = viewModel(repo).uiState.value.items

        assertEquals(listOf(true, false, false), items.map { it.unread })
        assertEquals(NotificationRowType.PARTNER_MEMO, items[1].type)
        assertEquals(NotificationRowType.PARTNER_EVENT, items[2].type)
    }

    @Test
    fun `SCR-NOTI-01 받은 적이 있는데 알림이 없으면 빈 상태이고 못 받았으면 빈 상태가 아니다`() = runTest {
        assertTrue(viewModel(FakeNotificationRepository(emptyList(), loaded = true)).uiState.value.isEmpty)
        assertFalse(viewModel(FakeNotificationRepository(emptyList(), loaded = false)).uiState.value.isEmpty)
    }

    @Test
    fun `SCR-NOTI-01 화면이 보이는 동안 목록이 열려 있고 진입할 때 첫 페이지를 다시 받는다`() = runTest {
        val repo = FakeNotificationRepository()
        val vm = viewModel(repo)

        vm.onAction(NotificationListAction.Shown)
        assertTrue(repo.isListOpen)
        assertEquals(1, repo.refreshFirstPageCalls)

        vm.onAction(NotificationListAction.Hidden)
        assertFalse(repo.isListOpen)
    }

    @Test
    fun `SCR-NOTI-01 다음 페이지가 있을 때만 끝에서 이어 붙인다`() = runTest {
        val repo = FakeNotificationRepository(listOf(notification("n1")))
        val vm = viewModel(repo)
        vm.onAction(NotificationListAction.EndReached)
        assertEquals(0, repo.loadMoreCalls)

        repo.list.value = NotificationList(items = listOf(notification("n1")), hasMore = true, isLoaded = true)
        vm.onAction(NotificationListAction.EndReached)

        assertEquals(1, repo.loadMoreCalls)
    }

    @Test
    fun `FR-NOTI-05 AC6 누르면 기기에서 먼저 읽음으로 바꾸고 바로 이동한다`() = runTest {
        val repo = FakeNotificationRepository(listOf(notification("n1")))
        tap.destination = NotificationDestination.EventDetail("e-n1")
        val vm = viewModel(repo)

        vm.effects.test {
            vm.onAction(NotificationListAction.ItemClicked("n1"))

            assertEquals(NotificationListSideEffect.Navigate(NotificationDestination.EventDetail("e-n1")), awaitItem())
        }
        assertEquals(listOf("n1"), tap.opened)
    }

    @Test
    fun `FR-NOTI-05 AC6 읽음 요청이 실패해도 이동하고 안내하지 않는다`() = runTest {
        // 읽음 요청의 실패는 처리기 안에서 삼켜지고 갈 곳만 돌아온다 (D-127): 화면에는 이동만 있고 안내는 없다
        val vm = viewModel(FakeNotificationRepository(listOf(notification("n1"))))
        tap.destination = NotificationDestination.MemoDetail("m1")

        vm.effects.test {
            vm.onAction(NotificationListAction.ItemClicked("n1"))
            assertEquals(NotificationListSideEffect.Navigate(NotificationDestination.MemoDetail("m1")), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `FR-NOTI-05 AC5 대상이 삭제됐으면 안내하고 목록에 머문다`() = runTest {
        val repo = FakeNotificationRepository(listOf(notification("n1"), notification("n2", kind = NotificationKind.MEMO_PINNED)))
        val vm = viewModel(repo)

        vm.effects.test {
            tap.destination = NotificationDestination.EventDeleted
            vm.onAction(NotificationListAction.ItemClicked("n1"))
            assertEquals(NotificationListSideEffect.ShowMessage(DesignR.string.notification_event_deleted), awaitItem())

            tap.destination = NotificationDestination.MemoDeleted
            vm.onAction(NotificationListAction.ItemClicked("n2"))
            assertEquals(NotificationListSideEffect.ShowMessage(DesignR.string.notification_memo_deleted), awaitItem())
        }
        assertEquals(listOf("n1", "n2"), tap.opened)
    }

    @Test
    fun `FR-NOTI-05 AC6 갈 곳이 없는 알림은 읽음만 하고 이동도 안내도 없다`() = runTest {
        val vm = viewModel(FakeNotificationRepository(listOf(notification("n1", targetType = null))))
        tap.destination = NotificationDestination.None

        vm.effects.test {
            vm.onAction(NotificationListAction.ItemClicked("n1"))
            expectNoEvents()
        }
        assertEquals(listOf("n1"), tap.opened)
    }

    @Test
    fun `SCR-NOTI-01 뒤로 가기를 누르면 닫는다`() = runTest {
        val vm = viewModel(FakeNotificationRepository())

        vm.effects.test {
            vm.onAction(NotificationListAction.BackClicked)
            assertEquals(NotificationListSideEffect.Back, awaitItem())
        }
    }
}
