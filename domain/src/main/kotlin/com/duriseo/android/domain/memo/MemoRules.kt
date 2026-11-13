package com.duriseo.android.domain.memo

import com.duriseo.android.domain.event.EventRules
import java.time.LocalDate

/** 메모 쓰기 규칙 (PRD FR-MEMO-02·03, D-132, D-133, API.md 3.4 "메모 쓰기 규칙"). 화면·리포지토리가 같은 규칙을 쓴다 */
object MemoRules {
    /** 제목 50자, 본문 5,000자 (NFR-LIMIT-01). 글자 수는 코드 포인트로 센다 (D-82) */
    const val TITLE_MAX = 50
    const val BODY_MAX = 5000

    /** 메모 날짜 선택 범위 1950-01-01 ~ 2050-12-31 (D-74). 일정과 같다 */
    val MIN_DATE: LocalDate = EventRules.MIN_DATE
    val MAX_DATE: LocalDate = EventRules.MAX_DATE

    fun isSelectable(date: LocalDate): Boolean = EventRules.isSelectable(date)

    /** 제목은 앞뒤 공백을 지운 값. 지운 뒤 비면 null = 제목 없음 (D-132) */
    fun normalizeTitle(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }

    /** 본문은 공백·줄바꿈을 그대로 두되, 공백·줄바꿈뿐이면 null = 본문 없음 (D-132) */
    fun normalizeBody(raw: String?): String? = raw?.takeIf { it.isNotBlank() }

    /** 정리한 제목·본문 중 하나라도 있어야 저장할 수 있다. 날짜만 있어도 안 된다 (D-132, D-133, FR-MEMO-02 AC2) */
    fun isSavable(title: String?, body: String?): Boolean =
        normalizeTitle(title) != null || normalizeBody(body) != null

    /** 제목 입력을 [TITLE_MAX] 코드 포인트까지만 남긴다 (한도를 넘는 입력은 받지 않는다, 오류 문구 없음, D-133) */
    fun limitTitle(text: String): String = EventRules.limit(text, TITLE_MAX)

    /** 본문 입력을 [BODY_MAX] 코드 포인트까지만 남긴다 */
    fun limitBody(text: String): String = EventRules.limit(text, BODY_MAX)

    /**
     * 수정 화면의 입력([title]·[body]·[memoDate])을 [original]과 비교해 바뀐 필드만 담은 [MemoPatch]를 만든다 (D-123, FR-MEMO-03 AC4).
     * 입력은 정리한 값([normalizeTitle]·[normalizeBody])끼리 비교하므로 앞뒤 공백만 다른 제목은 같은 값이다.
     * 저장할 수 있는지([isSavable])는 부른 쪽이 먼저 확인한다.
     */
    fun patchOf(original: Memo, title: String?, body: String?, memoDate: LocalDate?): MemoPatch {
        val newTitle = normalizeTitle(title)
        val newBody = normalizeBody(body)
        return MemoPatch(
            title = if (newTitle != original.title) FieldChange.Set(newTitle) else FieldChange.Keep,
            body = if (newBody != original.body) FieldChange.Set(newBody) else FieldChange.Keep,
            memoDate = if (memoDate != original.memoDate) FieldChange.Set(memoDate) else FieldChange.Keep,
        )
    }
}
