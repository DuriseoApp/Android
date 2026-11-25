package com.duriseo.android.domain.appops

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AppPopupCoordinatorTest {

    private val webImage = PopupImage.Web("https://example.com/banner.png")

    private class FakeRepository(
        var snapshot: AppOpsSnapshot,
        var delayMillis: Long = 0,
    ) : AppOpsRepository {
        override suspend fun check(): AppOpsSnapshot {
            delay(delayMillis)
            return snapshot
        }

        val changeFlow = MutableSharedFlow<AppOpsSnapshot>(extraBufferCapacity = 4)
        override val changes: Flow<AppOpsSnapshot> = changeFlow
    }

    private class FakeSignals(initial: ForceUpdateSignal? = null) : ForceUpdateSignals {
        private val state = MutableStateFlow(initial)
        override val current: StateFlow<ForceUpdateSignal?> = state
        override fun clear() {
            state.value = null
        }
    }

    private class FakeHidden : PopupHiddenTodayStore {
        val saved = mutableListOf<Triple<PopupIdSpace, String, LocalDate>>()
        override suspend fun hiddenIds(today: LocalDate): HiddenTodayIds = HiddenTodayIds(
            notices = saved.filter { it.first == PopupIdSpace.NOTICE && it.third == today }.map { it.second }.toSet(),
            rules = saved.filter { it.first == PopupIdSpace.UPDATE_RULE && it.third == today }.map { it.second }.toSet(),
        )

        override suspend fun hideToday(space: PopupIdSpace, id: String, today: LocalDate) {
            saved += Triple(space, id, today)
        }
    }

    private class FakePrefetcher(private val takesMillis: Long, private val ok: Boolean = true) : PopupImagePrefetcher {
        var calls = 0
        override suspend fun prefetch(url: String): Boolean {
            calls++
            delay(takesMillis)
            return ok
        }
    }

    private val clock: Clock = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))

    private fun TestScope.coordinator(
        repository: AppOpsRepository,
        prefetcher: PopupImagePrefetcher = FakePrefetcher(0),
        signals: ForceUpdateSignals = FakeSignals(),
        hidden: PopupHiddenTodayStore = FakeHidden(),
    ) = AppPopupCoordinator(repository, hidden, signals, prefetcher, ANDROID_34, clock, uptimeMillis = { testScheduler.currentTime })

    private fun snapshotWith(vararg notices: AppNotice) =
        AppOpsSnapshot(remote = AppOpsData(notices.toList(), emptyList()), server = null)

    @Test
    fun `Q-23 3초 안에 이미지를 못 받으면 이미지 없이 띄운다`() = runTest {
        // 실행 때: 서버를 1초 기다린 뒤 이미지는 남은 2초만 기다린다 (확인 시작부터 합쳐 3초)
        val repo = FakeRepository(snapshotWith(notice("n", image = webImage)), delayMillis = 1_000)
        val prefetcher = FakePrefetcher(takesMillis = 10_000)

        val popup = coordinator(repo, prefetcher).check(isLaunch = true)

        assertEquals(PopupImage.None, popup?.image)
        assertEquals("n", popup?.id)
        assertEquals(3_000L, testScheduler.currentTime)
    }

    @Test
    fun `Q-23 실행 때 서버를 기다린 시간을 뺀 안에 받으면 이미지와 함께 띄운다`() = runTest {
        val repo = FakeRepository(snapshotWith(notice("n", image = webImage)), delayMillis = 1_000)
        val prefetcher = FakePrefetcher(takesMillis = 1_500)

        val popup = coordinator(repo, prefetcher).check(isLaunch = true)

        assertEquals(webImage, popup?.image)
        assertEquals(2_500L, testScheduler.currentTime)
    }

    @Test
    fun `Q-23 실행이 아닐 때는 이미지만 3초 기다린다`() = runTest {
        val repo = FakeRepository(snapshotWith(notice("n", image = webImage)), delayMillis = 2_500)
        val prefetcher = FakePrefetcher(takesMillis = 2_900)

        val popup = coordinator(repo, prefetcher).check(isLaunch = false)

        assertEquals(webImage, popup?.image)
        assertEquals(5_400L, testScheduler.currentTime)
    }

    @Test
    fun `Q-23 받기에 실패하면 이미지 없이 띄우고 기본 이미지는 받지 않는다`() = runTest {
        val failing = FakePrefetcher(takesMillis = 0, ok = false)
        val popup = coordinator(FakeRepository(snapshotWith(notice("n", image = webImage))), failing).check(true)
        assertEquals(PopupImage.None, popup?.image)

        val preset = FakePrefetcher(takesMillis = 0)
        val withPreset = notice("p", image = PopupImage.Preset(PopupPreset.NOTICE))
        coordinator(FakeRepository(snapshotWith(withPreset)), preset).check(true)
        assertEquals(0, preset.calls)
    }

    @Test
    fun `D-179 오늘 하루 안 보기를 누르면 같은 날 다시 고르지 않는다`() = runTest {
        val hidden = FakeHidden()
        val c = coordinator(FakeRepository(snapshotWith(notice("a"), notice("b"))), hidden = hidden)

        val first = c.check(true)!!
        c.hideToday(first)

        assertEquals("a", first.id)
        assertEquals("b", c.check(true)?.id)
    }

    @Test
    fun `D-179 숨길 수 없는 종류는 기억하지 않는다`() = runTest {
        val hidden = FakeHidden()
        val c = coordinator(FakeRepository(snapshotWith(notice("a", closable = false))), hidden = hidden)

        c.hideToday(c.check(true)!!)

        assertTrue(hidden.saved.isEmpty())
    }

    @Test
    fun `unit-9 결정 2 426 강제 팝업은 서버 목록에 그 규칙이 없고 다른 강제도 없을 때만 닫는다`() = runTest {
        val signal = ForceUpdateSignal("old-rule", "제목", "본문", PopupImage.None, null)
        val signals = FakeSignals(signal)
        val noServer = AppOpsSnapshot(AppOpsData.Empty, server = null)
        val serverWithoutRule = AppOpsSnapshot(AppOpsData.Empty, server = AppOpsData.Empty)
        val repo = FakeRepository(noServer)
        val c = coordinator(repo, signals = signals)

        // 서버 값을 못 받았으면 유지
        assertEquals("old-rule", c.check(false)?.id)
        // 서버 목록에 같은 규칙이 있으면 유지
        repo.snapshot = AppOpsSnapshot(AppOpsData.Empty, AppOpsData(emptyList(), listOf(rule("old-rule", UpdateMode.FORCE))))
        assertEquals("old-rule", c.check(false)?.id)
        // 서버 목록을 받았는데 규칙도 다른 강제도 없으면 닫는다
        repo.snapshot = serverWithoutRule
        assertNull(c.check(false))
        assertNull(signals.current.value)
    }

    @Test
    fun `D-178 이번만 닫으면 다음 순서 팝업을 고르고 다음 확인 때 다시 고른다`() = runTest {
        val repo = FakeRepository(snapshotWith(notice("a"), notice("b")))
        val c = coordinator(repo)

        val first = c.check(true)!!
        c.dismissForNow(first)

        assertEquals("a", first.id)
        assertEquals("b", c.next()?.id)
        c.dismissForNow(c.next()!!)
        assertNull(c.next())
        // 돌아올 때 확인하면 닫았던 팝업도 다시 뜬다 (오늘 하루 안 보기와 다르다)
        assertEquals("a", c.check(false)?.id)
    }

    @Test
    fun `unit-9 결정 2 426 팝업은 지난 서버 값으로 닫지 않고 방금 받은 서버 목록으로만 닫는다`() = runTest {
        val signals = FakeSignals(ForceUpdateSignal("old-rule", "제목", null, PopupImage.None, null))
        val withRule = AppOpsData(emptyList(), listOf(rule("old-rule", UpdateMode.FORCE)))
        val repo = FakeRepository(AppOpsSnapshot(AppOpsData.Empty, withRule))
        val c = coordinator(repo, signals = signals)
        c.check(false)

        val shown = mutableListOf<AppPopup?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { c.changes.collect { shown += it } }
        // Remote Config가 바뀌었는데 서버 값(지난 것)에는 규칙이 없다: changes는 닫지 않는다
        repo.changeFlow.tryEmit(AppOpsSnapshot(AppOpsData.Empty, AppOpsData.Empty))

        assertEquals("old-rule", shown.single()?.id)
        assertEquals("old-rule", signals.current.value?.ruleId)
        assertTrue(c.hasForceSignal)
        // 방금 받은 서버 목록에 없으면 닫는다
        repo.snapshot = AppOpsSnapshot(AppOpsData.Empty, AppOpsData.Empty)
        assertNull(c.check(false))
        assertTrue(!c.hasForceSignal)
    }

    @Test
    fun `결정 18 서버를 못 받은 확인은 떠 있던 서버 공지를 그대로 둔다`() = runTest {
        val serverOnly = AppOpsData(listOf(notice("server-notice")), emptyList())
        val repo = FakeRepository(AppOpsSnapshot(AppOpsData.Empty, serverOnly))
        val c = coordinator(repo)
        val shown = c.check(true)!!
        assertEquals("server-notice", shown.id)

        // 돌아올 때 서버가 실패: 새로 고르면 Remote Config 값만이라 없지만, 떠 있던 팝업은 그대로
        repo.snapshot = AppOpsSnapshot(AppOpsData.Empty, null)
        assertEquals("server-notice", c.check(false, current = shown)?.id)
        // 현재 팝업을 알려 주지 않으면(새로 고름) Remote Config 값만으로 판단한다
        assertNull(c.check(false))
        // 서버 목록을 받았는데 없으면 내려간 것이다
        repo.snapshot = AppOpsSnapshot(AppOpsData.Empty, AppOpsData.Empty)
        assertNull(c.check(false, current = shown))
        // 서버를 못 받아도 Remote Config에 앞 순서 팝업이 생기면 바뀐다
        repo.snapshot = AppOpsSnapshot(AppOpsData(listOf(notice("rc-block", closable = false)), emptyList()), serverOnly)
        c.check(true)
        repo.snapshot = AppOpsSnapshot(AppOpsData(listOf(notice("rc-block", closable = false)), emptyList()), null)
        assertEquals("rc-block", c.check(false, current = shown)?.id)
    }
}
