package com.duriseo.android.core.common.legal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LegalLinksTest {

    @Test
    fun `D-154 FR-AUTH-08 시작 화면 두 링크는 각자의 주소를 가진다`() {
        assertEquals("https://juco-io.notion.site/41e5430a43d180f594d3c09751a82ebc", LegalLinks.TERMS_URL)
        assertEquals("https://juco-io.notion.site/41e5430a43d18054aba8fcbd4cf6c515", LegalLinks.PRIVACY_URL)
        assertNotEquals(LegalLinks.TERMS_URL, LegalLinks.PRIVACY_URL)
    }

    @Test
    fun `D-154 FR-SET-02 설정의 이용약관 개인정보처리방침 행은 개인정보처리방침 주소를 연다`() {
        assertEquals(LegalLinks.PRIVACY_URL, LegalLinks.SETTINGS_ROW_URL)
    }
}
