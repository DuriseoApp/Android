package com.duriseo.android.domain.network

import kotlinx.coroutines.flow.Flow

/**
 * 인터넷 연결 여부 (NFR-OFF-02, D-28).
 * 구현(`AndroidNetworkMonitor`)은 `data:remote`에 있다. domain은 Android를 모른다.
 */
interface NetworkMonitor {
    /** 연결 상태가 바뀔 때마다 내보낸다. 구독하면 현재 상태를 바로 내보낸다. */
    val isOnline: Flow<Boolean>

    /** 지금 연결돼 있는지. 쓰기를 보내기 전에 확인한다. */
    fun isOnlineNow(): Boolean
}
