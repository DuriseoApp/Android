package com.duriseo.android.core.common.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration

/**
 * 기기 시간대의 자정을 알리는 신호. 작성일의 "오늘"·"어제"가 자정이 지나도 낡지 않게 화면이 표기를 다시 계산하는 데 쓴다
 * (NFR-TIME-02, D-129). 주입받게 해서 테스트가 시계를 옮긴 뒤 직접 신호를 내보낸다(무한 반복이 가상 시간을 붙잡지 않게).
 */
interface MidnightTicker {
    /** 자정이 지날 때마다 한 번씩 내보낸다. 구독을 시작할 때는 내보내지 않는다(받는 쪽이 처음 값을 따로 계산한다) */
    fun ticks(): Flow<Unit>
}

/** 실제 구현: [clock] 기준 다음 자정까지 한 번 쉬고 내보내기를 되풀이한다. 분 단위로 돌지 않는다 */
class DeviceMidnightTicker(private val clock: Clock) : MidnightTicker {
    override fun ticks(): Flow<Unit> = flow {
        while (true) {
            val now = clock.instant().atZone(clock.zone)
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(clock.zone)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(MIN_WAIT_MILLIS))
            emit(Unit)
        }
    }

    private companion object {
        const val MIN_WAIT_MILLIS = 1_000L
    }
}
