package com.duriseo.android.feature.calendar.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.duriseo.android.core.common.navigation.CalendarRouteModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.event.EventTarget
import com.duriseo.android.domain.event.ReminderOption
import com.duriseo.android.feature.calendar.FakeEventRepository
import com.duriseo.android.feature.calendar.FakeMeRepository
import com.duriseo.android.feature.calendar.Offline
import com.duriseo.android.feature.calendar.PartnerId
import com.duriseo.android.feature.calendar.allDay
import com.duriseo.android.feature.calendar.apiError
import com.duriseo.android.feature.calendar.coupleMe
import com.duriseo.android.feature.calendar.sampleEvent
import com.duriseo.android.feature.calendar.soloMe
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {

    private val me = FakeMeRepository(coupleMe())
    private val events = FakeEventRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-12T01:20:00Z"), ZoneId.of("Asia/Seoul"))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(id: String = "e1"): EventDetailViewModel {
        val handle = SavedStateHandle(mapOf(CalendarRouteModel.EventDetail::eventId.name to id))
        val vm = EventDetailViewModel(handle, events, me, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    @Test
    fun `FR-CAL-03 AC1 내가 만든 일정은 수정과 삭제가 보인다`() = runTest {
        events.events.value = listOf(sampleEvent())
        assertTrue(viewModel().uiState.value.event!!.canEdit)
    }

    @Test
    fun `FR-CAL-03 AC2 상대가 만든 나 일정에는 수정과 삭제가 없다`() = runTest {
        events.events.value = listOf(sampleEvent(authorId = PartnerId))
        val vm = viewModel()
        assertFalse(vm.uiState.value.event!!.canEdit)
        assertEquals(EventTarget.PARTNER, vm.uiState.value.event!!.target)
        // 수정·삭제 누르기는 아무 일도 하지 않는다
        vm.onAction(EventDetailAction.DeleteClicked)
        assertFalse(vm.uiState.value.deleteDialogVisible)
    }

    @Test
    fun `FR-CAL-03 AC4 함께 일정은 상대도 수정과 삭제가 보인다`() = runTest {
        events.events.value = listOf(sampleEvent(authorId = PartnerId, isTogether = true))
        assertTrue(viewModel().uiState.value.event!!.canEdit)
    }

    @Test
    fun `FR-CAL-03 AC3 작성자 닉네임이 보인다`() = runTest {
        events.events.value = listOf(sampleEvent())
        assertEquals("민지", viewModel().uiState.value.event!!.authorName)
    }

    @Test
    fun `FR-CAL-03 AC3 상대가 만든 일정은 상대 닉네임을 보여 준다`() = runTest {
        events.events.value = listOf(sampleEvent(authorId = PartnerId))
        assertEquals("지우", viewModel().uiState.value.event!!.authorName)
    }

    @Test
    fun `FR-CAL-03 시간 일정의 시작 종료는 기기 시간대로 보인다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val event = viewModel().uiState.value.event!!
        assertEquals(LocalDate.of(2026, 10, 12), event.startDate)
        assertEquals(LocalTime.of(19, 30), event.startTime)
        assertEquals(LocalTime.of(21, 0), event.endTime)
    }

    @Test
    fun `FR-CAL-03 종일 일정은 날짜만 보인다`() = runTest {
        events.events.value = listOf(sampleEvent(time = allDay(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18))))
        val event = viewModel().uiState.value.event!!
        assertNull(event.startTime)
        assertNull(event.endTime)
        assertEquals(LocalDate.of(2026, 10, 18), event.endDate)
    }

    @Test
    fun `FR-CAL-05 AC1 선택지를 고르면 PUT reminder를 option 값으로 보낸다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val vm = viewModel()
        vm.onAction(EventDetailAction.ReminderSelected(ReminderOption.MIN_10))
        assertEquals(listOf("e1" to ReminderOption.MIN_10), events.reminderCalls)
        assertEquals(ReminderOption.MIN_10, vm.uiState.value.event!!.myReminder)
    }

    @Test
    fun `FR-CAL-05 AC1 없음을 고르면 DELETE reminder를 보낸다`() = runTest {
        events.events.value = listOf(sampleEvent(reminder = ReminderOption.HOUR_1))
        val vm = viewModel()
        vm.onAction(EventDetailAction.ReminderSelected(null))
        assertEquals(listOf<Pair<String, ReminderOption?>>("e1" to null), events.reminderCalls)
        assertNull(vm.uiState.value.event!!.myReminder)
    }

    @Test
    fun `FR-CAL-05 AC1 상대가 만든 일정에도 내 알림을 설정할 수 있다`() = runTest {
        events.events.value = listOf(sampleEvent(authorId = PartnerId))
        val vm = viewModel()
        vm.onAction(EventDetailAction.ReminderSelected(ReminderOption.AT_TIME))
        assertEquals(1, events.reminderCalls.size)
    }

    @Test
    fun `FR-CAL-05 AC1 시간 일정은 다섯 가지 선택지가 보인다`() {
        // 없음 + 4가지 (정시, 10분 전, 1시간 전, 1일 전)
        assertEquals(5, 1 + ReminderOption.optionsFor(isAllDay = false).size)
    }

    @Test
    fun `FR-CAL-05 AC3 종일 일정은 세 가지 선택지가 보인다`() {
        assertEquals(3, 1 + ReminderOption.optionsFor(isAllDay = true).size)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 일정 알림 설정을 안내하고 값은 그대로다`() = runTest {
        events.events.value = listOf(sampleEvent())
        events.reminderResult = Offline
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(EventDetailAction.ReminderSelected(ReminderOption.DAY_1))
            assertEquals(EventDetailSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
        assertNull(vm.uiState.value.event!!.myReminder)
        assertFalse(vm.uiState.value.reminderSaving)
    }

    @Test
    fun `FR-CAL-03 삭제를 확인하면 DELETE events를 보내고 닫는다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(EventDetailAction.DeleteClicked)
            assertTrue(vm.uiState.value.deleteDialogVisible)
            vm.onAction(EventDetailAction.DeleteConfirmed)
            assertEquals(EventDetailSideEffect.Close, awaitItem())
        }
        assertEquals(listOf("e1"), events.deleted)
    }

    @Test
    fun `FR-CAL-03 삭제 다이얼로그에서 취소하면 아무것도 보내지 않는다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val vm = viewModel()
        vm.onAction(EventDetailAction.DeleteClicked)
        vm.onAction(EventDetailAction.DeleteCancelled)
        assertFalse(vm.uiState.value.deleteDialogVisible)
        assertTrue(events.deleted.isEmpty())
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 일정 삭제 요청을 보내지 않고 안내한다`() = runTest {
        events.events.value = listOf(sampleEvent())
        events.deleteResult = Offline
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(EventDetailAction.DeleteClicked)
            vm.onAction(EventDetailAction.DeleteConfirmed)
            assertEquals(EventDetailSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
        assertFalse(vm.uiState.value.deleteDialogVisible)
        assertNotNull(vm.uiState.value.event)
    }

    @Test
    fun `FR-CAL-03 상대가 지워서 일정이 사라지면 오류 안내와 함께 닫는다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val vm = viewModel()
        vm.effects.test {
            events.events.value = emptyList()
            assertEquals(EventDetailSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(EventDetailSideEffect.Close, awaitItem())
        }
    }

    @Test
    fun `FR-CAL-03 수정을 누르면 수정 화면을 연다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(EventDetailAction.EditClicked)
            assertEquals(EventDetailSideEffect.OpenEdit("e1"), awaitItem())
        }
    }

    @Test
    fun `NFR-SYNC-02 화면이 보이면 그 일정을 다시 읽는다`() = runTest {
        events.events.value = listOf(sampleEvent())
        viewModel().onAction(EventDetailAction.Shown)
        assertEquals(listOf("e1"), events.refreshedIds)
    }

    @Test
    fun `FR-CAL-02 AC4 개인 공간 일정의 대상은 항상 나다`() = runTest {
        me.me.value = soloMe()
        events.events.value = listOf(sampleEvent())
        assertEquals(EventTarget.ME, viewModel().uiState.value.event!!.target)
    }

    @Test
    fun `D-109 개인 공간이면 삭제 확인 본문이 개인용이다`() = runTest {
        me.me.value = soloMe()
        events.events.value = listOf(sampleEvent())
        assertTrue(viewModel().uiState.value.isPersonalSpace)
    }

    @Test
    fun `D-109 커플 공간은 개인 공간 표시가 꺼져 있다`() = runTest {
        events.events.value = listOf(sampleEvent())
        assertFalse(viewModel().uiState.value.isPersonalSpace)
    }

    @Test
    fun `D-115 일정 상세는 요청 중에는 loading이 true이고 끝나면 false다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val deleteGate = CompletableDeferred<Unit>()
        val reminderGate = CompletableDeferred<Unit>()
        events.deleteGate = deleteGate
        events.reminderGate = reminderGate
        val vm = viewModel()
        vm.effects.test {
            assertFalse(vm.uiState.value.loading)

            // 내 알림 설정
            vm.onAction(EventDetailAction.ReminderSelected(ReminderOption.MIN_10))
            assertTrue(vm.uiState.value.loading)
            reminderGate.complete(Unit)
            assertFalse(vm.uiState.value.loading)

            // 삭제
            vm.onAction(EventDetailAction.DeleteClicked)
            vm.onAction(EventDetailAction.DeleteConfirmed)
            assertTrue(vm.uiState.value.loading)
            deleteGate.complete(Unit)
            assertEquals(EventDetailSideEffect.Close, awaitItem())
            assertFalse(vm.uiState.value.loading)
        }
    }

    @Test
    fun `D-111 삭제에서 일정이 없다고 하면 공통 오류를 한 번 보이고 닫는다`() = runTest {
        events.events.value = listOf(sampleEvent())
        events.deleteResult = apiError("NOT_FOUND")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(EventDetailAction.DeleteClicked)
            vm.onAction(EventDetailAction.DeleteConfirmed)
            assertEquals(EventDetailSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(EventDetailSideEffect.Close, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `D-111 알림 변경에서 일정이 없다고 하면 공통 오류를 보이고 닫는다`() = runTest {
        events.events.value = listOf(sampleEvent())
        events.reminderResult = apiError("NOT_FOUND")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(EventDetailAction.ReminderSelected(ReminderOption.MIN_10))
            assertEquals(EventDetailSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(EventDetailSideEffect.Close, awaitItem())
        }
    }

    @Test
    fun `D-111 삭제에서 권한이 없다고 하면 공통 오류만 보이고 화면은 그대로다`() = runTest {
        events.events.value = listOf(sampleEvent())
        events.deleteResult = apiError("NOT_AUTHOR")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(EventDetailAction.DeleteClicked)
            vm.onAction(EventDetailAction.DeleteConfirmed)
            assertEquals(EventDetailSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            expectNoEvents()
        }
        assertNotNull(vm.uiState.value.event)
    }
}
