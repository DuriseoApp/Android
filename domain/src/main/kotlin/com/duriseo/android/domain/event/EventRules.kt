package com.duriseo.android.domain.event

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 일정 대상을 보는 사람 기준으로 나눈 것 (라벨 나·상대·함께, FR-CAL-04 AC6) */
enum class EventTarget { ME, PARTNER, TOGETHER }

/**
 * 일정·캘린더 규칙 (PRD FR-CAL-01 AC6, FR-CAL-02, FR-CAL-03, 권한 매트릭스 7장).
 * 화면·리포지토리가 같은 규칙을 쓰도록 순수 함수로 둔다.
 */
object EventRules {
    /** 캘린더와 모든 일정 날짜 선택의 범위 (FR-CAL-01 AC6, D-74) */
    val MIN_DATE: LocalDate = LocalDate.of(1950, 1, 1)
    val MAX_DATE: LocalDate = LocalDate.of(2050, 12, 31)
    val MIN_MONTH: YearMonth = YearMonth.from(MIN_DATE)
    val MAX_MONTH: YearMonth = YearMonth.from(MAX_DATE)

    /** 제목 50자, 메모 1,000자 (NFR-LIMIT-01). 글자 수는 코드 포인트 (D-82) */
    const val TITLE_MAX = 50
    const val NOTE_MAX = 1000

    fun isSelectable(date: LocalDate): Boolean = !date.isBefore(MIN_DATE) && !date.isAfter(MAX_DATE)

    fun previousMonth(month: YearMonth): YearMonth? = month.minusMonths(1).takeIf { it >= MIN_MONTH }

    fun nextMonth(month: YearMonth): YearMonth? = month.plusMonths(1).takeIf { it <= MAX_MONTH }

    /** [month]를 캘린더 범위 안으로 당긴다 */
    fun clampMonth(month: YearMonth): YearMonth = when {
        month < MIN_MONTH -> MIN_MONTH
        month > MAX_MONTH -> MAX_MONTH
        else -> month
    }

    fun years(): List<Int> = (MIN_DATE.year..MAX_DATE.year).toList()

    /** 제목을 앞뒤 공백 없이 (D-89). 공백뿐이면 빈 문자열 = 제목 없음 */
    fun normalizeTitle(raw: String): String = raw.trim()

    fun isTitleValid(raw: String): Boolean = normalizeTitle(raw).isNotEmpty()

    /** [text]를 [max] 코드 포인트까지만 남긴다 (한도를 넘는 입력은 받지 않는다) */
    fun limit(text: String, max: Int): String {
        if (text.codePointCount(0, text.length) <= max) return text
        return text.substring(0, text.offsetByCodePoints(0, max))
    }

    /** 종료가 시작보다 이르면 저장하지 않는다. 같으면 저장 (FR-CAL-02 AC3) */
    fun isEndValid(time: EventTime): Boolean = when (time) {
        is EventTime.AllDay -> !time.end.isBefore(time.start)
        is EventTime.Timed -> !time.end.isBefore(time.start)
    }

    fun isInRange(time: EventTime): Boolean = when (time) {
        is EventTime.AllDay -> isSelectable(time.start) && isSelectable(time.end)
        is EventTime.Timed -> true
    }

    /** 일정이 걸친 첫 날짜와 마지막 날짜 (시간 일정은 [zone] 날짜). 시간 일정이 정각 0시에 끝나도 끝나는 날로 센다 (서버 규칙과 같다) */
    fun firstDay(event: Event, zone: ZoneId): LocalDate = when (val t = event.time) {
        is EventTime.AllDay -> t.start
        is EventTime.Timed -> t.start.atZone(zone).toLocalDate()
    }

    fun lastDay(event: Event, zone: ZoneId): LocalDate = when (val t = event.time) {
        is EventTime.AllDay -> t.end
        is EventTime.Timed -> t.end.atZone(zone).toLocalDate()
    }

    /** 여러 날 일정은 시작일부터 종료일까지 모든 날짜에 나온다 (FR-CAL-02 AC5) */
    fun occursOn(event: Event, day: LocalDate, zone: ZoneId): Boolean =
        !day.isBefore(firstDay(event, zone)) && !day.isAfter(lastDay(event, zone))

    /** 대상: `isTogether`면 함께, 아니면 작성자 = 대상 (D-34). 보는 사람 기준 (나·상대) */
    fun targetOf(event: Event, myId: String): EventTarget = when {
        event.isTogether -> EventTarget.TOGETHER
        event.authorId == myId -> EventTarget.ME
        else -> EventTarget.PARTNER
    }

    /** 수정·삭제: 작성자, 또는 대상이 함께인 일정이면 두 사람 (PRD 7장, FR-CAL-03 AC1·AC4·AC5) */
    fun canEdit(event: Event, myId: String): Boolean = event.authorId == myId || event.isTogether

    /** 대상(나·함께)은 작성자만 바꾼다 (D-52, R-13) */
    fun canChangeTarget(event: Event, myId: String): Boolean = event.authorId == myId
}
