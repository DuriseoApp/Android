package com.duriseo.android.feature.settings.notification

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.notification.NotificationSettingKind
import com.duriseo.android.domain.notification.NotificationSettings
import com.duriseo.android.domain.notification.NotificationSettingsRepository
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class FakeNotificationSettingsRepository(initial: NotificationSettings? = null) : NotificationSettingsRepository {
    val settings = MutableStateFlow(initial)
    var refreshResult: DataResult<Unit> = DataResult.Success(Unit)
    var refreshCalls = 0
    var updateResult: DataResult<Unit>? = null
    var updateGate: CompletableDeferred<Unit>? = null
    val updates = mutableListOf<Pair<NotificationSettingKind, Boolean>>()

    override fun observe(): Flow<NotificationSettings?> = settings

    override suspend fun refresh(): DataResult<Unit> {
        refreshCalls++
        return refreshResult
    }

    override suspend fun update(kind: NotificationSettingKind, enabled: Boolean): DataResult<Unit> {
        updates += kind to enabled
        updateGate?.await()
        val result = updateResult ?: DataResult.Success(Unit)
        // 저장소처럼 성공했을 때만 기기 값을 바꾼다 (낙관적 갱신 없음)
        if (result is DataResult.Success) settings.value = settings.value?.with(kind, enabled)
        return result
    }
}

private val AllOn = NotificationSettings(true, true, true, true)
private val Failure500 = DataResult.Failure(DataError.ApiError("INTERNAL_ERROR", null, null))

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FR-NOTI-02 AC2 들어올 때마다 서버 값을 읽고 기본은 모두 켜짐이다`() = runTest {
        val repo = FakeNotificationSettingsRepository(AllOn)
        val vm = NotificationSettingsViewModel(repo)

        assertEquals(1, repo.refreshCalls)
        assertTrue(NotificationSettingKind.entries.all { vm.uiState.value.isOn(it) })
        assertTrue(vm.uiState.value.switchesEnabled)
    }

    @Test
    fun `FR-NOTI-02 AC2 스위치를 누르면 바뀐 종류만 저장하고 로딩 덮개를 보인다`() = runTest {
        val repo = FakeNotificationSettingsRepository(AllOn)
        val gate = CompletableDeferred<Unit>()
        repo.updateGate = gate
        val vm = NotificationSettingsViewModel(repo)

        vm.onAction(NotificationSettingsAction.Toggled(NotificationSettingKind.MEMO_PINNED))

        // 요청 중: 덮개가 보이고 낙관적 갱신은 없다 (D-115)
        assertTrue(vm.uiState.value.loading)
        assertTrue(vm.uiState.value.isOn(NotificationSettingKind.MEMO_PINNED))
        assertEquals(listOf(NotificationSettingKind.MEMO_PINNED to false), repo.updates)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.loading)
        assertFalse(vm.uiState.value.isOn(NotificationSettingKind.MEMO_PINNED))
        assertTrue(vm.uiState.value.isOn(NotificationSettingKind.EVENT_REMINDER))
    }

    @Test
    fun `FR-NOTI-02 AC2 스위치를 누르면 바뀐 종류만 저장하고 실패하면 원래 값이다`() = runTest {
        val repo = FakeNotificationSettingsRepository(AllOn)
        repo.updateResult = Failure500
        val vm = NotificationSettingsViewModel(repo)

        vm.effects.test {
            vm.onAction(NotificationSettingsAction.Toggled(NotificationSettingKind.ANNIVERSARY))

            assertEquals(NotificationSettingsSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
        }
        assertTrue(vm.uiState.value.isOn(NotificationSettingKind.ANNIVERSARY))
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `FR-NOTI-02 AC2 오프라인이면 인터넷 연결이 필요해요를 보이고 원래 값이다`() = runTest {
        val repo = FakeNotificationSettingsRepository(AllOn)
        repo.updateResult = DataResult.Failure(DataError.Offline)
        val vm = NotificationSettingsViewModel(repo)

        vm.effects.test {
            vm.onAction(NotificationSettingsAction.Toggled(NotificationSettingKind.EVENT_REMINDER))

            assertEquals(NotificationSettingsSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
        assertTrue(vm.uiState.value.isOn(NotificationSettingKind.EVENT_REMINDER))
    }

    @Test
    fun `D-145 한 번도 받지 못하면 스위치가 비활성이고 공통 오류를 보인다`() = runTest {
        val repo = FakeNotificationSettingsRepository(null)
        repo.refreshResult = Failure500

        val vm = NotificationSettingsViewModel(repo)

        assertNull(vm.uiState.value.settings)
        assertFalse(vm.uiState.value.switchesEnabled)
        assertFalse(vm.uiState.value.isOn(NotificationSettingKind.EVENT_REMINDER))
        vm.effects.test {
            assertEquals(NotificationSettingsSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
        }
        // 비활성이라 눌러도 요청하지 않는다
        vm.onAction(NotificationSettingsAction.Toggled(NotificationSettingKind.EVENT_REMINDER))
        assertTrue(repo.updates.isEmpty())
    }

    @Test
    fun `D-145 한 번도 받지 못한 채 오프라인이어도 공통 오류를 보인다`() = runTest {
        val repo = FakeNotificationSettingsRepository(null)
        repo.refreshResult = DataResult.Failure(DataError.Offline)

        val vm = NotificationSettingsViewModel(repo)

        vm.effects.test {
            assertEquals(NotificationSettingsSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
        }
    }

    @Test
    fun `D-145 저장된 값이 있으면 읽기가 실패해도 그 값을 보이고 안내하지 않는다`() = runTest {
        val repo = FakeNotificationSettingsRepository(NotificationSettings(true, false, true, true))
        repo.refreshResult = Failure500

        val vm = NotificationSettingsViewModel(repo)

        assertTrue(vm.uiState.value.switchesEnabled)
        assertFalse(vm.uiState.value.isOn(NotificationSettingKind.PARTNER_CREATED))
        vm.effects.test { expectNoEvents() }
    }

    @Test
    fun `SCR-SET-02 뒤로 가기를 누르면 닫는다`() = runTest {
        val vm = NotificationSettingsViewModel(FakeNotificationSettingsRepository(AllOn))

        vm.effects.test {
            vm.onAction(NotificationSettingsAction.BackClicked)
            assertEquals(NotificationSettingsSideEffect.Back, awaitItem())
        }
    }
}
