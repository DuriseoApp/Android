package com.duriseo.android.domain.anniversary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AnniversaryRulesTest {
    @Test
    fun `FR-ANNIV-03 AC1 이름은 앞뒤 공백을 지운다`() {
        assertEquals("처음 여행", AnniversaryRules.normalizeName("  처음 여행 \n"))
    }

    @Test
    fun `FR-ANNIV-03 AC4 공백뿐인 이름은 이름이 없는 것이다`() {
        assertFalse(AnniversaryRules.isNameValid("   "))
        assertFalse(AnniversaryRules.isNameValid(""))
        assertTrue(AnniversaryRules.isNameValid(" 가 "))
    }

    @Test
    fun `FR-ANNIV-03 AC4 이름은 코드 포인트 30개까지만 남긴다`() {
        val long = "가".repeat(40)
        assertEquals(30, AnniversaryRules.nameLength(AnniversaryRules.limitName(long)))
        // 이모지는 UTF-16 두 칸이지만 한 글자다 (D-82)
        val emoji = "😀".repeat(31)
        val limited = AnniversaryRules.limitName(emoji)
        assertEquals(30, AnniversaryRules.nameLength(limited))
        assertEquals("😀".repeat(30), limited)
    }

    @Test
    fun `FR-ANNIV-04 AC1 오늘과 30일 이내는 강조한다`() {
        assertTrue(AnniversaryRules.isNear(0))
        assertTrue(AnniversaryRules.isNear(30))
        assertFalse(AnniversaryRules.isNear(31))
    }

    @Test
    fun `Q-19 월 조회는 앞뒤 하루를 더한다`() {
        val (from, to) = calendarMonthRange(YearMonth.of(2026, 10))
        assertEquals(LocalDate.of(2026, 9, 30), from)
        assertEquals(LocalDate.of(2026, 11, 1), to)
    }
}
