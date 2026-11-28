package com.duriseo.android.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 광고 하나를 받는 일. 성공이면 광고를, 실패면 null을 한 번 돌려준다. */
fun interface AdRequester<T : Any> {
    fun request(slot: Int, onResult: (T?) -> Unit)
}

/**
 * 화면(ViewModel 범위)이 들고 있는 네이티브 광고 보관함 (FR-AD-04 AC1, D-186 결정 6).
 *
 * - 자리 번호마다 광고를 **한 번만** 받는다. 받는 중이거나 이미 받았거나 실패한 자리는 다시 요청하지 않는다.
 * - 받은 광고는 화면에 있는 동안 바꾸지 않는다 (Q-24 ②). 같은 광고를 두 자리에 쓰지 않는다.
 * - 받는 중과 실패는 [ads]에 없다. 화면은 없으면 자리를 그리지 않는다 (빈 틀·로딩·오류 안내 없음).
 * - 화면을 떠나면 [clear]가 모두 destroy한다. 그 뒤에 도착한 광고도 바로 destroy한다.
 *
 * SDK 콜백은 메인 스레드에서 온다. 순수 로직이라 SDK 타입 없이 JVM에서 테스트한다 (`NativeAdSlotStateTest`).
 *
 * @param destroyer 보관하던 광고를 놓는다 (`NativeAd.destroy()`)
 */
class NativeAdSlots<T : Any>(
    private val requester: AdRequester<T>,
    private val destroyer: (T) -> Unit,
) {
    private val lock = Any()
    private val requested = mutableSetOf<Int>()
    private var cleared = false

    private val _ads = MutableStateFlow<Map<Int, T>>(emptyMap())

    /** 받아서 보관 중인 광고 (자리 번호 -> 광고). 받지 못한 자리는 없다. */
    val ads: StateFlow<Map<Int, T>> = _ads.asStateFlow()

    /** 자리의 광고를 요청한다. 이미 요청한 자리는 아무것도 하지 않는다. */
    fun request(slot: Int) {
        synchronized(lock) {
            if (cleared || !requested.add(slot)) return
        }
        requester.request(slot) { ad -> onResult(slot, ad) }
    }

    private fun onResult(slot: Int, ad: T?) {
        if (ad == null) return
        val keep = synchronized(lock) {
            if (cleared || _ads.value.containsKey(slot)) {
                false
            } else {
                _ads.update { it + (slot to ad) }
                true
            }
        }
        if (!keep) destroyer(ad)
    }

    /** 화면을 떠날 때. 보관한 광고를 모두 destroy하고 더는 받지 않는다. */
    fun clear() {
        val toDestroy = synchronized(lock) {
            cleared = true
            val old = _ads.value.values.toList()
            _ads.value = emptyMap()
            old
        }
        toDestroy.forEach(destroyer)
    }
}
