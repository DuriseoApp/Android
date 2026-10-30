package com.duriseo.android.core.common.social

import com.duriseo.android.domain.me.SocialProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialTokenResultTest {

    @Test
    fun `NFR-SEC-01 소셜 토큰 결과의 toString은 토큰 값을 찍지 않는다`() {
        val text = SocialTokenResult.Success(SocialProvider.GOOGLE, "google-id-token-secret").toString()

        assertFalse(text.contains("google-id-token-secret"))
        assertTrue(text.contains("GOOGLE"))
    }
}
