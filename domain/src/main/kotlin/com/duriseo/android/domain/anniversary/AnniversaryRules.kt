package com.duriseo.android.domain.anniversary

import com.duriseo.android.domain.event.EventRules
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 기념일 규칙 (PRD FR-ANNIV-03·04, D-104, D-122). 화면·리포지토리가 같은 규칙을 쓰도록 순수 함수로 둔다 */
object AnniversaryRules {
    /** 이름은 30자 (NFR-LIMIT-01, D-82). 글자 수는 코드 포인트로 센다 */
    const val NAME_MAX = 30

    /** D-N 배지를 강조하는 한계: 오늘(D-day)부터 30일 이내 (FR-ANNIV-04 AC1, `brand-100`/`brand-700`) */
    const val NEAR_DAYS = 30

    /** 날짜 선택 범위 1950-01-01 ~ 2050-12-31 (D-74). 일정과 같다 */
    val MIN_DATE: LocalDate = EventRules.MIN_DATE
    val MAX_DATE: LocalDate = EventRules.MAX_DATE

    fun isSelectable(date: LocalDate): Boolean = EventRules.isSelectable(date)

    /** 이름을 앞뒤 공백 없이 (D-104). 공백뿐이면 빈 문자열 = 이름 없음 */
    fun normalizeName(raw: String): String = raw.trim()

    fun isNameValid(raw: String): Boolean = normalizeName(raw).isNotEmpty()

    /** 이름 입력을 [NAME_MAX] 코드 포인트까지만 남긴다 (한도를 넘는 입력은 받지 않는다, D-122) */
    fun limitName(text: String): String = EventRules.limit(text, NAME_MAX)

    /** 이름 글자 수 표시("9 / 30")에 쓰는 코드 포인트 수 */
    fun nameLength(text: String): Int = text.codePointCount(0, text.length)

    /** 오늘이 기념일이거나 30일 이내면 강조한다 (D-N 배지) */
    fun isNear(daysLeft: Int): Boolean = daysLeft in 0..NEAR_DAYS
}

/** 함께한 지 D+N 계산 (FR-ANNIV-01, D-116, D-117) */
object DayCount {
    /**
     * 함께한 지 D+N의 N: `오늘 − 처음 만난 날 + 1` (처음 만난 날 = D+1). 시간대가 달라 계산값이 1보다 작으면 1이다 (D-116).
     * [today]는 기기 시계의 오늘이다.
     */
    fun of(startDate: LocalDate, today: LocalDate): Long =
        (ChronoUnit.DAYS.between(startDate, today) + 1).coerceAtLeast(1)

    /** 자동 기념일 카드의 D+N: `기념일 날짜 − 처음 만난 날 + 1` (100일 = D+100). 오늘과 무관하다 (D-117) */
    fun onDate(startDate: LocalDate, date: LocalDate): Long = ChronoUnit.DAYS.between(startDate, date) + 1
}
