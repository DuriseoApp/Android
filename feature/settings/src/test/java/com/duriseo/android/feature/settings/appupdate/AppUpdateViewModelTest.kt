package com.duriseo.android.feature.settings.appupdate

import android.app.Activity
import com.duriseo.android.core.common.update.AppUpdateAvailability
import com.duriseo.android.core.common.update.AppUpdatePhase
import com.duriseo.android.core.common.update.AppUpdateSource
import com.duriseo.android.core.common.update.AppVersion
import com.duriseo.android.domain.network.NetworkMonitor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeAppUpdateSource : AppUpdateSource {
    val phaseFlow = MutableStateFlow(AppUpdatePhase.IDLE)
    override val phase: StateFlow<AppUpdatePhase> = phaseFlow

    /** 완료시키기 전에는 확인 중이다 */
    var checkResult = CompletableDeferred(AppUpdateAvailability.UP_TO_DATE)
    var checkCalls = 0
    override suspend fun check(): AppUpdateAvailability {
        checkCalls++
        return checkResult.await()
    }

    var startResult = true
    var startCalls = 0
    override suspend fun startUpdate(activity: Activity): Boolean {
        startCalls++
        return startResult
    }

    override fun start() = Unit
    override fun completeIfDownloaded() = Unit
}

private class FakeNetworkMonitor(var online: Boolean) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = flowOf(online)
    override fun isOnlineNow(): Boolean = online
}

/** `startUpdate`에 넘길 Activity가 필요해서 Robolectric으로 돈다 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppUpdateViewModelTest {

    private val source = FakeAppUpdateSource()
    private val network = FakeNetworkMonitor(online = true)
    private val version = AppVersion(name = "1.0.0", code = 100)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): AppUpdateViewModel {
        val vm = AppUpdateViewModel(source, network, version)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    @Test
    fun `FR-SET-02 AC4 확인 중에는 최신 여부 줄을 비운다`() = runTest {
        source.checkResult = CompletableDeferred() // 끝나지 않음

        val state = viewModel().uiState.value

        assertEquals(AppUpdateStatus.CHECKING, state.status)
        assertFalse(state.latestLineVisible)
        // 줄은 안 보이지만 자리를 차지해 카드 높이를 유지한다 (D-151)
        assertTrue(state.latestLineReserved)
        assertFalse(state.updateButtonVisible)
        assertEquals("1.0.0", state.versionName)
        assertEquals(100L, state.versionCode)
    }

    @Test
    fun `FR-SET-02 AC4 업데이트 확인이 10초 안에 끝나지 않으면 확인할 수 없음이다`() = runTest {
        source.checkResult = CompletableDeferred() // Play가 답하지 않음
        val vm = viewModel()
        assertEquals(AppUpdateStatus.CHECKING, vm.uiState.value.status)

        // 가상 시간: 10초 직전까지는 계속 확인 중
        testScheduler.advanceTimeBy(9_999)
        testScheduler.runCurrent()
        assertEquals(AppUpdateStatus.CHECKING, vm.uiState.value.status)

        testScheduler.advanceTimeBy(2)
        testScheduler.runCurrent()
        assertEquals(AppUpdateStatus.UNKNOWN, vm.uiState.value.status)
        assertFalse(vm.uiState.value.latestLineReserved)
    }

    @Test
    fun `FR-SET-02 AC4 최신이면 최신 버전 안내를 보인다`() = runTest {
        source.checkResult = CompletableDeferred(AppUpdateAvailability.UP_TO_DATE)

        val state = viewModel().uiState.value

        assertTrue(state.latestLineVisible)
        assertFalse(state.updateButtonVisible)
    }

    @Test
    fun `FR-SET-02 AC4 새 버전이 있으면 업데이트 버튼을 보인다`() = runTest {
        source.checkResult = CompletableDeferred(AppUpdateAvailability.AVAILABLE)

        val state = viewModel().uiState.value

        assertEquals(AppUpdateStatus.UPDATE_AVAILABLE, state.status)
        assertTrue(state.updateButtonVisible)
        assertFalse(state.latestLineVisible)
    }

    @Test
    fun `FR-SET-02 AC4 확인할 수 없으면 버전만 보인다`() = runTest {
        source.checkResult = CompletableDeferred(AppUpdateAvailability.UNKNOWN)

        val state = viewModel().uiState.value

        assertEquals(AppUpdateStatus.UNKNOWN, state.status)
        // 최신 여부 줄과 [업데이트]는 숨기고 자리도 비운다 (D-151)
        assertFalse(state.latestLineVisible)
        assertFalse(state.latestLineReserved)
        assertFalse(state.updateButtonVisible)
        assertEquals("1.0.0", state.versionName)
    }

    @Test
    fun `FR-SET-02 AC4 오프라인이면 Play에 묻지 않고 확인할 수 없음이다`() = runTest {
        network.online = false

        val state = viewModel().uiState.value

        assertEquals(AppUpdateStatus.UNKNOWN, state.status)
        assertEquals(0, source.checkCalls)
    }

    @Test
    fun `FR-SET-02 AC4 업데이트를 누르면 받는 동안 확인 중과 같은 모양이다`() = runTest {
        source.checkResult = CompletableDeferred(AppUpdateAvailability.AVAILABLE)
        val vm = viewModel()
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        vm.onUpdateClicked(activity)
        assertEquals(1, source.startCalls)
        // Play가 받기 시작했다고 알림
        source.phaseFlow.value = AppUpdatePhase.DOWNLOADING

        val state = vm.uiState.value
        assertEquals(AppUpdateStatus.DOWNLOADING, state.status)
        assertFalse(state.updateButtonVisible)
        assertTrue(state.latestLineReserved)
        assertFalse(state.latestLineVisible)
    }

    @Test
    fun `FR-SET-02 AC4 받기를 취소하면 업데이트 버튼이 다시 보인다`() = runTest {
        source.checkResult = CompletableDeferred(AppUpdateAvailability.AVAILABLE)
        source.startResult = false
        val vm = viewModel()

        vm.onUpdateClicked(Robolectric.buildActivity(Activity::class.java).get())

        assertEquals(AppUpdateStatus.UPDATE_AVAILABLE, vm.uiState.value.status)
    }

    @Test
    fun `FR-SET-02 AC4 앱 시작 때 이미 받아 둔 업데이트가 있으면 확인 중과 같은 모양이다`() = runTest {
        source.checkResult = CompletableDeferred(AppUpdateAvailability.AVAILABLE)
        source.phaseFlow.value = AppUpdatePhase.DOWNLOADED

        val state = viewModel().uiState.value

        assertEquals(AppUpdateStatus.DOWNLOADING, state.status)
        assertFalse(state.updateButtonVisible)
    }
}
