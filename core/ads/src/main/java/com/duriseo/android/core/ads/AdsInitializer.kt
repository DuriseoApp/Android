package com.duriseo.android.core.ads

import com.duriseo.android.core.common.config.AdsConfig
import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.core.common.coroutines.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 시작 때 광고 SDK를 한 번만 초기화한다 (D-186 결정 5).
 * 광고가 꺼져 있으면(noAdsDebug, release인데 ID 없음) SDK를 건드리지 않는다 (FR-AD-04 AC4).
 * 광고 요청은 초기화가 끝나기를 기다리지 않아도 된다 (SDK 규칙).
 */
@Singleton
class AdsInitializer @Inject constructor(
    private val config: AdsConfig,
    private val sdk: MobileAdsSdk,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val started = AtomicBoolean(false)

    /** `MyApp.onCreate`에서 부른다. 여러 번 불러도 초기화는 한 번이다. */
    fun start() {
        if (!config.enabled) return
        if (!started.compareAndSet(false, true)) return
        scope.launch(ioDispatcher) { sdk.initialize() }
    }
}
