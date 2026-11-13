package com.duriseo.android.domain.memo

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoTextsTest {
    @Test
    fun `D-131 제목이 있으면 제목을 그대로 쓴다`() {
        assertEquals("장보기", MemoTexts.listTitle("장보기", "우유\n계란"))
    }

    @Test
    fun `D-131 제목이 없으면 본문 첫 비어 있지 않은 줄을 쓰고 50자를 넘으면 49자와 말줄임표다`() {
        // 앞뒤 공백과 빈 줄을 건너뛴 첫 줄
        assertEquals("우유 사기", MemoTexts.listTitle(null, "  \n\n  우유 사기  \n계란"))
        // 줄바꿈이 CRLF여도 같다
        assertEquals("첫째 줄", MemoTexts.listTitle(null, "\r\n첫째 줄\r\n둘째 줄"))
        // 딱 50자는 그대로
        val exactly50 = "가".repeat(50)
        assertEquals(exactly50, MemoTexts.listTitle(null, exactly50))
        // 51자부터는 49자 + "…"
        assertEquals("가".repeat(49) + "…", MemoTexts.listTitle(null, "가".repeat(51)))
        assertEquals("가".repeat(49) + "…", MemoTexts.listTitle(null, "가".repeat(500) + "\n다음 줄"))
    }

    @Test
    fun `D-131 글자 수는 코드 포인트로 센다`() {
        // 이모지는 UTF-16 두 칸이지만 한 글자다 (D-82)
        assertEquals("😀".repeat(50), MemoTexts.listTitle(null, "😀".repeat(50)))
        assertEquals("😀".repeat(49) + "…", MemoTexts.listTitle(null, "😀".repeat(51)))
    }

    @Test
    fun `D-131 공백뿐인 제목은 없는 것으로 보고 둘 다 없으면 빈 문자열이다`() {
        assertEquals("본문", MemoTexts.listTitle("   ", "본문"))
        assertEquals("", MemoTexts.listTitle(null, null))
        assertEquals("", MemoTexts.listTitle("  ", " \n "))
    }
}
