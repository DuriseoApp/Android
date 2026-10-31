package com.duriseo.android.domain.me

import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

/** 내 정보. 화면은 Room만 구독한다 (ARCHITECTURE.md 2장) */
interface MeRepository {
    /** Room의 내 정보. 아직 받은 적이 없으면 null */
    fun observeMe(): Flow<Me?>

    /** `GET /me` -> Room. 오프라인이면 요청 없이 `Offline`이고 저장된 값은 그대로 (NFR-OFF-01) */
    suspend fun refreshMe(): DataResult<Unit>

    /**
     * 기기 시간대 [zoneId]가 저장된 `Me.timeZone`과 다르면 `PATCH /me {timeZone}` -> Room (NFR-TIME-01).
     * 같거나 저장된 내 정보가 없으면 보내지 않는다. 오프라인이면 요청 없이 `Offline`. 호출한 쪽은 실패를 조용히 넘긴다.
     */
    suspend fun syncTimeZone(zoneId: String): DataResult<Unit>
}
