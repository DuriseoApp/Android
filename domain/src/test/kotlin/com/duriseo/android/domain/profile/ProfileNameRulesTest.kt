package com.duriseo.android.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileNameRulesTest {

    @Test
    fun `FR-AUTH-07 AC6 이름은 코드 포인트 10개까지만 남긴다`() {
        // 이모지 하나는 UTF-16으로 두 칸이다. 코드 포인트로 세어 10개에서 자른다
        val text = "가나다라마바사아자차😀카"
        assertEquals("가나다라마바사아자차", ProfileNameRules.limit(text))
        val emoji = "😀".repeat(11)
        assertEquals(10, ProfileNameRules.length(ProfileNameRules.limit(emoji)))
        assertEquals("짧은 이름", ProfileNameRules.limit("짧은 이름"))
    }

    @Test
    fun `FR-AUTH-07 AC6 앞뒤 공백만 있는 이름은 빈 값이다`() {
        assertTrue(ProfileNameRules.isBlank("   "))
        assertTrue(ProfileNameRules.isBlank(""))
        assertFalse(ProfileNameRules.isBlank(" 민 "))
        assertEquals("민 지", ProfileNameRules.normalize("  민 지 "))
    }
}
