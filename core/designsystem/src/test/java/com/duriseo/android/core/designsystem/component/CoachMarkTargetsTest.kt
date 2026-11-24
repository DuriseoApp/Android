package com.duriseo.android.core.designsystem.component

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 코치 마크(튜토리얼) 대상 위치 모음 (FR-CAL-06 AC2): 캘린더 화면과 하단 탭이 각자 위치를 넣고 튜토리얼이 꺼내 쓴다 */
class CoachMarkTargetsTest {
    private val targets = CoachMarkTargets()

    @Test
    fun `FR-CAL-06 AC2 위치를 넣으면 키로 꺼내고 모르는 대상은 null이다`() {
        targets.update("fab", Rect(1f, 2f, 3f, 4f))

        assertEquals(Rect(1f, 2f, 3f, 4f), targets.rectOf("fab"))
        assertNull(targets.rectOf("menu"))
        assertTrue("fab" in targets.keys)
    }

    @Test
    fun `FR-CAL-06 AC2 배치가 바뀌면 새 위치로 바뀌고 화면에서 사라지면 지워진다`() {
        targets.update("tab", Rect(0f, 0f, 10f, 10f))
        targets.update("tab", Rect(0f, 5f, 10f, 15f))
        assertEquals(Rect(0f, 5f, 10f, 15f), targets.rectOf("tab"))

        targets.remove("tab")

        assertNull(targets.rectOf("tab"))
        assertFalse("tab" in targets.keys)
    }
}
