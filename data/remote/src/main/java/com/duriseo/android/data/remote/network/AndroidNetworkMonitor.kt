package com.duriseo.android.data.remote.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.duriseo.android.domain.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `ConnectivityManager` 콜백으로 연결 상태를 알려 준다 (NFR-OFF-02).
 * 인터넷 능력(`NET_CAPABILITY_INTERNET`)이 있는 네트워크가 하나라도 있으면 온라인이다.
 * Android API라서 단위 테스트가 없다. 실기기에서 비행기 모드로 확인한다.
 */
@Singleton
class AndroidNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isOnline: Flow<Boolean> = callbackFlow {
        val available = mutableSetOf<Network>()

        fun publish() {
            trySend(synchronized(available) { available.isNotEmpty() })
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                synchronized(available) { available.add(network) }
                publish()
            }

            override fun onLost(network: Network) {
                synchronized(available) { available.remove(network) }
                publish()
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        // 콜백은 상태가 바뀔 때만 오므로 처음 값은 지금 상태로 먼저 내보낸다
        trySend(isOnlineNow())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    override fun isOnlineNow(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
