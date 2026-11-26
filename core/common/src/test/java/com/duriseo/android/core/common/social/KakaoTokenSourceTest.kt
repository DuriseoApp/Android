package com.duriseo.android.core.common.social

import com.duriseo.android.core.common.config.AppConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KakaoTokenSourceTest {

    private fun config(key: String) = AppConfig(apiBaseUrl = "", googleWebClientId = "", kakaoNativeAppKey = key)

    @Test
    fun `FR-AUTH-01 SDK를 초기화하지 않았으면 실패를 돌려준다`() {
        // 앱 키가 있어도 초기화가 끝나지 않았으면 SDK를 부르지 않는다
        val notInitialized = KakaoTokenSource(config("key"), KakaoSdkState())
        assertNotNull(notInitialized.notReady())

        // 앱 키가 비어 초기화를 건너뛴 빌드
        val state = KakaoSdkState().apply { markInitialized() }
        assertTrue(KakaoTokenSource(config(""), state).notReady() is SocialTokenResult.Failed)
    }

    @Test
    fun `FR-AUTH-01 키가 있고 초기화가 끝났으면 SDK를 부를 수 있다`() {
        val state = KakaoSdkState().apply { markInitialized() }

        assertNull(KakaoTokenSource(config("key"), state).notReady())
        assertTrue(state.isUsable(config("key")))
        assertFalse(state.isUsable(config(" ")))
    }
}
