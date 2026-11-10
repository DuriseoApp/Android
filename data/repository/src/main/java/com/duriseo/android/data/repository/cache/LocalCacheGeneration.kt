package com.duriseo.android.data.repository.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 캐시(Room) 세대. 캐시를 지우는 쪽(로그아웃·탈퇴·세션 끊김, 해제 초기화, 공간 변경, 가져오기 결정)이 세대를 올리고,
 * 서버 응답을 저장하는 쪽은 **요청 전에** 적어 둔 세대가 지금도 같을 때만 저장한다.
 * 지운 뒤에 늦게 도착한 이전 응답이 캐시를 되살리는 것을 막는다.
 *
 * 확인과 저장, 올림과 지우기는 같은 [Mutex] 아래라 서로 끼어들지 못한다 ([saveIfCurrent], [clearing]).
 *
 * 락 순서 (바깥 -> 안쪽): `MeWriteLock` -> 목록별 락(`upcomingLock` 등) -> 이 클래스의 Mutex.
 * 이 Mutex는 맨 안쪽이라, 그 안(블록)에서는 Room·설정 작업만 하고 다른 락을 잡지 않는다.
 * 코루틴 Mutex는 재진입이 안 되므로 [clearing]·[saveIfCurrent]의 블록 안에서 이 둘을 다시 부르지 않는다.
 */
@Singleton
class LocalCacheGeneration @Inject constructor() {
    private val generation = AtomicLong(0)
    private val mutex = Mutex()

    /** 요청을 보내기 전에 적어 둘 현재 세대 */
    fun current(): Long = generation.get()

    /**
     * 진행 중인 요청을 먼저 무효로 만든다 (락을 잡지 않는다). 지우기가 락을 기다리는 동안에도
     * 이 호출 전에 시작한 응답이 저장되지 않게 하려고 지우기 맨 앞에서 부른다.
     */
    fun invalidate() {
        generation.incrementAndGet()
    }

    /** 세대를 올리고 [block](지우기)을 한 번에 한다. 저장 확인이 올림과 지우기 사이에 끼어들지 못한다 */
    suspend fun <T> clearing(block: suspend () -> T): T = mutex.withLock {
        generation.incrementAndGet()
        block()
    }

    /** [generation]이 지금 세대와 같을 때만 [block](저장)을 한다. 저장했으면 true */
    suspend fun saveIfCurrent(generation: Long, block: suspend () -> Unit): Boolean = mutex.withLock {
        if (generation != this.generation.get()) {
            false
        } else {
            block()
            true
        }
    }
}
