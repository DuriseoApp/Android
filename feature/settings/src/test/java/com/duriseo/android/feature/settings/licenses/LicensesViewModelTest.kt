package com.duriseo.android.feature.settings.licenses

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class FakeLicenseSource(private val libraries: List<LicenseEntry>) : LicenseSource {
    override suspend fun libraries(): List<LicenseEntry> = libraries
    override suspend fun fontLicenseText(): String = "OFL text"
}

@OptIn(ExperimentalCoroutinesApi::class)
class LicensesViewModelTest {

    private val libraries = listOf(
        LicenseEntry("z", "Zeta", "MIT", "MIT text"),
        LicenseEntry("a", "alpha", "Apache License 2.0", "Apache text"),
        LicenseEntry("b", "Beta", "Apache License 2.0", "Apache text"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FR-SET-02 AC3 글꼴 두 행은 목록 끝에 있다`() = runTest {
        val vm = LicensesViewModel(FakeLicenseSource(libraries))

        val names = vm.uiState.value.rows.map { it.name }

        // 라이브러리는 이름순(대소문자 무시), 그 뒤에 글꼴 두 행
        assertEquals(listOf("alpha", "Beta", "Zeta", "IBM Plex Sans KR", "IBM Plex Mono"), names)
        val fonts = vm.uiState.value.rows.takeLast(2)
        assertEquals(listOf("SIL OFL 1.1", "SIL OFL 1.1"), fonts.map { it.licenseName })
        assertEquals(listOf("OFL text", "OFL text"), fonts.map { it.body })
    }

    @Test
    fun `FR-SET-02 AC3 라이브러리 목록을 못 읽어도 글꼴 두 행은 보인다`() = runTest {
        val vm = LicensesViewModel(FakeLicenseSource(emptyList()))

        assertEquals(listOf("IBM Plex Sans KR", "IBM Plex Mono"), vm.uiState.value.rows.map { it.name })
    }

    @Test
    fun `FR-SET-02 AC3 행을 누르면 펼치고 다시 누르면 접는다`() = runTest {
        val vm = LicensesViewModel(FakeLicenseSource(libraries))
        assertNull(vm.uiState.value.expandedId)

        vm.onAction(LicensesAction.RowClicked("a"))
        assertEquals(listOf("alpha"), vm.uiState.value.displayRows.filter { it.expanded }.map { it.name })

        // 다른 행을 누르면 그 행만 펼쳐진다
        vm.onAction(LicensesAction.RowClicked("b"))
        assertEquals(listOf("Beta"), vm.uiState.value.displayRows.filter { it.expanded }.map { it.name })

        vm.onAction(LicensesAction.RowClicked("b"))
        assertNull(vm.uiState.value.expandedId)
        assertEquals(emptyList<String>(), vm.uiState.value.displayRows.filter { it.expanded }.map { it.name })
    }
}
