package com.duriseo.android.domain.couple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeInputTest {

    @Test
    fun `FR-COUPLE-01 AC4 소문자로 입력하면 대문자로 바뀐다`() {
        assertEquals("K7Q29X", InviteCodeInput.clean("k7q29x"))
    }

    @Test
    fun `FR-COUPLE-01 AC4 영문과 숫자가 아닌 글자는 넣지 않는다`() {
        assertEquals("K7Q2", InviteCodeInput.clean(" k7-q2 "))
    }

    @Test
    fun `FR-COUPLE-01 AC4 6자리까지만 받는다`() {
        assertEquals("ABCDEF", InviteCodeInput.clean("abcdefgh"))
    }

    @Test
    fun `FR-COUPLE-02 AC1 6자리가 차야 완성이다`() {
        assertFalse(InviteCodeInput.isComplete("ABCDE"))
        assertTrue(InviteCodeInput.isComplete("ABCDEF"))
    }
}
