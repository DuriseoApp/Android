package com.duriseo.android.core.common.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * 시각은 시스템 시계, 시간대는 **부를 때마다 지금 기기 시간대**를 읽는 `Clock` (Q-19 ①③).
 * `Clock.systemDefaultZone()`은 만들어질 때의 시간대를 붙들고 있어서, 앱이 떠 있는 동안 기기 시간대가 바뀌어도
 * `clock.zone`이 옛 값을 돌려줬다. 이 시계는 [zoneProvider]를 매번 불러 그 문제가 없다.
 *
 * @param zoneProvider 지금 기기 시간대. 기본은 `ZoneId.systemDefault()` (테스트는 가짜 값을 넣는다)
 * @param instantProvider 지금 시각. 기본은 `Instant.now()`
 */
class DeviceZoneClock(
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val instantProvider: () -> Instant = { Instant.now() },
) : Clock() {
    override fun getZone(): ZoneId = zoneProvider()

    /** 시간대를 못 박은 시계를 돌려준다 (`LocalDate.now(clock.withZone(...))`처럼 직접 고를 때) */
    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)

    override fun instant(): Instant = instantProvider()
}
