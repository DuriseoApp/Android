package com.duriseo.android.feature.calendar.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.duriseo.android.core.common.navigation.CalendarRouteModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.event.EventRules
import com.duriseo.android.domain.event.EventTime
import com.duriseo.android.domain.event.NoteChange
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
class EventEditViewModelTest {

    private val me = FakeMeRepository(coupleMe())
    private val events = FakeEventRepository()

    /** 2026-10-12(월) 오전 10:20, 서울 */
    private val clock = Clock.fixed(Instant.parse("2026-10-12T01:20:00Z"), ZoneId.of("Asia/Seoul"))
    private val seoul = ZoneId.of("Asia/Seoul")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun create(date: String? = "2026-10-12"): EventEditViewModel {
        val args = buildMap<String, Any?> { if (date != null) put(CalendarRouteModel.EventEdit::date.name, date) }
        return EventEditViewModel(SavedStateHandle(args), events, me, clock)
    }

    /** [utc] 시각에 연 작성 화면 (서울) */
    private fun createAt(utc: String, date: String = "2026-10-12") = EventEditViewModel(
        SavedStateHandle(mapOf(CalendarRouteModel.EventEdit::date.name to date)),
        events,
        me,
        Clock.fixed(Instant.parse(utc), seoul),
    )

    private fun edit(id: String = "e1"): EventEditViewModel =
        EventEditViewModel(SavedStateHandle(mapOf(CalendarRouteModel.EventEdit::eventId.name to id)), events, me, clock)

    private fun EventEditViewModel.fill(title: String = "저녁 데이트") = onAction(EventEditAction.TitleChanged(title))

    // --- 종류 탭 [일정 | 메모] (D-133) ---

    @Test
    fun `D-133 수정할 때는 종류 탭이 없다`() = runTest {
        events.events.value = listOf(sampleEvent(id = "e1"))
        val vm = EventEditViewModel(
            SavedStateHandle(
                mapOf(CalendarRouteModel.EventEdit::eventId.name to "e1", CalendarRouteModel.EventEdit::startOnMemo.name to true),
            ),
            events,
            me,
            clock,
        )
        assertTrue(vm.uiState.value.loaded)
        assertFalse(vm.uiState.value.showKindTabs)
        // 메모 탭으로 열라는 인자가 있어도 수정은 일정이고, 탭을 골라도 바뀌지 않는다
        assertEquals(EditKind.Event, vm.uiState.value.kind)
        vm.onAction(EventEditAction.KindSelected(EditKind.Memo))
        assertEquals(EditKind.Event, vm.uiState.value.kind)
    }

    @Test
    fun `D-133 새로 쓸 때는 종류 탭이 있고 일정 탭으로 시작한다`() = runTest {
        val state = create().uiState.value
        assertTrue(state.showKindTabs)
        assertEquals(EditKind.Event, state.kind)
    }

    @Test
    fun `D-133 메모 탭의 FAB에서 열면 메모 탭이 골라져 있다`() = runTest {
        val vm = EventEditViewModel(
            SavedStateHandle(mapOf(CalendarRouteModel.EventEdit::startOnMemo.name to true)),
            events,
            me,
            clock,
        )
        assertEquals(EditKind.Memo, vm.uiState.value.kind)
    }

    @Test
    fun `D-133 탭을 오가도 일정 입력은 그대로 남는다`() = runTest {
        val vm = create()
        vm.fill("저녁 데이트")
        vm.onAction(EventEditAction.KindSelected(EditKind.Memo))
        assertEquals(EditKind.Memo, vm.uiState.value.kind)
        vm.onAction(EventEditAction.KindSelected(EditKind.Event))
        assertEquals("저녁 데이트", vm.uiState.value.title)
        assertTrue(events.created.isEmpty())
    }

    // --- 새 일정 기본값 (D-114, Q-17) ---

    @Test
    fun `FR-CAL-02 AC1 새 일정은 선택한 날 다음 정시부터 한 시간으로 시작한다`() = runTest {
        val state = create("2026-10-14").uiState.value
        assertEquals(LocalDate.of(2026, 10, 14), state.startDate)
        assertEquals(LocalTime.of(11, 0), state.startTime)
        assertEquals(LocalTime.of(12, 0), state.endTime)
        assertFalse(state.endError)
    }

    @Test
    fun `D-114 Q-17 22시대에 열면 시작은 23시 종료는 같은 날 23시 59분이다`() = runTest {
        val state = createAt("2026-10-12T13:30:00Z").uiState.value // 서울 22:30
        assertEquals(LocalTime.of(23, 0), state.startTime)
        assertEquals(LocalTime.of(23, 59), state.endTime)
        assertEquals(LocalDate.of(2026, 10, 12), state.endDate)
        assertFalse(state.endError)
    }

    @Test
    fun `D-114 Q-17 23시대에 열면 시작은 선택한 날 23시 종료는 23시 59분이다`() = runTest {
        val state = createAt("2026-10-12T14:30:00Z", "2026-10-15").uiState.value // 서울 23:30
        assertEquals(LocalDate.of(2026, 10, 15), state.startDate)
        assertEquals(LocalTime.of(23, 0), state.startTime)
        assertEquals(LocalDate.of(2026, 10, 15), state.endDate)
        assertEquals(LocalTime.of(23, 59), state.endTime)
    }

    @Test
    fun `D-114 Q-17 2050년 12월 31일을 골라도 날짜 범위를 넘지 않는다`() = runTest {
        val state = createAt("2026-10-12T14:30:00Z", "2050-12-31").uiState.value
        assertEquals(LocalDate.of(2050, 12, 31), state.startDate)
        assertEquals(LocalDate.of(2050, 12, 31), state.endDate)
    }

    @Test
    fun `D-114 종일을 켰다가 끄면 마지막으로 고른 시간으로 돌아간다`() = runTest {
        val vm = create()
        vm.onAction(EventEditAction.TimeRowClicked(EditField.Start))
        vm.onAction(EventEditAction.TimeConfirmed(LocalTime.of(14, 30)))
        vm.onAction(EventEditAction.AllDayChanged(true))
        vm.onAction(EventEditAction.AllDayChanged(false))
        assertEquals(LocalTime.of(14, 30), vm.uiState.value.startTime)
    }

    @Test
    fun `FR-CAL-02 AC1 날짜를 모르면 오늘로 시작한다`() = runTest {
        assertEquals(LocalDate.of(2026, 10, 12), create(null).uiState.value.startDate)
    }

    @Test
    fun `FR-CAL-02 AC1 제목과 시작 종료를 넣고 저장하면 POST events를 보낸다`() = runTest {
        val vm = create()
        vm.fill()
        vm.effects.test {
            vm.onAction(EventEditAction.SaveClicked)
            assertEquals(EventEditSideEffect.Close, awaitItem())
        }
        val sent = events.created.single()
        assertEquals("저녁 데이트", sent.title)
        val time = sent.time as EventTime.Timed
        assertEquals(Instant.parse("2026-10-12T02:00:00Z"), time.start)
        assertEquals(Instant.parse("2026-10-12T03:00:00Z"), time.end)
    }

    @Test
    fun `FR-CAL-02 AC1 D-89 제목은 앞뒤 공백을 지워 보낸다`() = runTest {
        val vm = create()
        vm.fill("  저녁 데이트  ")
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals("저녁 데이트", events.created.single().title)
    }

    @Test
    fun `FR-CAL-02 AC1 D-89 공백만 있는 제목은 저장할 수 없다`() = runTest {
        val vm = create()
        vm.fill("   ")
        assertFalse(vm.uiState.value.saveEnabled)
        vm.onAction(EventEditAction.SaveClicked)
        assertTrue(events.created.isEmpty())
    }

    @Test
    fun `FR-CAL-02 AC1 제목이 비어 있으면 저장 버튼이 꺼진다`() = runTest {
        assertFalse(create().uiState.value.saveEnabled)
    }

    @Test
    fun `FR-CAL-02 AC2 종일 일정은 날짜만 보낸다`() = runTest {
        val vm = create("2026-10-17")
        vm.fill()
        vm.onAction(EventEditAction.AllDayChanged(true))
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals(allDay(LocalDate.of(2026, 10, 17)), events.created.single().time)
    }

    @Test
    fun `FR-CAL-02 AC2 하루 종일을 켜면 시작 종료 행의 시간이 사라진다`() = runTest {
        val vm = create()
        vm.onAction(EventEditAction.AllDayChanged(true))
        assertTrue(vm.uiState.value.isAllDay)
        // 시간 행을 눌러도 시트가 열리지 않는다
        vm.onAction(EventEditAction.TimeRowClicked(EditField.Start))
        assertNull(vm.uiState.value.sheet)
    }

    @Test
    fun `FR-CAL-02 AC2 종일을 껐다 켜도 마지막 시간 값을 유지한다`() = runTest {
        val vm = create()
        vm.onAction(EventEditAction.AllDayChanged(true))
        vm.onAction(EventEditAction.AllDayChanged(false))
        assertEquals(LocalTime.of(11, 0), vm.uiState.value.startTime)
    }

    @Test
    fun `FR-CAL-02 AC3 종료가 시작보다 이르면 저장하지 않고 오류를 보여 준다`() = runTest {
        val vm = create()
        vm.fill()
        vm.onAction(EventEditAction.TimeRowClicked(EditField.End))
        vm.onAction(EventEditAction.TimeConfirmed(LocalTime.of(10, 0)))
        assertTrue(vm.uiState.value.endError)
        assertFalse(vm.uiState.value.saveEnabled)
        vm.onAction(EventEditAction.SaveClicked)
        assertTrue(events.created.isEmpty())
    }

    @Test
    fun `FR-CAL-02 AC3 종료가 시작과 같으면 저장한다`() = runTest {
        val vm = create()
        vm.fill()
        vm.onAction(EventEditAction.TimeRowClicked(EditField.End))
        vm.onAction(EventEditAction.TimeConfirmed(LocalTime.of(11, 0)))
        assertFalse(vm.uiState.value.endError)
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals(1, events.created.size)
    }

    @Test
    fun `FR-CAL-02 AC1 종료 날짜를 바꿔 여러 날 일정을 만든다`() = runTest {
        val vm = create()
        vm.fill()
        vm.onAction(EventEditAction.DateRowClicked(EditField.End))
        vm.onAction(EventEditAction.DatePicked(LocalDate.of(2026, 10, 14)))
        vm.onAction(EventEditAction.DateConfirmed)
        assertEquals(LocalDate.of(2026, 10, 14), vm.uiState.value.endDate)
        assertNull(vm.uiState.value.sheet)
    }

    @Test
    fun `FR-CAL-01 AC6 1950년 이전과 2050년 이후 날짜는 고를 수 없다`() = runTest {
        val vm = create()
        vm.onAction(EventEditAction.DateRowClicked(EditField.Start))
        vm.onAction(EventEditAction.DatePicked(LocalDate.of(1949, 12, 31)))
        vm.onAction(EventEditAction.DatePicked(LocalDate.of(2051, 1, 1)))
        assertEquals(LocalDate.of(2026, 10, 12), (vm.uiState.value.sheet as EditSheet.DatePicker).selected)
        assertEquals(EventRules.MIN_DATE, LocalDate.of(1950, 1, 1))
    }

    // --- 대상 ---

    @Test
    fun `FR-CAL-02 AC4 대상은 기본이 나이고 상대는 고를 수 없다`() = runTest {
        val vm = create()
        assertFalse(vm.uiState.value.together)
        vm.fill()
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals(false, events.created.single().isTogether)
    }

    @Test
    fun `FR-CAL-02 AC4 함께를 고르면 isTogether true를 보낸다`() = runTest {
        val vm = create()
        vm.fill()
        vm.onAction(EventEditAction.TogetherSelected(true))
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals(true, events.created.single().isTogether)
    }

    @Test
    fun `FR-CAL-02 AC4 개인 공간에는 대상 선택이 없고 isTogether를 보내지 않는다`() = runTest {
        me.me.value = soloMe()
        val vm = create()
        assertFalse(vm.uiState.value.showTargetSelector)
        vm.onAction(EventEditAction.TogetherSelected(true))
        vm.fill()
        vm.onAction(EventEditAction.SaveClicked)
        val sent = events.created.single()
        assertNull(sent.isTogether)
        assertNull(sent.notifyPartner)
    }

    // --- 상대에게 알림 보내기 (FR-NOTI-04) ---

    @Test
    fun `FR-NOTI-04 커플 공간 새 일정은 상대에게 알림 보내기가 기본 켜짐이다`() = runTest {
        val vm = create()
        assertTrue(vm.uiState.value.notifyPartner)
        vm.fill()
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals(true, events.created.single().notifyPartner)
    }

    @Test
    fun `FR-NOTI-04 끄면 notifyPartner false를 보낸다`() = runTest {
        val vm = create()
        vm.fill()
        vm.onAction(EventEditAction.NotifyPartnerChanged(false))
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals(false, events.created.single().notifyPartner)
    }

    @Test
    fun `FR-NOTI-04 수정 화면에는 상대에게 알림 보내기가 없다`() = runTest {
        events.events.value = listOf(sampleEvent(title = "원래"))
        val vm = edit()
        assertTrue(vm.uiState.value.isEdit)
        vm.onAction(EventEditAction.TitleChanged("바꿈"))
        vm.onAction(EventEditAction.SaveClicked)
        // 수정은 PATCH이고 PATCH 본문에는 notifyPartner가 없다 (EventPatch에 필드 자체가 없다)
        assertEquals(1, events.patches.size)
        assertTrue(events.created.isEmpty())
    }

    // --- 글자 수 (NFR-LIMIT-01) ---

    @Test
    fun `NFR-LIMIT-01 제목은 50자까지만 받는다`() = runTest {
        val vm = create()
        vm.fill("가".repeat(60))
        assertEquals(50, vm.uiState.value.title.length)
    }

    @Test
    fun `NFR-LIMIT-01 일정 메모는 1000자까지만 받는다`() = runTest {
        val vm = create()
        vm.onAction(EventEditAction.NoteChanged("나".repeat(1200)))
        assertEquals(1000, vm.uiState.value.note.length)
    }

    @Test
    fun `D-110 이모지는 한 글자로 세어 제목 50자에서 막는다`() = runTest {
        val vm = create()
        vm.fill("😀".repeat(60))
        assertEquals(50, vm.uiState.value.title.codePointCount(0, vm.uiState.value.title.length))
    }

    @Test
    fun `D-110 공백이나 줄바꿈뿐인 제목은 저장 버튼이 꺼진다`() = runTest {
        val vm = create()
        vm.fill("  \n ")
        assertFalse(vm.uiState.value.saveEnabled)
        vm.onAction(EventEditAction.SaveClicked)
        assertTrue(events.created.isEmpty())
    }

    // --- 알림 ---

    @Test
    fun `FR-CAL-05 AC2 새 일정의 내 알림은 없음이다`() = runTest {
        assertNull(create().uiState.value.reminder)
    }

    @Test
    fun `FR-CAL-05 AC1 알림을 고르면 일정을 만든 뒤 PUT reminder를 보낸다`() = runTest {
        val vm = create()
        vm.fill()
        vm.onAction(EventEditAction.ReminderRowClicked)
        vm.onAction(EventEditAction.ReminderSelected(ReminderOption.MIN_10))
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals(listOf("new-1" to ReminderOption.MIN_10), events.reminderCalls)
    }

    @Test
    fun `FR-CAL-05 AC3 종일로 바꾸면 시간 일정용 알림 선택은 없음으로 돌아간다`() = runTest {
        val vm = create()
        vm.onAction(EventEditAction.ReminderSelected(ReminderOption.HOUR_1))
        vm.onAction(EventEditAction.AllDayChanged(true))
        assertNull(vm.uiState.value.reminder)
    }

    @Test
    fun `FR-CAL-05 AC3 종일 일정에는 시간 일정용 알림을 고를 수 없다`() = runTest {
        val vm = create()
        vm.onAction(EventEditAction.AllDayChanged(true))
        vm.onAction(EventEditAction.ReminderSelected(ReminderOption.MIN_10))
        assertNull(vm.uiState.value.reminder)
    }

    @Test
    fun `FR-CAL-05 AC4 수정에서 하루 종일을 바꾸면 내 알림이 없음으로 보인다`() = runTest {
        events.events.value = listOf(sampleEvent(reminder = ReminderOption.MIN_10))
        val vm = edit()
        assertEquals(ReminderOption.MIN_10, vm.uiState.value.reminder)
        vm.onAction(EventEditAction.AllDayChanged(true))
        assertNull(vm.uiState.value.reminder)
        vm.onAction(EventEditAction.SaveClicked)
        // 서버가 알림을 지우므로 없음 그대로면 따로 DELETE를 보내지 않는다
        assertTrue(events.reminderCalls.isEmpty())
    }

    @Test
    fun `알림 설정이 실패해도 일정은 남기고 오류 안내 뒤 닫는다`() = runTest {
        events.reminderResult = apiError("INTERNAL_ERROR")
        val vm = create()
        vm.fill()
        vm.onAction(EventEditAction.ReminderSelected(ReminderOption.MIN_10))
        vm.effects.test {
            vm.onAction(EventEditAction.SaveClicked)
            assertEquals(EventEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(EventEditSideEffect.Close, awaitItem())
        }
        assertEquals(1, events.created.size)
        assertEquals(1, events.events.value.size)
    }

    @Test
    fun `D-107 내 알림 행을 누르면 선택 시트가 열리고 고른 값이 반영되며 완료로 닫는다`() = runTest {
        val vm = create()
        vm.onAction(EventEditAction.ReminderRowClicked)
        assertEquals(EditSheet.Reminder, vm.uiState.value.sheet)
        vm.onAction(EventEditAction.ReminderSelected(ReminderOption.HOUR_1))
        assertEquals(ReminderOption.HOUR_1, vm.uiState.value.reminder)
        // 칩을 눌러도 시트는 열려 있다. [완료]가 닫는다
        assertEquals(EditSheet.Reminder, vm.uiState.value.sheet)
        vm.onAction(EventEditAction.SheetDismissed)
        assertNull(vm.uiState.value.sheet)
    }

    @Test
    fun `D-107 하루 종일을 켜고 끌 때마다 고른 알림은 없음으로 돌아간다`() = runTest {
        val vm = create()
        vm.onAction(EventEditAction.AllDayChanged(true))
        vm.onAction(EventEditAction.ReminderSelected(ReminderOption.ALLDAY_9AM))
        vm.onAction(EventEditAction.AllDayChanged(false))
        assertNull(vm.uiState.value.reminder)
    }

    @Test
    fun `D-108 일정 만들기가 실패하면 알림은 보내지 않고 화면에 남는다`() = runTest {
        events.createResult = apiError("INTERNAL_ERROR")
        val vm = create()
        vm.fill()
        vm.onAction(EventEditAction.ReminderSelected(ReminderOption.MIN_10))
        vm.onAction(EventEditAction.SaveClicked)
        assertTrue(events.reminderCalls.isEmpty())
        assertFalse(vm.uiState.value.saving)
    }

    @Test
    fun `D-108 수정은 일정을 저장한 뒤 알림을 바꾼다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val vm = edit()
        vm.onAction(EventEditAction.ReminderSelected(ReminderOption.MIN_10))
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals(1, events.patches.size)
        assertEquals(listOf("e1" to ReminderOption.MIN_10), events.reminderCalls)
    }

    @Test
    fun `D-108 수정에서 알림 변경만 실패해도 저장된 채 공통 오류를 보이고 닫는다`() = runTest {
        events.events.value = listOf(sampleEvent())
        events.reminderResult = apiError("INTERNAL_ERROR")
        val vm = edit()
        vm.onAction(EventEditAction.ReminderSelected(ReminderOption.MIN_10))
        vm.effects.test {
            vm.onAction(EventEditAction.SaveClicked)
            assertEquals(EventEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(EventEditSideEffect.Close, awaitItem())
        }
        assertEquals(1, events.patches.size)
    }

    @Test
    fun `D-111 수정 저장에서 일정이 없다고 하면 공통 오류를 보이고 화면을 닫는다`() = runTest {
        events.events.value = listOf(sampleEvent(title = "원래"))
        events.updateResult = apiError("NOT_FOUND")
        val vm = edit()
        vm.fill("바꿈")
        vm.effects.test {
            vm.onAction(EventEditAction.SaveClicked)
            assertEquals(EventEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(EventEditSideEffect.Close, awaitItem())
        }
    }

    @Test
    fun `D-121 수정 저장이 403이면 화면을 닫고 상세를 다시 불러온다`() = runTest {
        events.events.value = listOf(sampleEvent(title = "원래"))
        events.updateResult = apiError("NOT_AUTHOR")
        val vm = edit()
        vm.fill("바꿈")
        vm.effects.test {
            vm.onAction(EventEditAction.SaveClicked)
            assertEquals(EventEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(EventEditSideEffect.Close, awaitItem())
        }
        assertFalse(vm.uiState.value.saving)
        // 저장소가 403이면 그 일정을 GET /events/{id}로 다시 읽는다 (EventRepositoryTest가 확인). 가짜도 같은 계약이다
        assertEquals(listOf("e1"), events.refreshedIds)
    }

    @Test
    fun `D-115 일정 저장 중에는 loading이 true이고 끝나면 false다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        events.updateGate = gate
        events.events.value = listOf(sampleEvent(title = "원래"))
        val vm = edit()
        vm.fill("바꿈")
        assertFalse(vm.uiState.value.loading)
        vm.effects.test {
            vm.onAction(EventEditAction.SaveClicked)
            assertTrue(vm.uiState.value.loading)

            gate.complete(Unit)
            assertEquals(EventEditSideEffect.Close, awaitItem())
            // 닫기 전에 덮개는 이미 걷혔고, 닫히는 동안 입력은 계속 막혀 있다
            assertFalse(vm.uiState.value.loading)
            assertTrue(vm.uiState.value.saving)
        }
    }

    @Test
    fun `D-111 입력 오류도 공통 오류로 알린다`() = runTest {
        events.createResult = apiError("VALIDATION_FAILED")
        val vm = create()
        vm.fill()
        vm.effects.test {
            vm.onAction(EventEditAction.SaveClicked)
            assertEquals(EventEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            expectNoEvents()
        }
    }

    // --- 수정 ---

    @Test
    fun `FR-CAL-03 AC4 작성자는 대상을 나에서 함께로 바꿀 수 있다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val vm = edit()
        assertFalse(vm.uiState.value.targetReadOnly)
        vm.onAction(EventEditAction.TogetherSelected(true))
        vm.onAction(EventEditAction.SaveClicked)
        assertEquals(true, events.patches.single().second.isTogether)
    }

    @Test
    fun `FR-CAL-03 AC4 상대가 함께 일정을 고칠 때 대상은 읽기 전용이다`() = runTest {
        events.events.value = listOf(sampleEvent(authorId = PartnerId, isTogether = true))
        val vm = edit()
        assertTrue(vm.uiState.value.targetReadOnly)
        assertTrue(vm.uiState.value.together)
        vm.onAction(EventEditAction.TogetherSelected(false))
        assertTrue(vm.uiState.value.together)
    }

    @Test
    fun `FR-CAL-03 AC4 상대가 고칠 때는 isTogether를 보내지 않는다`() = runTest {
        events.events.value = listOf(sampleEvent(authorId = PartnerId, isTogether = true))
        val vm = edit()
        vm.onAction(EventEditAction.TitleChanged("새 제목"))
        vm.onAction(EventEditAction.SaveClicked)
        val patch = events.patches.single().second
        assertEquals("새 제목", patch.title)
        assertNull(patch.isTogether)
    }

    @Test
    fun `FR-CAL-03 수정은 바뀐 필드만 보낸다`() = runTest {
        events.events.value = listOf(sampleEvent(title = "원래", note = "메모"))
        val vm = edit()
        vm.onAction(EventEditAction.NoteChanged(""))
        vm.onAction(EventEditAction.SaveClicked)
        val patch = events.patches.single().second
        assertNull(patch.title)
        assertEquals(NoteChange.Clear, patch.note)
        assertNull(patch.time)
        assertNull(patch.isTogether)
    }

    @Test
    fun `FR-CAL-03 수정에서 바꾼 게 없으면 요청 본문이 비어 있다`() = runTest {
        events.events.value = listOf(sampleEvent())
        val vm = edit()
        vm.onAction(EventEditAction.SaveClicked)
        assertTrue(events.patches.single().second.isEmpty)
    }

    @Test
    fun `FR-CAL-03 수정 권한이 없는 일정은 수정 화면을 닫는다`() = runTest {
        events.events.value = listOf(sampleEvent(authorId = PartnerId, isTogether = false))
        val vm = edit()
        vm.effects.test {
            assertEquals(EventEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(EventEditSideEffect.Close, awaitItem())
        }
        assertFalse(vm.uiState.value.loaded)
    }

    // --- 오류 ---

    @Test
    fun `NFR-OFF-02 오프라인이면 일정 저장 요청을 보내지 않고 안내한다`() = runTest {
        events.createResult = Offline
        val vm = create()
        vm.fill()
        vm.effects.test {
            vm.onAction(EventEditAction.SaveClicked)
            assertEquals(EventEditSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
        // 화면은 그대로이고 입력도 남는다
        assertFalse(vm.uiState.value.saving)
        assertEquals("저녁 데이트", vm.uiState.value.title)
    }

    @Test
    fun `D-88 일정 저장이 서버 오류면 공통 오류를 보여 주고 화면은 그대로다`() = runTest {
        events.createResult = apiError("INTERNAL_ERROR")
        val vm = create()
        vm.fill()
        vm.effects.test {
            vm.onAction(EventEditAction.SaveClicked)
            assertEquals(EventEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
        }
        assertFalse(vm.uiState.value.saving)
        assertNotNull(vm.uiState.value.title)
        assertTrue(vm.uiState.value.saveEnabled)
    }

    @Test
    fun `FR-CAL-02 AC4 색상 변경 링크는 우리 색상 화면을 연다`() = runTest {
        val vm = create()
        vm.effects.test {
            vm.onAction(EventEditAction.ColorChangeClicked)
            assertEquals(EventEditSideEffect.OpenColors, awaitItem())
        }
    }

    @Test
    fun `FR-CAL-02 AC1 닫기는 저장하지 않고 닫는다`() = runTest {
        val vm = create()
        vm.fill()
        vm.effects.test {
            vm.onAction(EventEditAction.CloseClicked)
            assertEquals(EventEditSideEffect.Close, awaitItem())
        }
        assertTrue(events.created.isEmpty())
    }
}
