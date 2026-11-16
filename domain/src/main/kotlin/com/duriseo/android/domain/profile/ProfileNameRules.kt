package com.duriseo.android.domain.profile

/**
 * 프로필 이름 규칙 (FR-AUTH-07 AC6, NFR-LIMIT-01, D-82). 글자 수는 유니코드 코드 포인트 단위로 세고,
 * 앞뒤 공백만 있는 이름은 빈 값으로 본다. 순수 Kotlin이라 Android 없이 테스트한다.
 */
object ProfileNameRules {
    /** 이름 최대 글자 수 (코드 포인트, NFR-LIMIT-01, D-56) */
    const val MAX = 10

    /** [text]를 [MAX] 코드 포인트까지만 남긴다 (10자에서 더 입력되지 않는다) */
    fun limit(text: String): String {
        if (text.codePointCount(0, text.length) <= MAX) return text
        return text.substring(0, text.offsetByCodePoints(0, MAX))
    }

    /** 글자 수 "N / 10"의 N (코드 포인트) */
    fun length(text: String): Int = text.codePointCount(0, text.length)

    /** 서버에 보내는 값: 앞뒤 공백을 지운 이름 (D-82) */
    fun normalize(text: String): String = text.trim()

    /** 앞뒤 공백을 지운 뒤 비면 저장할 수 없다 */
    fun isBlank(text: String): Boolean = normalize(text).isEmpty()
}
