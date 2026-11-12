package com.duriseo.android.feature.memo.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.duriseo.android.core.common.navigation.MemoRouteModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.memo.FieldChange
import com.duriseo.android.domain.memo.MemoRules
import com.duriseo.android.feature.memo.FakeMeRepository
import com.duriseo.android.feature.memo.FakeMemoRepository
import com.duriseo.android.feature.memo.Offline
import com.duriseo.android.feature.memo.PartnerId
import com.duriseo.android.feature.memo.TestClock
import com.duriseo.android.feature.memo.apiError
import com.duriseo.android.feature.memo.coupleMe
import com.duriseo.android.feature.memo.makeMemo
import com.duriseo.android.feature.memo.soloMe
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
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class MemoEditViewModelTest {

    private val repo = FakeMemoRepository()
    private val me = FakeMeRepository(coupleMe())

    /** 2026-10-12(월) 서울 */
    private val today = LocalDate.of(2026, 10, 12)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 새 메모: 메모 탭의 FAB이나 SCR-CAL-03의 [메모] 탭 (메모 id 키가 없다) */
    private fun create(args: Map<String, Any?> = emptyMap()) =
        MemoEditViewModel(SavedStateHandle(args), repo, me, TestClock)

    private fun edit(id: String = "m1") =
        MemoEditViewModel(SavedStateHandle(mapOf(MemoRouteModel.Edit::memoId.name to id)), repo, me, TestClock)

    private fun storeMine() {
        repo.memos.value = mapOf(
            "m1" to makeMemo("m1", title = "선물 아이디어", body = "향수\n머플러", memoDate = LocalDate.of(2026, 11, 20)),
        )
    }

    @Test
    fun `ARCH 메모 id 키 이름은 내비게이션 경로의 memoId와 같다`() {
        assertEquals(MemoRouteModel.Edit::memoId.name, MemoEditViewModel.ARG_MEMO_ID)
    }

    // --- 새로 쓰기 ---

    @Test
    fun `FR-MEMO-02 AC3 새 메모는 날짜가 비어 있고 날짜 추가로 고른 뒤 빼면 다시 비어 있다`() = runTest {
        val vm = create()
        assertTrue(vm.uiState.value.loaded)
        assertFalse(vm.uiState.value.isEdit)
        assertNull(vm.uiState.value.memoDate)

        vm.onAction(MemoEditAction.DateAddClicked)
        // 시트는 오늘 달에서 열리지만 날짜는 [완료]를 눌러야 붙는다
        assertEquals(YearMonth.of(2026, 10), vm.uiState.value.dateSheet?.visibleMonth)
        assertNull(vm.uiState.value.memoDate)
        vm.onAction(MemoEditAction.DatePicked(LocalDate.of(2026, 10, 17)))
        vm.onAction(MemoEditAction.DateConfirmed)
        assertEquals(LocalDate.of(2026, 10, 17), vm.uiState.value.memoDate)
        assertNull(vm.uiState.value.dateSheet)

        vm.onAction(MemoEditAction.DateRemoved)
        assertNull(vm.uiState.value.memoDate)
    }

    @Test
    fun `FR-MEMO-02 AC3 날짜 시트를 닫으면 날짜는 그대로다`() = runTest {
        val vm = create()
        vm.onAction(MemoEditAction.DateAddClicked)
        vm.onAction(MemoEditAction.DatePicked(LocalDate.of(2026, 10, 17)))

        vm.onAction(MemoEditAction.DateSheetDismissed)

        assertNull(vm.uiState.value.dateSheet)
        assertNull(vm.uiState.value.memoDate)
    }

    @Test
    fun `D-74 메모 날짜는 1950-01-01부터 2050-12-31까지만 고른다`() = runTest {
        val vm = create()
        vm.onAction(MemoEditAction.DateAddClicked)

        vm.onAction(MemoEditAction.DatePicked(LocalDate.of(2051, 1, 1)))
        assertEquals(today, vm.uiState.value.dateSheet?.selected)
        vm.onAction(MemoEditAction.DatePicked(LocalDate.of(1949, 12, 31)))
        assertEquals(today, vm.uiState.value.dateSheet?.selected)
        vm.onAction(MemoEditAction.DatePicked(MemoRules.MAX_DATE))
        assertEquals(MemoRules.MAX_DATE, vm.uiState.value.dateSheet?.selected)
    }

    @Test
    fun `D-133 캘린더에서 열어도 메모 날짜는 비어 있다`() = runTest {
        // SCR-CAL-03 항목에는 선택한 날(date)과 일정 id 키가 있지만 메모 id 키는 없다
        val vm = create(mapOf("eventId" to null, "date" to "2026-10-17"))

        assertFalse(vm.uiState.value.isEdit)
        assertNull(vm.uiState.value.memoDate)
    }

    @Test
    fun `FR-MEMO-02 AC2 제목과 본문이 공백뿐이면 저장할 수 없다`() = runTest {
        val vm = create()
        assertFalse(vm.uiState.value.saveEnabled)

        vm.onAction(MemoEditAction.TitleChanged("   "))
        vm.onAction(MemoEditAction.BodyChanged(" \n  \n"))
        assertFalse(vm.uiState.value.saveEnabled)
        vm.onAction(MemoEditAction.SaveClicked)
        assertTrue(repo.created.isEmpty())

        // 날짜만 있어도 저장할 수 없다 (D-132, D-133)
        vm.onAction(MemoEditAction.DateAddClicked)
        vm.onAction(MemoEditAction.DateConfirmed)
        assertEquals(today, vm.uiState.value.memoDate)
        assertFalse(vm.uiState.value.saveEnabled)

        // 제목이나 본문 중 하나만 있으면 저장할 수 있다
        vm.onAction(MemoEditAction.BodyChanged("우유 사기"))
        assertTrue(vm.uiState.value.saveEnabled)
    }

    @Test
    fun `D-133 제목은 50자 본문은 5000자에서 더 입력되지 않는다`() = runTest {
        val vm = create()

        vm.onAction(MemoEditAction.TitleChanged("가".repeat(51)))
        vm.onAction(MemoEditAction.BodyChanged("나".repeat(5001)))

        assertEquals(50, vm.uiState.value.title.length)
        assertEquals(5000, vm.uiState.value.body.length)
    }

    @Test
    fun `FR-MEMO-02 AC1 저장하면 입력 그대로 POST하고 닫는다`() = runTest {
        val vm = create()
        vm.onAction(MemoEditAction.TitleChanged("  주말에 살 것 "))
        vm.onAction(MemoEditAction.BodyChanged("우유\n계란"))
        vm.onAction(MemoEditAction.DateAddClicked)
        vm.onAction(MemoEditAction.DatePicked(LocalDate.of(2026, 10, 17)))
        vm.onAction(MemoEditAction.DateConfirmed)

        vm.effects.test {
            vm.onAction(MemoEditAction.SaveClicked)
            assertEquals(MemoEditSideEffect.Close, awaitItem())
        }

        val sent = repo.created.single()
        // 정리(D-132)는 저장소가 보내기 직전에 한다
        assertEquals("  주말에 살 것 ", sent.title)
        assertEquals("우유\n계란", sent.body)
        assertEquals(LocalDate.of(2026, 10, 17), sent.memoDate)
    }

    @Test
    fun `FR-NOTI-04 AC1 커플의 새 메모는 알림 스위치가 기본 켜짐이고 끄면 false를 보낸다`() = runTest {
        val vm = create()
        assertTrue(vm.uiState.value.showNotify)
        assertTrue(vm.uiState.value.notifyPartner)
        vm.onAction(MemoEditAction.BodyChanged("본문"))

        vm.onAction(MemoEditAction.SaveClicked)
        assertEquals(true, repo.created.last().notifyPartner)

        val off = create()
        off.onAction(MemoEditAction.BodyChanged("본문"))
        off.onAction(MemoEditAction.NotifyChanged(false))
        off.onAction(MemoEditAction.SaveClicked)
        assertEquals(false, repo.created.last().notifyPartner)
    }

    @Test
    fun `FR-NOTI-04 AC3 개인 공간과 수정에서는 알림 스위치가 없다`() = runTest {
        // 개인 공간의 새 메모
        me.me.value = soloMe()
        val solo = create()
        assertFalse(solo.uiState.value.showNotify)
        solo.onAction(MemoEditAction.NotifyChanged(false))
        solo.onAction(MemoEditAction.BodyChanged("본문"))
        solo.onAction(MemoEditAction.SaveClicked)
        assertNull(repo.created.single().notifyPartner)

        // 커플 공간의 수정
        me.me.value = coupleMe()
        storeMine()
        val vm = edit()
        assertTrue(vm.uiState.value.isEdit)
        assertFalse(vm.uiState.value.showNotify)
    }

    // --- 수정 ---

    @Test
    fun `FR-MEMO-03 AC1 수정 화면은 메모의 제목 본문 날짜로 시작한다`() = runTest {
        storeMine()

        val state = edit().uiState.value

        assertTrue(state.isEdit)
        assertEquals("선물 아이디어", state.title)
        assertEquals("향수\n머플러", state.body)
        assertEquals(LocalDate.of(2026, 11, 20), state.memoDate)
        assertTrue(state.saveEnabled)
    }

    @Test
    fun `FR-MEMO-03 AC4 바꾼 것이 없으면 요청 없이 닫는다`() = runTest {
        storeMine()
        val vm = edit()

        vm.effects.test {
            vm.onAction(MemoEditAction.SaveClicked)
            assertEquals(MemoEditSideEffect.Close, awaitItem())
        }
        assertTrue(repo.patches.isEmpty())
    }

    @Test
    fun `FR-MEMO-03 AC4 앞뒤 공백만 다른 제목도 바꾼 것으로 보지 않는다`() = runTest {
        storeMine()
        val vm = edit()
        vm.onAction(MemoEditAction.TitleChanged("  선물 아이디어  "))

        vm.effects.test {
            vm.onAction(MemoEditAction.SaveClicked)
            assertEquals(MemoEditSideEffect.Close, awaitItem())
        }
        assertTrue(repo.patches.isEmpty())
    }

    @Test
    fun `FR-MEMO-03 AC1 수정은 바꾼 필드만 PATCH한다`() = runTest {
        storeMine()
        val vm = edit()
        vm.onAction(MemoEditAction.TitleChanged("선물 목록"))
        vm.onAction(MemoEditAction.DateRemoved)

        vm.effects.test {
            vm.onAction(MemoEditAction.SaveClicked)
            assertEquals(MemoEditSideEffect.Close, awaitItem())
        }

        val (id, patch) = repo.patches.single()
        assertEquals("m1", id)
        assertEquals(FieldChange.Set("선물 목록"), patch.title)
        assertEquals(FieldChange.Keep, patch.body)
        // 날짜를 뺐다: 값을 지운다
        assertEquals(FieldChange.Set<LocalDate>(null), patch.memoDate)
    }

    @Test
    fun `FR-MEMO-02 AC5 수정해서 제목과 본문이 모두 비면 저장할 수 없다`() = runTest {
        storeMine()
        val vm = edit()

        vm.onAction(MemoEditAction.TitleChanged(""))
        vm.onAction(MemoEditAction.BodyChanged("  \n "))

        assertFalse(vm.uiState.value.saveEnabled)
        vm.onAction(MemoEditAction.SaveClicked)
        assertTrue(repo.patches.isEmpty())
    }

    @Test
    fun `FR-MEMO-03 AC2 상대 메모나 없는 메모는 수정 화면을 열지 않고 닫는다`() = runTest {
        repo.memos.value = mapOf("m1" to makeMemo("m1", authorId = PartnerId))

        val vm = edit()

        assertFalse(vm.uiState.value.loaded)
        vm.effects.test {
            assertEquals(MemoEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(MemoEditSideEffect.Close, awaitItem())
        }
    }

    // --- 서버 오류 (D-121) ---

    @Test
    fun `D-121 수정이 404면 수정과 상세를 함께 닫는다`() = runTest {
        storeMine()
        repo.updateResult = apiError("NOT_FOUND")
        val vm = edit()
        vm.onAction(MemoEditAction.TitleChanged("바꾼 제목"))

        vm.effects.test {
            vm.onAction(MemoEditAction.SaveClicked)
            assertEquals(MemoEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(MemoEditSideEffect.CloseWithDetail, awaitItem())
            expectNoEvents()
        }
        // 저장소가 기기에서 지웠다
        assertNull(repo.memos.value["m1"])
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `D-121 수정이 403이면 수정 화면만 닫고 상세를 다시 불러온다`() = runTest {
        storeMine()
        repo.updateResult = apiError("NOT_AUTHOR")
        val vm = edit()
        vm.onAction(MemoEditAction.TitleChanged("바꾼 제목"))

        vm.effects.test {
            vm.onAction(MemoEditAction.SaveClicked)
            assertEquals(MemoEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(MemoEditSideEffect.Close, awaitItem())
            expectNoEvents()
        }
        assertEquals(1, repo.refreshAllCalls)
        assertEquals(listOf("m1"), repo.refreshMemoCalls)
    }

    @Test
    fun `D-121 그 밖의 서버 오류는 공통 오류만 보이고 입력은 그대로다`() = runTest {
        storeMine()
        repo.updateResult = apiError("INTERNAL_ERROR")
        val vm = edit()
        vm.onAction(MemoEditAction.TitleChanged("바꾼 제목"))

        vm.effects.test {
            vm.onAction(MemoEditAction.SaveClicked)
            assertEquals(MemoEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            expectNoEvents()
        }
        assertEquals("바꾼 제목", vm.uiState.value.title)
        assertFalse(vm.uiState.value.saving)
    }

    @Test
    fun `D-115 요청 중에는 loading이 true이고 끝나면 false다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repo.writeGate = gate
        val vm = create()
        vm.onAction(MemoEditAction.BodyChanged("본문"))

        vm.onAction(MemoEditAction.SaveClicked)
        assertTrue(vm.uiState.value.loading)
        assertTrue(vm.uiState.value.saving)
        // 요청 중에는 입력과 저장을 받지 않는다
        vm.onAction(MemoEditAction.BodyChanged("바꿈"))
        vm.onAction(MemoEditAction.SaveClicked)
        assertEquals("본문", vm.uiState.value.body)
        assertEquals(1, repo.created.size)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `오프라인이면 요청하지 않는다`() = runTest {
        // 저장소가 오프라인이면 요청 없이 Offline을 돌려준다 (NFR-OFF-02): 화면은 안내만 보이고 입력을 남긴다
        repo.createResult = Offline
        val vm = create()
        vm.onAction(MemoEditAction.BodyChanged("본문"))

        vm.effects.test {
            vm.onAction(MemoEditAction.SaveClicked)
            assertEquals(MemoEditSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            expectNoEvents()
        }
        assertEquals("본문", vm.uiState.value.body)
        assertFalse(vm.uiState.value.saving)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `SCR-MEMO-04 닫기는 저장 없이 닫는다`() = runTest {
        val vm = create()
        vm.onAction(MemoEditAction.BodyChanged("본문"))

        vm.effects.test {
            vm.onAction(MemoEditAction.CloseClicked)
            assertEquals(MemoEditSideEffect.Close, awaitItem())
        }
        assertTrue(repo.created.isEmpty())
    }
}
