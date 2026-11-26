package com.duriseo.android.feature.appops

import android.app.Activity
import app.cash.turbine.test
import coil3.ImageLoader
import com.duriseo.android.core.common.update.AppForegroundEvents
import com.duriseo.android.core.common.update.AppUpdateAvailability
import com.duriseo.android.core.common.update.AppUpdatePhase
import com.duriseo.android.core.common.update.AppUpdateSource
import com.duriseo.android.core.common.update.ImmediateUpdateResult
import com.duriseo.android.domain.appops.AppEnvironment
import com.duriseo.android.domain.appops.AppNotice
import com.duriseo.android.domain.appops.AppOpsData
import com.duriseo.android.domain.appops.AppOpsRepository
import com.duriseo.android.domain.appops.AppOpsSnapshot
import com.duriseo.android.domain.appops.AppPlatform
import com.duriseo.android.domain.appops.AppPopupCoordinator
import com.duriseo.android.domain.appops.AppUpdateRule
import com.duriseo.android.domain.appops.BuildCondition
import com.duriseo.android.domain.appops.ForceUpdateSignal
import com.duriseo.android.domain.appops.ForceUpdateSignals
import com.duriseo.android.domain.appops.HiddenTodayIds
import com.duriseo.android.domain.appops.PopupHiddenTodayStore
import com.duriseo.android.domain.appops.PopupIdSpace
import com.duriseo.android.domain.appops.PopupImage
import com.duriseo.android.domain.appops.PopupImagePrefetcher
import com.duriseo.android.domain.appops.PopupKind
import com.duriseo.android.domain.appops.PopupLink
import com.duriseo.android.domain.appops.PopupPreset
import com.duriseo.android.domain.appops.UpdateMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private class FakeAppOps(var snapshot: AppOpsSnapshot) : AppOpsRepository {
    var checks = 0

    /** 설정하면 완료될 때까지 [check]가 끝나지 않는다 (서버를 기다리는 중) */
    var gate: CompletableDeferred<Unit>? = null
    override suspend fun check(): AppOpsSnapshot {
        checks++
        gate?.await()
        return snapshot
    }

    val changeFlow = MutableSharedFlow<AppOpsSnapshot>(extraBufferCapacity = 4)
    override val changes: Flow<AppOpsSnapshot> = changeFlow
}

private class FakeSignals : ForceUpdateSignals {
    val state = MutableStateFlow<ForceUpdateSignal?>(null)
    override val current: StateFlow<ForceUpdateSignal?> = state
    override fun clear() {
        state.value = null
    }
}

private class FakeHidden : PopupHiddenTodayStore {
    val saved = mutableListOf<Pair<PopupIdSpace, String>>()
    override suspend fun hiddenIds(today: LocalDate): HiddenTodayIds = HiddenTodayIds(
        notices = saved.filter { it.first == PopupIdSpace.NOTICE }.map { it.second }.toSet(),
        rules = saved.filter { it.first == PopupIdSpace.UPDATE_RULE }.map { it.second }.toSet(),
    )

    override suspend fun hideToday(space: PopupIdSpace, id: String, today: LocalDate) {
        saved += space to id
    }
}

private class FakeForeground : AppForegroundEvents {
    val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val returns = flow
}

private class FakeUpdateSource : AppUpdateSource {
    override val phase: StateFlow<AppUpdatePhase> = MutableStateFlow(AppUpdatePhase.IDLE)
    var result = ImmediateUpdateResult.UNAVAILABLE
    var calls = 0
    override suspend fun check(): AppUpdateAvailability = AppUpdateAvailability.UNKNOWN
    override suspend fun startUpdate(activity: Activity): Boolean = false
    override suspend fun startImmediateUpdate(activity: Activity): ImmediateUpdateResult {
        calls++
        return result
    }

    override fun start() = Unit
    override fun completeIfDownloaded() = Unit
}

private class FakePrefetch : PopupImagePrefetcher {
    /** 설정하면 완료될 때까지 이미지를 받는 중이다 */
    var gate: CompletableDeferred<Unit>? = null
    override suspend fun prefetch(url: String): Boolean {
        gate?.await()
        return true
    }
}

private fun notice(id: String, closable: Boolean = true) = AppNotice(
    id = id, closable = closable, title = "제목 $id", body = "본문", image = PopupImage.None, imageAlt = null,
    link = null, platform = null, startsAt = null, endsAt = null,
)

private fun bannerNotice(id: String) = notice(id).copy(image = PopupImage.Web("https://example.com/$id.png"))

private fun forceRule(id: String) = AppUpdateRule(
    id = id, mode = UpdateMode.FORCE, platform = AppPlatform.ANDROID, build = BuildCondition.Lte(10), os = null,
    title = "업데이트", body = null, image = PopupImage.Preset(PopupPreset.UPDATE), imageAlt = null,
)

private fun snapshot(vararg notices: AppNotice, rules: List<AppUpdateRule> = emptyList()) =
    AppOpsSnapshot(remote = AppOpsData(notices.toList(), rules), server = null)

/** SCR-APP-01·02 팝업의 확인 시점과 버튼 동작 (D-178, D-179, Q-21, FR-APP-01·02). [Activity]가 필요해서 Robolectric으로 돈다 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppPopupViewModelTest {
    private val signals = FakeSignals()
    private val hidden = FakeHidden()
    private val foreground = FakeForeground()
    private val update = FakeUpdateSource()
    private val prefetch = FakePrefetch()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: FakeAppOps): AppPopupViewModel {
        val coordinator = AppPopupCoordinator(
            repository = repo,
            hiddenStore = hidden,
            forceSignals = signals,
            prefetcher = prefetch,
            environment = AppEnvironment(AppPlatform.ANDROID, build = 10, osVersion = 34),
            clock = Clock.fixed(Instant.parse("2026-11-25T03:00:00Z"), ZoneId.of("Asia/Seoul")),
        )
        val loader = ImageLoader.Builder(RuntimeEnvironment.getApplication()).build()
        return AppPopupViewModel(coordinator, update, foreground, loader)
    }

    private fun AppPopupViewModel.shownId() = uiState.value.popup?.id

    @Test
    fun `D-179 닫을 수 없는 팝업은 뒤로 가기를 무시한다`() = runTest {
        val vm = viewModel(FakeAppOps(snapshot(notice("block", closable = false))))

        vm.onAction(AppPopupAction.Back)

        assertEquals("block", vm.shownId())
        assertEquals(PopupKind.BLOCK_NOTICE, vm.uiState.value.popup?.kind)
        assertTrue(hidden.saved.isEmpty())
    }

    @Test
    fun `Q-21 뒤로 가기는 확인과 같고 오늘 하루 안 보기를 기억하지 않는다`() = runTest {
        val vm = viewModel(FakeAppOps(snapshot(notice("a"), notice("b"))))

        vm.onAction(AppPopupAction.Back)

        assertEquals("b", vm.shownId())
        assertTrue(hidden.saved.isEmpty())
    }

    @Test
    fun `D-178 닫으면 다음 팝업을 보인다`() = runTest {
        val vm = viewModel(FakeAppOps(snapshot(notice("a"), notice("b"))))
        assertEquals("a", vm.shownId())

        vm.onAction(AppPopupAction.Dismiss)
        assertEquals("b", vm.shownId())

        // 오늘 하루 안 보기는 기억한다
        vm.onAction(AppPopupAction.HideToday)
        assertNull(vm.shownId())
        assertEquals(listOf(PopupIdSpace.NOTICE to "b"), hidden.saved)
    }

    @Test
    fun `D-178 앞 순서 팝업이 생기면 바꾼다`() = runTest {
        val repo = FakeAppOps(snapshot(notice("info")))
        val vm = viewModel(repo)
        assertEquals("info", vm.shownId())

        repo.changeFlow.tryEmit(snapshot(notice("info"), notice("block", closable = false)))

        assertEquals("block", vm.shownId())
    }

    @Test
    fun `FR-APP-01 AC2 공지가 내려가면 닫는다`() = runTest {
        val repo = FakeAppOps(snapshot(notice("block", closable = false)))
        val vm = viewModel(repo)
        assertNotNull(vm.uiState.value.popup)

        repo.changeFlow.tryEmit(snapshot())

        assertNull(vm.uiState.value.popup)
    }

    @Test
    fun `D-178 포그라운드 복귀 때 다시 확인한다`() = runTest {
        val repo = FakeAppOps(snapshot())
        val vm = viewModel(repo)
        assertNull(vm.shownId())
        assertEquals(1, repo.checks)

        repo.snapshot = snapshot(notice("new"))
        foreground.flow.tryEmit(Unit)

        assertEquals(2, repo.checks)
        assertEquals("new", vm.shownId())
    }

    @Test
    fun `D-178 Remote Config가 바뀌면 다시 확인한다`() = runTest {
        val repo = FakeAppOps(snapshot())
        val vm = viewModel(repo)

        repo.changeFlow.tryEmit(snapshot(notice("from-rc")))

        assertEquals("from-rc", vm.shownId())
        // 서버를 다시 부르지 않고 바뀐 값으로 판단한다
        assertEquals(1, repo.checks)
    }

    @Test
    fun `FR-APP-02 AC6 426 신호가 오면 강제 팝업을 바로 띄운다`() = runTest {
        val vm = viewModel(FakeAppOps(snapshot(notice("info"))))
        assertEquals(PopupKind.INFO_NOTICE, vm.uiState.value.popup?.kind)

        signals.state.value = ForceUpdateSignal("rule-9", "필수 업데이트", null, PopupImage.Preset(PopupPreset.UPDATE), null)

        val popup = vm.uiState.value.popup
        assertEquals(PopupKind.FORCE_UPDATE, popup?.kind)
        assertEquals("필수 업데이트", popup?.title)
    }

    @Test
    fun `FR-APP-02 AC2 즉시 업데이트를 쓸 수 없으면 스토어를 연다`() = runTest {
        val vm = viewModel(FakeAppOps(snapshot(rules = listOf(forceRule("force")))))
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        update.result = ImmediateUpdateResult.UNAVAILABLE

        vm.sideEffects.test {
            vm.onAction(AppPopupAction.Update(activity))
            assertEquals(
                AppPopupSideEffect.OpenStore(PlayStoreLinks.MARKET_URI, PlayStoreLinks.WEB_URL),
                awaitItem(),
            )
        }
        assertEquals(1, update.calls)
    }

    @Test
    fun `FR-APP-02 AC2 업데이트를 취소하면 스토어를 열지 않고 팝업이 그대로 보인다`() = runTest {
        val vm = viewModel(FakeAppOps(snapshot(rules = listOf(forceRule("force")))))
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        update.result = ImmediateUpdateResult.CANCELED

        vm.sideEffects.test {
            vm.onAction(AppPopupAction.Update(activity))
            expectNoEvents()
        }
        assertEquals("force", vm.shownId())
    }

    @Test
    fun `D-179 앱 종료를 누르면 종료 효과를 낸다`() = runTest {
        val vm = viewModel(FakeAppOps(snapshot(notice("block", closable = false))))

        vm.sideEffects.test {
            vm.onAction(AppPopupAction.Exit)
            assertEquals(AppPopupSideEffect.FinishApp, awaitItem())
        }
        // 팝업은 그대로다 (다음 실행 때 다시 확인한다)
        assertEquals("block", vm.shownId())
    }

    @Test
    fun `D-178 이미지를 받는 중에 426이 오면 강제 팝업이 남는다`() = runTest {
        prefetch.gate = CompletableDeferred()
        val vm = viewModel(FakeAppOps(snapshot(bannerNotice("info"))))
        assertNull(vm.uiState.value.popup) // 이미지를 받는 동안은 뜨지 않는다

        signals.state.value = ForceUpdateSignal("rule-9", "필수 업데이트", null, PopupImage.Preset(PopupPreset.UPDATE), null)
        assertEquals(PopupKind.FORCE_UPDATE, vm.uiState.value.popup?.kind)

        prefetch.gate?.complete(Unit) // 늦게 끝난 확인(알리는 공지)이 도착한다
        assertEquals(PopupKind.FORCE_UPDATE, vm.uiState.value.popup?.kind)
    }

    @Test
    fun `D-178 늦게 끝난 확인이 앞 순서 팝업을 덮지 않는다`() = runTest {
        val repo = FakeAppOps(snapshot(notice("info")))
        repo.gate = CompletableDeferred()
        val vm = viewModel(repo)
        assertNull(vm.shownId())

        repo.changeFlow.tryEmit(snapshot(notice("block", closable = false)))
        assertEquals("block", vm.shownId())

        repo.gate?.complete(Unit)
        assertEquals("block", vm.shownId())
    }

    @Test
    fun `D-179 확인을 누르면 다음 팝업 이미지를 받는 동안에도 지금 팝업이 먼저 내려간다`() = runTest {
        val vm = viewModel(FakeAppOps(snapshot(notice("a"), bannerNotice("b"))))
        assertEquals("a", vm.shownId())
        prefetch.gate = CompletableDeferred()

        vm.onAction(AppPopupAction.Dismiss)
        assertNull(vm.shownId())
        // 닫는 중에 다시 눌러도 아무 일이 없다
        vm.onAction(AppPopupAction.Dismiss)

        prefetch.gate?.complete(Unit)
        assertEquals("b", vm.shownId())
    }

    @Test
    fun `결정 17 링크로 나갔다 돌아오는 한 번은 돌아옴 확인으로 세지 않고 팝업이 남는다`() = runTest {
        val repo = FakeAppOps(snapshot(notice("a").copy(link = PopupLink("자세히", "https://example.com"))))
        val vm = viewModel(repo)
        assertEquals("a", vm.shownId())
        assertEquals(1, repo.checks)

        vm.onAction(AppPopupAction.OpenLink)
        foreground.flow.tryEmit(Unit)

        assertEquals(1, repo.checks)
        assertEquals("a", vm.shownId())

        // 그다음 돌아옴부터는 다시 확인한다
        foreground.flow.tryEmit(Unit)
        assertEquals(2, repo.checks)
    }
}
