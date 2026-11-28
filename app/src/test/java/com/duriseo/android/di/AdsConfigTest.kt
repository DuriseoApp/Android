package com.duriseo.android.di

import com.duriseo.android.BuildConfig
import com.duriseo.android.core.common.config.AdsConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D-186: 빌드 타입별 광고 설정. 현재 빌드 타입의 `BuildConfig` 값으로 만든 [AdsConfig]를 확인한다. */
class AdsConfigTest {

    private fun fromBuildConfig(): AdsConfig = AdsConfig.create(
        enabled = BuildConfig.ADS_ENABLED,
        memoListNativeId = BuildConfig.ADMOB_MEMO_LIST_NATIVE_ID,
        anniversaryListNativeId = BuildConfig.ADMOB_ANNIVERSARY_LIST_NATIVE_ID,
        menuBottomBannerId = BuildConfig.ADMOB_MENU_BOTTOM_BANNER_ID,
    )

    @Test
    fun `noAdsDebug는 광고가 꺼져 있다`() {
        // 단위 테스트는 빌드 타입마다 도니 noAdsDebug로 돌릴 때(testNoAdsDebugUnitTest) 이 갈래를 탄다.
        // debug 빌드에서는 값이 true인 쪽을 확인하고, 꺼진 설정이 광고를 쓰지 않는지는 아래 Disabled로 확인한다.
        val config = fromBuildConfig()
        if (BuildConfig.BUILD_TYPE == "noAdsDebug") {
            assertFalse(config.enabled)
        } else if (BuildConfig.BUILD_TYPE == "debug") {
            assertTrue(config.enabled)
        }
        assertFalse(AdsConfig.create(false, "a", "b", "c").enabled)
        assertFalse(AdsConfig.Disabled.enabled)
    }

    @Test
    fun `debug 설정은 Google 테스트 단위 ID로 광고가 켜진다`() {
        if (BuildConfig.BUILD_TYPE != "debug") return
        val config = fromBuildConfig()
        assertTrue(config.enabled)
        assertEquals("ca-app-pub-3940256099942544/2247696110", config.memoListNativeId)
        assertEquals("ca-app-pub-3940256099942544/2247696110", config.anniversaryListNativeId)
        assertEquals("ca-app-pub-3940256099942544/9214589741", config.menuBottomBannerId)
    }

    @Test
    fun `release 값이 비면 광고를 끈다`() {
        assertFalse(AdsConfig.create(true, "", "b", "c").enabled)
        assertFalse(AdsConfig.create(true, "a", " ", "c").enabled)
        assertFalse(AdsConfig.create(true, "a", "b", "").enabled)
        assertTrue(AdsConfig.create(true, "a", "b", "c").enabled)
    }

    @Test
    fun `AdsConfig toString은 ID를 찍지 않는다`() {
        val text = AdsConfig.create(true, "memo-id", "anniv-id", "banner-id").toString()
        assertFalse(text.contains("memo-id"))
        assertFalse(text.contains("anniv-id"))
        assertFalse(text.contains("banner-id"))
    }
}
