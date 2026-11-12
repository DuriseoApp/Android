package com.duriseo.android.domain.format

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * 작성일 표기의 계산 결과 (PRD NFR-TIME-02, D-129). 문구 조각(오늘·어제·오전·오후·년월일)은 화면 쪽이
 * `strings.xml`로 붙인다 (`core:designsystem`의 `DateTextFormat`). 계산과 문자열을 나눠 계산을 순수 함수로 테스트한다.
 */
sealed interface WrittenAt {
    /** 오늘: "오늘 오전 9:12". [hour12]는 1~12 (0시와 12시는 12), [minute]는 0~59 */
    data class Today(val isPm: Boolean, val hour12: Int, val minute: Int) : WrittenAt

    /** 어제: "어제" */
    data object Yesterday : WrittenAt

    /** 올해(오늘·어제가 아닌 날): "10월 8일" */
    data class ThisYear(val month: Int, val day: Int) : WrittenAt

    /** 다른 해: "2025년 10월 8일" */
    data class OtherYear(val year: Int, val month: Int, val day: Int) : WrittenAt
}

/** 메모 날짜 칩의 계산 결과 (D-129). [year]가 null이면 올해라서 연도를 붙이지 않는다: "10월 17일 (토)", 아니면 "2027년 10월 17일 (일)" */
data class MemoDateChip(val year: Int?, val month: Int, val day: Int, val dayOfWeek: DayOfWeek)

/** 날짜·시각 표기 규칙 (NFR-TIME-02, D-129). 오늘·어제·올해는 **기기 시간대**의 날짜로 가른다 */
object DateTexts {
    /**
     * 작성일 표기: 오늘이면 시각, 어제면 "어제", 올해면 월일, 다른 해면 연월일.
     * 날짜는 [clock]의 시간대(기기 시간대)로 바꿔 가르고 오늘은 `clock.instant()`다.
     */
    fun writtenAt(createdAt: Instant, clock: Clock): WrittenAt {
        val zone = clock.zone
        val local = createdAt.atZone(zone)
        val today = clock.instant().atZone(zone).toLocalDate()
        val date = local.toLocalDate()
        return when {
            date == today -> {
                val h = local.hour
                WrittenAt.Today(isPm = h >= 12, hour12 = if (h % 12 == 0) 12 else h % 12, minute = local.minute)
            }

            date == today.minusDays(1) -> WrittenAt.Yesterday
            date.year == today.year -> WrittenAt.ThisYear(date.monthValue, date.dayOfMonth)
            else -> WrittenAt.OtherYear(date.year, date.monthValue, date.dayOfMonth)
        }
    }

    /** 메모 날짜 칩 표기: 올해가 아니면 연도를 붙인다. [today]는 기기 시계의 오늘이다 */
    fun memoDateChip(date: LocalDate, today: LocalDate): MemoDateChip = MemoDateChip(
        year = date.year.takeIf { it != today.year },
        month = date.monthValue,
        day = date.dayOfMonth,
        dayOfWeek = date.dayOfWeek,
    )

    fun memoDateChip(date: LocalDate, clock: Clock): MemoDateChip = memoDateChip(date, LocalDate.now(clock))
}
