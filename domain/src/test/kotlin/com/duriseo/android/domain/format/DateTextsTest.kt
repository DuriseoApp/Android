package com.duriseo.android.domain.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DateTextsTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    /** 지금: 서울 2026-11-12 09:30 */
    private val clock: Clock = Clock.fixed(Instant.parse("2026-11-12T00:30:00Z"), seoul)

    @Test
    fun `NFR-TIME-02 작성일은 오늘 어제 올해 다른 해로 나눈다`() {
        // 오늘 09:12 -> 오전 9:12
        assertEquals(WrittenAt.Today(isPm = false, hour12 = 9, minute = 12), DateTexts.writtenAt(Instant.parse("2026-11-12T00:12:00Z"), clock))
        // 오늘 오후 3시 5분 -> 오후 3:05 (분은 두 자리로 보이는 값 5)
        assertEquals(WrittenAt.Today(isPm = true, hour12 = 3, minute = 5), DateTexts.writtenAt(Instant.parse("2026-11-12T06:05:00Z"), clock))
        // 어제
        assertEquals(WrittenAt.Yesterday, DateTexts.writtenAt(Instant.parse("2026-11-11T03:00:00Z"), clock))
        // 올해의 다른 날
        assertEquals(WrittenAt.ThisYear(10, 8), DateTexts.writtenAt(Instant.parse("2026-10-08T00:12:00Z"), clock))
        // 다른 해
        assertEquals(WrittenAt.OtherYear(2025, 10, 8), DateTexts.writtenAt(Instant.parse("2025-10-08T00:12:00Z"), clock))
        assertEquals(WrittenAt.OtherYear(2027, 1, 5), DateTexts.writtenAt(Instant.parse("2027-01-05T00:12:00Z"), clock))
    }

    @Test
    fun `NFR-TIME-02 자정과 정오는 12시로 표기한다`() {
        // 서울 00:00 = UTC 전날 15:00
        assertEquals(WrittenAt.Today(isPm = false, hour12 = 12, minute = 0), DateTexts.writtenAt(Instant.parse("2026-11-11T15:00:00Z"), clock))
        // 서울 12:00
        val noonClock = Clock.fixed(Instant.parse("2026-11-12T08:00:00Z"), seoul)
        assertEquals(WrittenAt.Today(isPm = true, hour12 = 12, minute = 0), DateTexts.writtenAt(Instant.parse("2026-11-12T03:00:00Z"), noonClock))
        // 서울 23:59 어제
        assertEquals(WrittenAt.Yesterday, DateTexts.writtenAt(Instant.parse("2026-11-11T14:59:00Z"), clock))
    }

    @Test
    fun `NFR-TIME-02 날짜는 기기 시간대로 가른다`() {
        // 같은 시각(UTC 10월 31일 15:30)이 서울에서는 11월 1일 00:30이다
        val created = Instant.parse("2026-10-31T15:30:00Z")
        val seoulNow = Clock.fixed(Instant.parse("2026-11-01T01:00:00Z"), seoul)
        val utcNow = Clock.fixed(Instant.parse("2026-11-01T01:00:00Z"), ZoneId.of("UTC"))

        assertEquals(WrittenAt.Today(isPm = false, hour12 = 12, minute = 30), DateTexts.writtenAt(created, seoulNow))
        assertEquals(WrittenAt.Yesterday, DateTexts.writtenAt(created, utcNow))
    }

    @Test
    fun `NFR-TIME-02 새해 첫날에는 지난해 마지막 날이 어제다`() {
        val newYear = Clock.fixed(Instant.parse("2026-12-31T16:00:00Z"), seoul) // 서울 2027-01-01 01:00

        assertEquals(WrittenAt.Yesterday, DateTexts.writtenAt(Instant.parse("2026-12-31T03:00:00Z"), newYear))
        assertEquals(WrittenAt.OtherYear(2026, 12, 30), DateTexts.writtenAt(Instant.parse("2026-12-30T03:00:00Z"), newYear))
    }
}
