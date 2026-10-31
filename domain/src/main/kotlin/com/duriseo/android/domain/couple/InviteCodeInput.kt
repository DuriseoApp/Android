package com.duriseo.android.domain.couple

/**
 * 초대코드 입력 규칙 (SCR-COUPLE-03, FR-COUPLE-01.AC4).
 * 보드 `InviteEnter.dc.html`의 `clean()`과 같다: 대문자로 바꾸고, 영문 대문자·숫자만 남기고, 6자까지만 받는다.
 * 코드에 쓰지 않는 글자(0·O·1·I·L)는 막지 않는다. 서버가 `INVITE_NOT_FOUND`로 답한다.
 */
object InviteCodeInput {
    const val LENGTH = 6

    fun clean(raw: String): String =
        raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(LENGTH)

    fun isComplete(code: String): Boolean = code.length == LENGTH
}
