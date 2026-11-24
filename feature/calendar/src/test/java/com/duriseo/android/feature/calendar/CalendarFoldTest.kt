package com.duriseo.android.feature.calendar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarFoldTest {
    private val threshold = 40f

    @Test
    fun `D-163 접힌 상태에서 아래로 기준 이상 쓸면 펼친다`() {
        assertEquals(FoldChange.Expand, foldChangeFor(40f, expanded = false, thresholdPx = threshold))
        assertEquals(FoldChange.Expand, foldChangeFor(120f, expanded = false, thresholdPx = threshold))
    }

    @Test
    fun `D-163 펼친 상태에서 위로 기준 이상 쓸면 접는다`() {
        assertEquals(FoldChange.Collapse, foldChangeFor(-40f, expanded = true, thresholdPx = threshold))
    }

    @Test
    fun `D-163 기준 미만은 그대로다`() {
        assertEquals(FoldChange.None, foldChangeFor(39f, expanded = false, thresholdPx = threshold))
        assertEquals(FoldChange.None, foldChangeFor(-39f, expanded = true, thresholdPx = threshold))
    }

    @Test
    fun `D-163 이미 펼친 상태에서 아래로, 접힌 상태에서 위로 쓸면 그대로다`() {
        assertEquals(FoldChange.None, foldChangeFor(100f, expanded = true, thresholdPx = threshold))
        assertEquals(FoldChange.None, foldChangeFor(-100f, expanded = false, thresholdPx = threshold))
    }

    @Test
    fun `D-163 펼친 상태에서 목록을 위로 끌면 스크롤 전에 이동량을 소비하며 한 번만 접는다`() {
        var expanded = true
        var toggles = 0
        val connection = FoldNestedScrollConnection({ expanded }, threshold) { toggles++; expanded = !expanded }
        val first = connection.onPreScroll(Offset(0f, -30f), NestedScrollSource.UserInput)
        assertEquals(-30f, first.y, 0f)
        assertEquals(0, toggles)
        connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)
        assertEquals(1, toggles)
        // 같은 끌기에서 계속 끌어도 더 바꾸지 않고, 접힌 뒤에는 목록 스크롤에 맡긴다
        val after = connection.onPreScroll(Offset(0f, -80f), NestedScrollSource.UserInput)
        assertEquals(0f, after.y, 0f)
        assertEquals(1, toggles)
    }

    @Test
    fun `D-163 접힌 상태에서 목록이 맨 위일 때 남은 아래 이동량이 기준 이상이면 펼친다`() {
        var expanded = false
        var toggles = 0
        val connection = FoldNestedScrollConnection({ expanded }, threshold) { toggles++; expanded = !expanded }
        connection.onPostScroll(Offset.Zero, Offset(0f, 25f), NestedScrollSource.UserInput)
        assertEquals(0, toggles)
        connection.onPostScroll(Offset.Zero, Offset(0f, 25f), NestedScrollSource.UserInput)
        assertEquals(1, toggles)
    }

    @Test
    fun `D-163 접힌 상태에서 위로, 펼친 상태에서 아래로 끄는 건 목록 스크롤 그대로다`() {
        var toggles = 0
        val collapsed = FoldNestedScrollConnection({ false }, threshold) { toggles++ }
        assertEquals(Offset.Zero, collapsed.onPreScroll(Offset(0f, -100f), NestedScrollSource.UserInput))
        assertEquals(Offset.Zero, collapsed.onPostScroll(Offset.Zero, Offset(0f, -100f), NestedScrollSource.UserInput))
        val expanded = FoldNestedScrollConnection({ true }, threshold) { toggles++ }
        assertEquals(Offset.Zero, expanded.onPreScroll(Offset(0f, 100f), NestedScrollSource.UserInput))
        assertEquals(Offset.Zero, expanded.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.UserInput))
        assertEquals(0, toggles)
    }
}
