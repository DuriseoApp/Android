package com.duriseo.android.feature.memo.pinned

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.feature.memo.FakeMeRepository
import com.duriseo.android.feature.memo.FakeMemoRepository
import com.duriseo.android.feature.memo.Offline
import com.duriseo.android.feature.memo.TestClock
import com.duriseo.android.feature.memo.coupleMe
import com.duriseo.android.feature.memo.makeMemo
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinnedMemosViewModelTest {

    private val repo = FakeMemoRepository()
    private val me = FakeMeRepository(coupleMe())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PinnedMemosViewModel(repo, me, TestClock)

    @Test
    fun `FR-MEMO-05 AC3 고정 메모 전체는 서버 순서(최근 고정순) 그대로이고 앱바 개수는 길이다`() = runTest {
        repo.pinned.value = listOf(makeMemo("p3", pinned = true), makeMemo("p1", pinned = true), makeMemo("p2", pinned = true))

        val state = viewModel().uiState.value

        assertEquals(listOf("p3", "p1", "p2"), state.items.map { it.id })
        assertEquals(3, state.items.size)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `SCR-MEMO-02 고정이 없으면 비었다는 문구를 보인다`() = runTest {
        assertTrue(viewModel().uiState.value.isEmpty)
    }

    @Test
    fun `FR-MEMO-05 핀을 누르면 고정을 푼다`() = runTest {
        repo.pinned.value = listOf(makeMemo("p1", pinned = true))
        val vm = viewModel()

        vm.onAction(PinnedMemosAction.UnpinClicked(vm.uiState.value.items.single()))

        assertEquals(listOf("p1" to false), repo.pinCalls)
    }

    @Test
    fun `D-115 요청 중에는 loading이 true이고 끝나면 false다`() = runTest {
        repo.pinned.value = listOf(makeMemo("p1", pinned = true))
        val gate = CompletableDeferred<Unit>()
        repo.writeGate = gate
        val vm = viewModel()

        vm.onAction(PinnedMemosAction.UnpinClicked(vm.uiState.value.items.single()))
        assertTrue(vm.uiState.value.loading)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `D-115 저장소가 예외를 던져도 loading이 false로 돌아온다`() = runTest {
        repo.pinned.value = listOf(makeMemo("p1", pinned = true))
        repo.writeFailure = IllegalStateException("저장 실패")
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(PinnedMemosAction.UnpinClicked(vm.uiState.value.items.single()))
            assertEquals(PinnedMemosSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
        }
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 인터넷 연결이 필요하다는 안내를 보인다`() = runTest {
        repo.pinned.value = listOf(makeMemo("p1", pinned = true))
        repo.pinResult = Offline
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(PinnedMemosAction.UnpinClicked(vm.uiState.value.items.single()))
            assertEquals(PinnedMemosSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
        }
    }

    @Test
    fun `FR-MEMO-05 화면이 보이면 고정 목록을 다시 불러온다`() = runTest {
        val vm = viewModel()

        vm.onAction(PinnedMemosAction.Shown)

        assertEquals(1, repo.refreshPinnedCalls)
    }

    @Test
    fun `SCR-MEMO-02 메모를 누르면 상세로 가고 뒤로 가기는 닫는다`() = runTest {
        repo.pinned.value = listOf(makeMemo("p1", pinned = true))
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(PinnedMemosAction.ItemClicked(vm.uiState.value.items.single()))
            assertEquals(PinnedMemosSideEffect.OpenDetail("p1"), awaitItem())
            vm.onAction(PinnedMemosAction.BackClicked)
            assertEquals(PinnedMemosSideEffect.Back, awaitItem())
        }
    }
}
