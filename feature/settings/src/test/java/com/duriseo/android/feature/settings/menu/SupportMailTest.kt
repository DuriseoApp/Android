package com.duriseo.android.feature.settings.menu

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportMailTest {

    @Test
    fun `FR-SET-02 AC1 제목과 본문 세 줄을 채운다`() {
        val mail = supportMail(
            subject = "[둘이서 문의]",
            appVersionLine = "앱 버전: 1.0.0",
            androidVersionLine = "Android 버전: 15",
            deviceModelLine = "기기 모델: Google Pixel 8",
        )

        assertEquals("[둘이서 문의]", mail.subject)
        assertEquals("앱 버전: 1.0.0\nAndroid 버전: 15\n기기 모델: Google Pixel 8", mail.body)
    }

    @Test
    fun `FR-SET-02 AC1 기기 모델은 제조사와 모델을 붙이고 모델에 제조사가 있으면 모델만 쓴다`() {
        assertEquals("Google Pixel 8", deviceModelLabel("Google", "Pixel 8"))
        assertEquals("Samsung SM-S918N", deviceModelLabel("samsung", "SM-S918N"))
        assertEquals("Samsung Galaxy S23", deviceModelLabel("samsung", "Samsung Galaxy S23"))
        assertEquals("Pixel 8", deviceModelLabel("", "Pixel 8"))
    }
}
