package com.duriseo.android.domain.couple

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorChangeTest {

    @Test
    fun `FR-CAL-04 AC2 바꾼 것이 없으면 비어 있다`() {
        assertTrue(ColorChange().isEmpty)
    }

    @Test
    fun `FR-CAL-04 AC2 함께 색만 있어도 비어 있지 않다`() {
        assertFalse(ColorChange(together = "pick-purple").isEmpty)
    }

    @Test
    fun `FR-CAL-04 AC2 사람 색만 있어도 비어 있지 않다`() {
        assertFalse(ColorChange(members = listOf(MemberColor("u1", "pick-rose"))).isEmpty)
    }
}
