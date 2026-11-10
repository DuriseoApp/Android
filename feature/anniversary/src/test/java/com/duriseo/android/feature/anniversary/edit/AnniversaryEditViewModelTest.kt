package com.duriseo.android.feature.anniversary.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.duriseo.android.core.common.navigation.AnniversaryRouteModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.anniversary.AnniversaryPatch
import com.duriseo.android.feature.anniversary.FakeAnniversaryRepository
import com.duriseo.android.feature.anniversary.FakeMeRepository
import com.duriseo.android.feature.anniversary.Offline
import com.duriseo.android.feature.anniversary.PartnerId
import com.duriseo.android.feature.anniversary.apiError
import com.duriseo.android.feature.anniversary.coupleMe
import com.duriseo.android.feature.anniversary.custom
import com.duriseo.android.feature.anniversary.past
import com.duriseo.android.feature.anniversary.soloMe
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class AnniversaryEditViewModelTest {

    /** 2026-10-12(월) 오전 10:20, 서울 */
    private val clock = Clock.fixed(Instant.parse("2026-10-12T01:20:00Z"), ZoneId.of("Asia/Seoul"))
    private val me = FakeMeRepository(coupleMe())
    private val today = LocalDate.of(2026, 10, 12)

    /** 매년 반복 기념일: 2025-11-20에 등록했고 목록에는 2026년 발생일(2026-11-20)이 보인다 */
    private val birthday = custom("a1", "생일", LocalDate.of(2026, 11, 20), yearly = true)
    private val repo = FakeAnniversaryRepository(listOf(birthday))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun create() = AnniversaryEditViewModel(SavedStateHandle(emptyMap<String, Any?>()), repo, me, clock)

    private fun edit(id: String = "a1", date: String? = "2026-11-20"): AnniversaryEditViewModel {
        val args = buildMap<String, Any?> {
            put(AnniversaryRouteModel.Edit::anniversaryId.name, id)
            if (date != null) put(AnniversaryRouteModel.Edit::date.name, date)
        }
        return AnniversaryEditViewModel(SavedStateHandle(args), repo, me, clock)
    }

    // --- 새 기념일 (D-122) ---

    @Test
    fun `FR-ANNIV-03 AC4 새 기념일은 오늘 날짜이고 매년 반복이 켜져 있다`() = runTest {
        val state = create().uiState.value

        assertTrue(state.loaded)
        assertFalse(state.isEdit)
        assertEquals(today, state.date)
        assertTrue(state.repeatsYearly)
        assertEquals("", state.name)
        // 이름이 비어 [저장]은 꺼져 있다
        assertFalse(state.saveEnabled)
    }

    @Test
    fun `FR-ANNIV-03 AC4 이름이 공백뿐이면 저장할 수 없고 30자를 넘겨 입력되지 않는다`() = runTest {
        val vm = create()

        vm.onAction(AnniversaryEditAction.NameChanged("    "))
        assertFalse(vm.uiState.value.saveEnabled)
        vm.onAction(AnniversaryEditAction.SaveClicked)
        assertTrue(repo.created.isEmpty())

        vm.onAction(AnniversaryEditAction.NameChanged("가".repeat(31)))
        assertEquals(30, vm.uiState.value.nameLength)
        assertEquals("가".repeat(30), vm.uiState.value.name)
        assertTrue(vm.uiState.value.saveEnabled)
    }

    @Test
    fun `FR-ANNIV-03 AC4 커플 공간과 개인 공간을 구분한다`() = runTest {
        assertTrue(create().uiState.value.isCouple)
        me.me.value = soloMe()
        assertFalse(create().uiState.value.isCouple)
    }

    @Test
    fun `FR-ANNIV-03 AC1 저장하면 앞뒤 공백을 지운 이름으로 POST한다`() = runTest {
        val vm = create()
        vm.onAction(AnniversaryEditAction.NameChanged("  처음 여행 간 날 "))

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.SaveClicked)
            assertEquals(AnniversaryEditSideEffect.Close, awaitItem())
        }

        val made = repo.created.single()
        assertEquals("처음 여행 간 날", made.name)
        assertEquals(today, made.date)
        assertEquals(true, made.repeatsYearly)
    }

    @Test
    fun `FR-ANNIV-03 AC1 날짜 시트에서 고른 날짜와 매년 반복 끔을 보낸다`() = runTest {
        val vm = create()
        vm.onAction(AnniversaryEditAction.NameChanged("이름"))
        vm.onAction(AnniversaryEditAction.YearlyChanged(false))
        vm.onAction(AnniversaryEditAction.DateRowClicked)
        assertEquals(YearMonth.of(2026, 10), vm.uiState.value.dateSheet!!.visibleMonth)
        vm.onAction(AnniversaryEditAction.DatePicked(LocalDate.of(2050, 12, 31)))
        // 2050-12-31 다음 날은 고를 수 없다
        vm.onAction(AnniversaryEditAction.DatePicked(LocalDate.of(2051, 1, 1)))
        vm.onAction(AnniversaryEditAction.DateConfirmed)
        assertNull(vm.uiState.value.dateSheet)

        vm.onAction(AnniversaryEditAction.SaveClicked)

        val made = repo.created.single()
        assertEquals(LocalDate.of(2050, 12, 31), made.date)
        assertEquals(false, made.repeatsYearly)
    }

    // --- 수정 (확인 필요 1) ---

    @Test
    fun `FR-ANNIV-03 수정 화면은 누른 항목의 이름 날짜 반복을 채운다`() = runTest {
        val state = edit().uiState.value

        assertTrue(state.isEdit)
        assertEquals("생일", state.name)
        // 날짜 칸은 누른 발생일이다 (등록한 날짜가 아닐 수 있다)
        assertEquals(LocalDate.of(2026, 11, 20), state.date)
        assertTrue(state.repeatsYearly)
        assertTrue(state.saveEnabled)
    }

    @Test
    fun `FR-ANNIV-03 AC6 바꾼 것이 없으면 요청 없이 닫는다`() = runTest {
        val vm = edit()

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.SaveClicked)
            assertEquals(AnniversaryEditSideEffect.Close, awaitItem())
        }
        assertTrue(repo.patches.isEmpty())
        assertTrue(repo.created.isEmpty())
    }

    @Test
    fun `FR-ANNIV-03 AC6 이름 앞뒤 공백만 바뀌면 바꾼 것이 없다`() = runTest {
        val vm = edit()
        vm.onAction(AnniversaryEditAction.NameChanged("  생일  "))

        vm.onAction(AnniversaryEditAction.SaveClicked)

        assertTrue(repo.patches.isEmpty())
    }

    @Test
    fun `FR-ANNIV-03 수정은 바꾼 필드만 PATCH하고 날짜를 안 바꾸면 date를 보내지 않는다`() = runTest {
        val vm = edit()
        vm.onAction(AnniversaryEditAction.NameChanged("생일 파티"))

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.SaveClicked)
            assertEquals(AnniversaryEditSideEffect.Close, awaitItem())
        }

        assertEquals(listOf("a1" to AnniversaryPatch(name = "생일 파티")), repo.patches)
    }

    @Test
    fun `FR-ANNIV-03 수정에서 날짜를 바꾸면 고른 날이 새 등록일로 간다`() = runTest {
        val vm = edit()
        vm.onAction(AnniversaryEditAction.DateRowClicked)
        vm.onAction(AnniversaryEditAction.DatePicked(LocalDate.of(2026, 11, 21)))
        vm.onAction(AnniversaryEditAction.DateConfirmed)
        vm.onAction(AnniversaryEditAction.YearlyChanged(false))

        vm.onAction(AnniversaryEditAction.SaveClicked)

        assertEquals(
            listOf("a1" to AnniversaryPatch(date = LocalDate.of(2026, 11, 21), repeatsYearly = false)),
            repo.patches,
        )
    }

    @Test
    fun `FR-ANNIV-04 AC3 지난 기념일 항목도 수정할 수 있다`() = runTest {
        val repo2 = FakeAnniversaryRepository(pastItems = listOf(past("p1", "제주 여행 출발", LocalDate.of(2026, 8, 1))))
        val vm = AnniversaryEditViewModel(
            SavedStateHandle(mapOf(AnniversaryRouteModel.Edit::anniversaryId.name to "p1")),
            repo2,
            me,
            clock,
        )

        val state = vm.uiState.value
        assertTrue(state.loaded)
        assertEquals("제주 여행 출발", state.name)
        assertEquals(LocalDate.of(2026, 8, 1), state.date)
        // 지난 기념일은 1회성이다
        assertFalse(state.repeatsYearly)
    }

    @Test
    fun `FR-ANNIV-03 AC2 상대가 만든 기념일은 열지 않고 닫는다`() = runTest {
        val repo2 = FakeAnniversaryRepository(listOf(custom("p1", "처음 여행 간 날", LocalDate.of(2026, 12, 5), authorId = PartnerId)))
        val vm = AnniversaryEditViewModel(
            SavedStateHandle(mapOf(AnniversaryRouteModel.Edit::anniversaryId.name to "p1")),
            repo2,
            me,
            clock,
        )

        vm.effects.test {
            assertEquals(AnniversaryEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(AnniversaryEditSideEffect.Close, awaitItem())
        }
        assertFalse(vm.uiState.value.loaded)
    }

    // --- 삭제 ---

    @Test
    fun `FR-ANNIV-03 삭제 확인 뒤 DELETE하고 닫는다`() = runTest {
        val vm = edit()

        vm.onAction(AnniversaryEditAction.DeleteClicked)
        assertTrue(vm.uiState.value.deleteDialogVisible)
        assertEquals("생일", vm.uiState.value.originalName)
        // 확인 전에는 요청하지 않는다
        assertTrue(repo.deleted.isEmpty())

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.DeleteConfirmed)
            assertEquals(AnniversaryEditSideEffect.Close, awaitItem())
        }
        assertEquals(listOf("a1"), repo.deleted)
    }

    @Test
    fun `FR-ANNIV-03 삭제 확인에서 취소하면 요청 없이 다이얼로그를 닫는다`() = runTest {
        val vm = edit()
        vm.onAction(AnniversaryEditAction.DeleteClicked)

        vm.onAction(AnniversaryEditAction.DeleteDismissed)

        assertFalse(vm.uiState.value.deleteDialogVisible)
        assertTrue(repo.deleted.isEmpty())
    }

    @Test
    fun `FR-ANNIV-03 새 기념일에는 삭제가 없다`() = runTest {
        val vm = create()

        vm.onAction(AnniversaryEditAction.DeleteClicked)

        assertFalse(vm.uiState.value.deleteDialogVisible)
    }

    // --- 서버 오류 (D-121) ---

    @Test
    fun `FR-ANNIV-03 AC5 404면 기기에서 지우고 화면을 닫는다`() = runTest {
        repo.updateResult = apiError("NOT_FOUND")
        val vm = edit()
        vm.onAction(AnniversaryEditAction.NameChanged("새 이름"))

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.SaveClicked)
            // 공통 오류 안내를 보이고 화면을 닫는다
            assertEquals(AnniversaryEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(AnniversaryEditSideEffect.Close, awaitItem())
        }
        // 저장소가 기기에서 지웠다 (저장소 계약, 진짜 동작은 AnniversaryRepositoryTest)
        assertTrue(repo.upcoming.value.items.isEmpty())
    }

    @Test
    fun `FR-ANNIV-03 AC5 403이면 화면을 닫고 공통 오류를 보이고 다시 불러온다`() = runTest {
        repo.updateResult = apiError("NOT_AUTHOR")
        val vm = edit()
        vm.onAction(AnniversaryEditAction.NameChanged("새 이름"))

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.SaveClicked)
            assertEquals(AnniversaryEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(AnniversaryEditSideEffect.Close, awaitItem())
        }
        // 저장소가 목록·달을 다시 불러왔다 (저장소 계약, 진짜 동작은 AnniversaryRepositoryTest)
        assertEquals(1, repo.refreshAllCalls)
    }

    @Test
    fun `FR-ANNIV-03 AC5 삭제가 404여도 닫고 공통 오류를 보인다`() = runTest {
        repo.deleteResult = apiError("NOT_FOUND")
        val vm = edit()
        vm.onAction(AnniversaryEditAction.DeleteClicked)

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.DeleteConfirmed)
            assertEquals(AnniversaryEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(AnniversaryEditSideEffect.Close, awaitItem())
        }
        assertFalse(vm.uiState.value.deleteDialogVisible)
    }

    @Test
    fun `오류 VALIDATION_FAILED는 공통 오류를 보이고 화면은 그대로다`() = runTest {
        repo.createResult = apiError("VALIDATION_FAILED")
        val vm = create()
        vm.onAction(AnniversaryEditAction.NameChanged("이름"))

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.SaveClicked)
            assertEquals(AnniversaryEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            expectNoEvents()
        }
        assertFalse(vm.uiState.value.saving)
        assertEquals("이름", vm.uiState.value.name)
    }

    @Test
    fun `오프라인이면 요청하지 않고 인터넷 연결 안내를 보인다`() = runTest {
        // 저장소가 오프라인이면 API를 부르지 않고 Offline을 돌려준다 (NFR-OFF-02, 저장소 테스트가 확인)
        repo.createResult = Offline
        val vm = create()
        vm.onAction(AnniversaryEditAction.NameChanged("이름"))

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.SaveClicked)
            assertEquals(AnniversaryEditSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            expectNoEvents()
        }
        assertFalse(vm.uiState.value.saving)
    }

    @Test
    fun `SCR-ANNIV-02 닫기를 누르면 닫는다`() = runTest {
        val vm = create()

        vm.effects.test {
            vm.onAction(AnniversaryEditAction.CloseClicked)
            assertEquals(AnniversaryEditSideEffect.Close, awaitItem())
        }
    }
}
