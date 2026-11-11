package com.duriseo.android.feature.settings.connection

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.settings.FakeCoupleRepository
import com.duriseo.android.feature.settings.FakeMeRepository
import com.duriseo.android.feature.settings.apiError
import com.duriseo.android.feature.settings.coupleMe
import com.duriseo.android.feature.settings.memberMe
import com.duriseo.android.feature.settings.menu.SettingsSpace
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** SCR-SET-01 연결 정보 (FR-SET-01, FR-COUPLE-05, D-94, D-97, D-99) */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionInfoViewModelTest {

    private val me = FakeMeRepository()
    private val couple = FakeCoupleRepository()

    /** 오늘은 2026-10-31 (서울) */
    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-31T03:00:00Z"), ZoneId.of("Asia/Seoul"))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): ConnectionInfoViewModel {
        val vm = ConnectionInfoViewModel(me, couple, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    @Test
    fun `FR-COUPLE-05 AC3 연결 정보에 들어오면 내 정보를 서버에서 다시 읽는다`() = runTest {
        me.me.value = coupleMe()
        viewModel()

        assertEquals(1, me.refreshCalls)
    }

    @Test
    fun `FR-SET-01 연결 정보는 처음 만난 날과 함께한 지를 보여 준다`() = runTest {
        me.me.value = coupleMe(startDate = LocalDate.of(2026, 10, 12))
        val vm = viewModel()

        val space = vm.uiState.value.space as SettingsSpace.Couple

        assertEquals(LocalDate.of(2026, 10, 12), space.startDate)
        assertEquals(20L, space.dayNumber)
        assertEquals("지우", space.partner.name)
    }

    @Test
    fun `FR-SET-01 AC1 D-94 연결한 날은 연결한 시각의 기기 시간대 날짜다`() = runTest {
        // UTC 2026-10-12 15:30은 서울에서 10월 13일 00:30
        me.me.value = coupleMe(linkedAt = Instant.parse("2026-10-12T15:30:00Z"))
        val vm = viewModel()

        val space = vm.uiState.value.space as SettingsSpace.Couple

        assertEquals(LocalDate.of(2026, 10, 13), space.linkedDate)
    }

    @Test
    fun `FR-SET-01 AC1 D-94 서버가 연결한 시각을 주지 않으면 연결한 날 값이 없다`() = runTest {
        me.me.value = coupleMe(linkedAt = null)
        val vm = viewModel()

        assertNull((vm.uiState.value.space as SettingsSpace.Couple).linkedDate)
    }

    @Test
    fun `FR-COUPLE-04 AC5 D-99 처음 만난 날이 없으면 처음 만난 날과 함께한 지 값이 없다`() = runTest {
        me.me.value = coupleMe(startDate = null)
        val vm = viewModel()

        val space = vm.uiState.value.space as SettingsSpace.Couple

        assertNull(space.startDate)
        assertNull(space.dayNumber)
    }

    @Test
    fun `D-97 혼자 사용 중이면 연결 정보는 혼자 모양이다`() = runTest {
        me.me.value = memberMe()
        val vm = viewModel()

        assertTrue(vm.uiState.value.space is SettingsSpace.Alone)
    }

    @Test
    fun `FR-COUPLE-05 AC1 연결 해제를 누르면 확인 다이얼로그가 뜨고 상대 이름이 쓰인다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()

        vm.onAction(ConnectionInfoAction.UnlinkClicked)

        assertTrue(vm.uiState.value.unlinkDialogVisible)
        assertEquals("지우", (vm.uiState.value.space as SettingsSpace.Couple).partner.name)
        assertEquals(0, couple.unlinkCalls)
    }

    @Test
    fun `FR-COUPLE-05 AC1 취소하면 아무것도 보내지 않는다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()
        vm.onAction(ConnectionInfoAction.UnlinkClicked)

        vm.onAction(ConnectionInfoAction.UnlinkDismissed)

        assertFalse(vm.uiState.value.unlinkDialogVisible)
        assertEquals(0, couple.unlinkCalls)
    }

    @Test
    fun `FR-COUPLE-05 AC1 연결 해제를 확인하면 DELETE couple을 한 번 보내고 다이얼로그를 닫는다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()
        vm.onAction(ConnectionInfoAction.UnlinkClicked)

        vm.effects.test {
            vm.onAction(ConnectionInfoAction.UnlinkConfirmed)
            vm.onAction(ConnectionInfoAction.UnlinkConfirmed)

            // 이동은 저장소가 앱 초기화를 마치고 feature:main이 한다. 이 화면은 안내도 이동도 하지 않는다
            expectNoEvents()
        }
        assertEquals(1, couple.unlinkCalls)
        assertFalse(vm.uiState.value.unlinkDialogVisible)
        assertFalse(vm.uiState.value.requesting)
    }

    @Test
    fun `D-115 연결 정보는 요청 중에는 loading이 true이고 끝나면 false다`() = runTest {
        me.me.value = coupleMe()
        val gate = CompletableDeferred<Unit>()
        couple.unlinkGate = gate
        val vm = viewModel()
        vm.onAction(ConnectionInfoAction.UnlinkClicked)
        assertFalse(vm.uiState.value.loading)

        vm.onAction(ConnectionInfoAction.UnlinkConfirmed)
        assertTrue(vm.uiState.value.loading)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 연결 해제가 인터넷 연결 안내로 끝난다`() = runTest {
        me.me.value = coupleMe()
        couple.unlinkResult = DataResult.Failure(DataError.Offline)
        val vm = viewModel()
        vm.onAction(ConnectionInfoAction.UnlinkClicked)

        vm.effects.test {
            vm.onAction(ConnectionInfoAction.UnlinkConfirmed)

            assertEquals(ConnectionInfoSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
        assertFalse(vm.uiState.value.unlinkDialogVisible)
        assertTrue(vm.uiState.value.space is SettingsSpace.Couple)
    }

    @Test
    fun `FR-COUPLE-05 AC1 서버 오류면 공통 오류를 보여 주고 연결은 그대로다`() = runTest {
        me.me.value = coupleMe()
        couple.unlinkResult = apiError("INTERNAL_ERROR")
        val vm = viewModel()
        vm.onAction(ConnectionInfoAction.UnlinkClicked)

        vm.effects.test {
            vm.onAction(ConnectionInfoAction.UnlinkConfirmed)

            assertEquals(ConnectionInfoSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
        }
        assertTrue(vm.uiState.value.space is SettingsSpace.Couple)
    }

    @Test
    fun `FR-COUPLE-05 AC3 다이얼로그가 떠 있는 동안 상대가 먼저 해제하면 다이얼로그가 사라진다`() = runTest {
        me.me.value = coupleMe()
        val vm = viewModel()
        vm.onAction(ConnectionInfoAction.UnlinkClicked)

        me.me.value = memberMe()

        assertFalse(vm.uiState.value.unlinkDialogVisible)
        assertTrue(vm.uiState.value.space is SettingsSpace.Alone)
    }

    @Test
    fun `FR-COUPLE-05 AC1 혼자일 때는 연결 해제를 보내지 않는다`() = runTest {
        me.me.value = memberMe()
        val vm = viewModel()

        vm.onAction(ConnectionInfoAction.UnlinkClicked)
        vm.onAction(ConnectionInfoAction.UnlinkConfirmed)

        assertEquals(0, couple.unlinkCalls)
    }
}
