package com.duriseo.android.feature.memo.component

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoPreviewTest {

    @Test
    fun `FR-MEMO-01 AC3 제목이 있으면 본문 전체를 한 줄로 접어 미리보기로 쓴다`() {
        assertEquals("우유, 계란 두부", MemoPreview.of("장보기", "우유, 계란\n\n  두부 "))
    }

    @Test
    fun `D-131 제목이 없으면 제목 자리에 올라간 첫 줄을 빼고 나머지를 미리보기로 쓴다`() {
        assertEquals("두 번째 줄 세 번째 줄", MemoPreview.of(null, "\n  첫 줄\n두 번째 줄\n\n세 번째 줄"))
        // 한 줄뿐이면 미리보기는 비어 있다
        assertEquals("", MemoPreview.of(null, "첫 줄뿐"))
        assertEquals("", MemoPreview.of("제목뿐", null))
    }

    @Test
    fun `FR-MEMO-01 AC3 미리보기는 200자까지만 상태에 담는다`() {
        assertEquals(MemoPreview.MAX_CHARS, MemoPreview.of("제목", "가".repeat(1000)).length)
    }
}
