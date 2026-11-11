package com.duriseo.android.core.common.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Q-19 ① ③: 앱이 떠 있는 동안 기기 시간대가 바뀌면 시계가 바뀐 시간대를 돌려준다 */
class DeviceZoneClockTest {
    @Test
    fun `Q-19 시간대를 부를 때마다 지금 기기 시간대를 읽는다`() {
        var device = ZoneId.of("Asia/Seoul")
        val clock = DeviceZoneClock(zoneProvider = { device }, instantProvider = { Instant.parse("2026-11-10T16:00:00Z") })

        assertEquals(ZoneId.of("Asia/Seoul"), clock.zone)

        device = ZoneId.of("America/Los_Angeles")

        assertEquals(ZoneId.of("America/Los_Angeles"), clock.zone)
    }

    @Test
    fun `Q-19 오늘 날짜도 바뀐 시간대로 계산한다`() {
        var device = ZoneId.of("Asia/Seoul")
        val clock = DeviceZoneClock(zoneProvider = { device }, instantProvider = { Instant.parse("2026-11-10T16:00:00Z") })

        // UTC 16:00은 서울에서 다음 날 01:00
        assertEquals(LocalDate.of(2026, 11, 11), LocalDate.now(clock))

        device = ZoneId.of("America/Los_Angeles")

        assertEquals(LocalDate.of(2026, 11, 10), LocalDate.now(clock))
        assertEquals(LocalDateTime.of(2026, 11, 10, 8, 0), LocalDateTime.now(clock))
    }

    @Test
    fun `Q-19 withZone은 고른 시간대로 못 박은 시계를 돌려준다`() {
        val clock = DeviceZoneClock(zoneProvider = { ZoneId.of("Asia/Seoul") })

        assertEquals(ZoneId.of("UTC"), clock.withZone(ZoneId.of("UTC")).zone)
    }
}
