package com.duriseo.android.data.repository.online

import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult

/**
 * 쓰기(작성·수정·삭제·고정)는 온라인에서만 한다 (NFR-OFF-02, D-28).
 * 오프라인이면 [block]을 실행하지 않고(= API를 부르지 않고) [DataError.Offline]을 돌려준다.
 * 화면은 `Offline`을 "인터넷 연결이 필요해요" 안내로 보여 준다.
 *
 * 낙관적 갱신은 하지 않는다. [block] 안에서 서버가 성공한 뒤에 Room을 바꾼다 (ARCHITECTURE.md 2장).
 */
suspend fun <T> NetworkMonitor.runOnlineWrite(
    block: suspend () -> DataResult<T>,
): DataResult<T> =
    if (isOnlineNow()) block() else DataResult.Failure(DataError.Offline)
