package com.duriseo.android.domain.memo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MemoRulesTest {
    private val original = Memo(
        id = "m1",
        title = "장보기",
        body = "우유\n계란",
        memoDate = LocalDate.of(2026, 10, 17),
        authorId = "u1",
        isPinned = false,
        pinnedAt = null,
        isFavorite = false,
        createdAt = Instant.parse("2026-10-08T00:12:00Z"),
        updatedAt = Instant.parse("2026-10-08T00:12:00Z"),
    )

    @Test
    fun `FR-MEMO-02 AC2 제목은 앞뒤 공백을 지우고 본문은 공백과 줄바꿈을 그대로 둔다`() {
        assertEquals("장보기", MemoRules.normalizeTitle("  장보기 \n"))
        assertEquals("  우유\n\n계란  ", MemoRules.normalizeBody("  우유\n\n계란  "))
    }

    @Test
    fun `FR-MEMO-02 AC2 제목과 본문이 공백뿐이면 저장할 수 없다`() {
        assertFalse(MemoRules.isSavable("   ", " \n\t "))
        assertFalse(MemoRules.isSavable(null, null))
        assertFalse(MemoRules.isSavable("", ""))
        assertTrue(MemoRules.isSavable(" 제목 ", null))
        assertTrue(MemoRules.isSavable(null, "\n본문\n"))
        assertNull(MemoRules.normalizeTitle("  "))
        assertNull(MemoRules.normalizeBody(" \n "))
    }

    @Test
    fun `D-133 제목은 50 본문은 5000 코드 포인트까지만 남긴다`() {
        assertEquals("가".repeat(50), MemoRules.limitTitle("가".repeat(60)))
        assertEquals("😀".repeat(50), MemoRules.limitTitle("😀".repeat(51)))
        assertEquals("나".repeat(5000), MemoRules.limitBody("나".repeat(5001)))
        assertEquals("짧은 글", MemoRules.limitBody("짧은 글"))
    }

    @Test
    fun `FR-CAL-01 AC6 메모 날짜는 1950-01-01 부터 2050-12-31 까지 고를 수 있다`() {
        assertTrue(MemoRules.isSelectable(LocalDate.of(1950, 1, 1)))
        assertTrue(MemoRules.isSelectable(LocalDate.of(2050, 12, 31)))
        assertFalse(MemoRules.isSelectable(LocalDate.of(1949, 12, 31)))
        assertFalse(MemoRules.isSelectable(LocalDate.of(2051, 1, 1)))
    }

    @Test
    fun `FR-MEMO-03 AC4 같은 값이면 바뀐 것이 없다`() {
        val patch = MemoRules.patchOf(original, "장보기", "우유\n계란", LocalDate.of(2026, 10, 17))

        assertTrue(patch.isEmpty)
    }

    @Test
    fun `FR-MEMO-03 AC4 앞뒤 공백만 다른 제목과 공백뿐인 본문 입력도 정리한 값으로 비교한다`() {
        val noBody = original.copy(body = null, memoDate = null)

        val patch = MemoRules.patchOf(noBody, "  장보기  ", "  \n ", null)

        assertTrue(patch.isEmpty)
    }

    @Test
    fun `D-123 바뀐 필드만 patch에 담는다`() {
        val patch = MemoRules.patchOf(original, "  새 제목 ", "우유\n계란", LocalDate.of(2026, 10, 17))

        assertEquals(FieldChange.Set("새 제목"), patch.title)
        assertEquals(FieldChange.Keep, patch.body)
        assertEquals(FieldChange.Keep, patch.memoDate)
        assertFalse(patch.isEmpty)
    }

    @Test
    fun `D-123 지운 날짜와 비운 제목은 null 값으로 담는다`() {
        val patch = MemoRules.patchOf(original, "   ", "우유\n계란", null)

        assertEquals(FieldChange.Set<String>(null), patch.title)
        assertEquals(FieldChange.Keep, patch.body)
        assertEquals(FieldChange.Set<LocalDate>(null), patch.memoDate)
    }

    @Test
    fun `D-123 본문은 공백만 달라도 바뀐 것이다`() {
        val patch = MemoRules.patchOf(original, "장보기", "우유\n계란\n", LocalDate.of(2026, 10, 17))

        assertEquals(FieldChange.Set("우유\n계란\n"), patch.body)
        assertEquals(FieldChange.Keep, patch.title)
    }
}
