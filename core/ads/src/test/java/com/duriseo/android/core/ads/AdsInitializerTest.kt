package com.duriseo.android.core.ads

import com.duriseo.android.core.common.config.AdsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AdsInitializerTest {

    private class FakeSdk : MobileAdsSdk {
        var initializeCount = 0
        override fun initialize() {
            initializeCount++
        }
    }

    private fun config(enabled: Boolean) = AdsConfig(
        enabled = enabled,
        memoListNativeId = "memo",
        anniversaryListNativeId = "anniv",
        menuBottomBannerId = "banner",
    )

    @Test
    fun `FR-AD-04 AC4 광고가 꺼져 있으면 초기화하지 않는다`() = runTest {
        val sdk = FakeSdk()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val initializer = AdsInitializer(config(enabled = false), sdk, TestScope(dispatcher), dispatcher)

        initializer.start()
        advanceUntilIdle()

        assertEquals(0, sdk.initializeCount)
    }

    @Test
    fun `광고가 켜져 있으면 한 번만 초기화한다`() = runTest {
        val sdk = FakeSdk()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope: CoroutineScope = TestScope(dispatcher)
        val initializer = AdsInitializer(config(enabled = true), sdk, scope, dispatcher)

        initializer.start()
        initializer.start()
        advanceUntilIdle()

        assertEquals(1, sdk.initializeCount)
    }
}
