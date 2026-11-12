package com.duriseo.android.feature.memo.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.duriseo.android.core.common.navigation.MemoRouteModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.feature.memo.FakeMeRepository
import com.duriseo.android.feature.memo.FakeMemoRepository
import com.duriseo.android.feature.memo.Offline
import com.duriseo.android.feature.memo.PartnerId
import com.duriseo.android.feature.memo.TestClock
import com.duriseo.android.feature.memo.apiError
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class MemoDetailViewModelTest {

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

    private fun viewModel(id: String = "m1") = MemoDetailViewModel(
        SavedStateHandle(mapOf(MemoRouteModel.Detail::memoId.name to id)),
        repo,
        me,
        TestClock,
    )

    private fun storeMine() {
        repo.memos.value = mapOf("m1" to makeMemo("m1", memoDate = LocalDate.of(2026, 11, 20)))
    }

    private fun storePartner() {
        repo.memos.value = mapOf("m1" to makeMemo("m1", authorId = PartnerId, pinned = true))
    }

    @Test
    fun `FR-MEMO-03 AC1 내 메모에는 수정과 삭제가 있다`() = runTest {
        storeMine()
        val vm = viewModel()

        val memo = vm.uiState.value.memo
        assertTrue(memo?.canEdit == true)
        assertEquals("민지", memo?.nickname)
        assertEquals(11, memo?.memoDate?.month)

        vm.effects.test {
            vm.onAction(MemoDetailAction.EditClicked)
            assertEquals(MemoDetailSideEffect.OpenEdit("m1"), awaitItem())
        }
        vm.onAction(MemoDetailAction.DeleteClicked)
        assertTrue(vm.uiState.value.deleteDialogVisible)
    }

    @Test
    fun `FR-MEMO-03 AC2 상대 메모에는 수정 삭제가 없다`() = runTest {
        storePartner()
        val vm = viewModel()

        val memo = vm.uiState.value.memo
        assertFalse(memo?.canEdit == true)
        assertEquals("지우", memo?.nickname)

        vm.effects.test {
            vm.onAction(MemoDetailAction.EditClicked)
            vm.onAction(MemoDetailAction.DeleteClicked)
            expectNoEvents()
        }
        assertFalse(vm.uiState.value.deleteDialogVisible)
        // 서버도 거부하므로 확인해도 요청하지 않는다
        vm.onAction(MemoDetailAction.DeleteConfirmed)
        assertTrue(repo.deleted.isEmpty())
    }

    @Test
    fun `FR-MEMO-03 작성일은 D-129 표기 계산 결과다`() = runTest {
        repo.memos.value = mapOf("m1" to makeMemo("m1", createdAt = java.time.Instant.parse("2026-10-11T23:40:00Z")))

        assertEquals(WrittenAt.Today(isPm = false, hour12 = 8, minute = 40), viewModel().uiState.value.memo?.writtenAt)
    }

    @Test
    fun `FR-MEMO-03 화면이 보이면 상세를 다시 불러온다`() = runTest {
        storeMine()

        viewModel().onAction(MemoDetailAction.Shown)

        assertEquals(listOf("m1"), repo.refreshMemoCalls)
    }

    @Test
    fun `D-121 404면 기기에서 지우고 닫는다`() = runTest {
        storeMine()
        repo.refreshMemoResult = apiError("NOT_FOUND")
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(MemoDetailAction.Shown)
            // 공통 오류를 보이고 상세를 닫는다. 한 번만 알린다
            assertEquals(MemoDetailSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(MemoDetailSideEffect.Close, awaitItem())
            expectNoEvents()
        }
        // 저장소가 기기에서 지웠다
        assertNull(repo.memos.value["m1"])
    }

    @Test
    fun `D-121 403이면 공통 오류를 보이고 다시 불러온다`() = runTest {
        storeMine()
        repo.deleteResult = apiError("NOT_AUTHOR")
        val vm = viewModel()
        vm.onAction(MemoDetailAction.DeleteClicked)

        vm.effects.test {
            vm.onAction(MemoDetailAction.DeleteConfirmed)
            assertEquals(MemoDetailSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            // 화면은 닫히지 않는다
            expectNoEvents()
        }
        assertEquals(1, repo.refreshAllCalls)
        assertEquals(listOf("m1"), repo.refreshMemoCalls)
        assertFalse(vm.uiState.value.deleteDialogVisible)
        assertFalse(vm.uiState.value.deleting)
        assertTrue(vm.uiState.value.memo != null)
    }

    @Test
    fun `D-121 삭제 중 404면 공통 오류를 보이고 닫는다`() = runTest {
        storeMine()
        repo.deleteResult = apiError("NOT_FOUND")
        val vm = viewModel()
        vm.onAction(MemoDetailAction.DeleteClicked)

        vm.effects.test {
            vm.onAction(MemoDetailAction.DeleteConfirmed)
            assertEquals(MemoDetailSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(MemoDetailSideEffect.Close, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `D-121 상대가 지워 사라지면 공통 오류를 보이고 닫는다`() = runTest {
        storeMine()
        val vm = viewModel()

        vm.effects.test {
            repo.memos.value = emptyMap()
            assertEquals(MemoDetailSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(MemoDetailSideEffect.Close, awaitItem())
        }
    }

    @Test
    fun `FR-MEMO-03 AC1 삭제를 확인하면 삭제하고 닫는다`() = runTest {
        storeMine()
        val vm = viewModel()
        vm.onAction(MemoDetailAction.DeleteClicked)

        vm.effects.test {
            vm.onAction(MemoDetailAction.DeleteConfirmed)
            assertEquals(MemoDetailSideEffect.Close, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf("m1"), repo.deleted)
    }

    @Test
    fun `FR-MEMO-03 삭제 확인에서 취소하면 닫기만 한다`() = runTest {
        storeMine()
        val vm = viewModel()
        vm.onAction(MemoDetailAction.DeleteClicked)

        vm.onAction(MemoDetailAction.DeleteCancelled)

        assertFalse(vm.uiState.value.deleteDialogVisible)
        assertTrue(repo.deleted.isEmpty())
    }

    @Test
    fun `FR-MEMO-05 AC1 상대 메모도 고정하고 해제할 수 있다`() = runTest {
        storePartner()
        val vm = viewModel()

        // 고정된 상태라 누르면 해제한다
        vm.onAction(MemoDetailAction.PinClicked)

        assertEquals(listOf("m1" to false), repo.pinCalls)
    }

    @Test
    fun `FR-MEMO-06 별을 누르면 즐겨찾기를 켠다`() = runTest {
        storePartner()
        val vm = viewModel()

        vm.onAction(MemoDetailAction.FavoriteClicked)

        assertEquals(listOf("m1" to true), repo.favoriteCalls)
    }

    @Test
    fun `D-115 요청 중에는 loading이 true이고 끝나면 false다`() = runTest {
        storeMine()
        val gate = CompletableDeferred<Unit>()
        repo.writeGate = gate
        val vm = viewModel()

        vm.onAction(MemoDetailAction.FavoriteClicked)
        assertTrue(vm.uiState.value.loading)
        gate.complete(Unit)
        assertFalse(vm.uiState.value.loading)

        // 삭제도 같다
        val gate2 = CompletableDeferred<Unit>()
        repo.writeGate = gate2
        vm.onAction(MemoDetailAction.DeleteClicked)
        vm.onAction(MemoDetailAction.DeleteConfirmed)
        assertTrue(vm.uiState.value.loading)
        gate2.complete(Unit)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 인터넷 연결이 필요하다는 안내를 보이고 화면에 남는다`() = runTest {
        storeMine()
        repo.pinResult = Offline
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(MemoDetailAction.PinClicked)
            assertEquals(MemoDetailSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            expectNoEvents()
        }
        assertFalse(vm.uiState.value.loading)
    }
}
