package com.duriseo.android.domain.memo

/** 메모를 목록·캘린더 카드에 보일 때의 글 규칙 (D-131). 서버가 만드는 알림 문구도 같은 규칙이다 */
object MemoTexts {
    /** 제목이 없을 때 본문 첫 줄을 쓰는 최대 글자 수 (D-82 기준 코드 포인트) */
    const val LIST_TITLE_MAX = 50

    private const val ELLIPSIS = "…"

    /**
     * 목록 항목의 제목 (D-131): 제목이 있으면 그대로, 없으면 본문에서 앞뒤 공백을 지운 뒤 첫 번째 비어 있지 않은 줄.
     * 50글자까지 쓰고 넘으면 49글자 뒤에 "…"를 붙인다. 제목도 본문도 없으면 빈 문자열이다.
     */
    fun listTitle(title: String?, body: String?): String {
        val t = title?.trim().orEmpty()
        if (t.isNotEmpty()) return t
        val firstLine = body?.trim()?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }.orEmpty()
        return ellipsize(firstLine)
    }

    fun listTitle(memo: Memo): String = listTitle(memo.title, memo.body)

    private fun ellipsize(text: String): String {
        if (text.codePointCount(0, text.length) <= LIST_TITLE_MAX) return text
        return text.substring(0, text.offsetByCodePoints(0, LIST_TITLE_MAX - 1)) + ELLIPSIS
    }
}
