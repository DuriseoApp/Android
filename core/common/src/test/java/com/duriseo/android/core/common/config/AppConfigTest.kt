package com.duriseo.android.core.common.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {

    @Test
    fun `API 1장 서버 주소 뒤에 api v1 경로를 붙인 기본 주소를 만든다`() {
        val config = AppConfig(apiBaseUrl = "https://api.example.test", googleWebClientId = "", kakaoNativeAppKey = "")

        assertEquals("https://api.example.test/api/v1/", config.apiV1BaseUrl())
    }

    @Test
    fun `API 1장 서버 주소 끝의 슬래시와 공백은 정리하고 api v1 경로를 붙인다`() {
        val config = AppConfig(apiBaseUrl = " https://api.example.test/ ", googleWebClientId = "", kakaoNativeAppKey = "")

        assertEquals("https://api.example.test/api/v1/", config.apiV1BaseUrl())
    }

    @Test
    fun `서버 주소가 비어 있으면 빈 설정으로 표시한다`() {
        val empty = AppConfig(apiBaseUrl = "", googleWebClientId = "", kakaoNativeAppKey = "")
        val blank = AppConfig(apiBaseUrl = "  ", googleWebClientId = "", kakaoNativeAppKey = "")

        assertFalse(empty.isApiConfigured)
        assertNull(empty.apiV1BaseUrl())
        assertFalse(blank.isApiConfigured)
        assertNull(blank.apiV1BaseUrl())
    }

    @Test
    fun `NFR-SEC-01 설정값은 문자열 표현에 찍히지 않는다`() {
        val config = AppConfig(
            apiBaseUrl = "https://api.example.test",
            googleWebClientId = "google-client-id-sample",
            kakaoNativeAppKey = "kakao-key-sample",
        )

        val text = config.toString()

        assertTrue(config.isApiConfigured)
        assertFalse(text.contains("example"))
        assertFalse(text.contains("google-client-id-sample"))
        assertFalse(text.contains("kakao-key-sample"))
    }
}
