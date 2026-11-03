package com.duriseo.android.data.repository.me

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room의 내 정보(`me`)와 설정을 바꾸는 쓰기를 한 번에 하나씩 처리하는 자물쇠.
 * `GET /me` 저장(해제 감지 포함), 앱 초기화, `PATCH` 응답 반영이 모두 이것 아래에서 돈다.
 * **코루틴 Mutex는 재진입이 안 된다.** 락 안에서 같은 락을 다시 잡는 함수(`refreshMe`, `saveFresh` 등)를 부르면 멈춘다.
 * 락 안에서는 락 없는 버전(`...Locked`)만 부른다.
 */
@Singleton
class MeWriteLock @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
