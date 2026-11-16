package com.duriseo.android.data.remote.image

import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.domain.profile.ProfileImage
import com.duriseo.android.domain.profile.profileImageOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileImageUrlTest {
    private fun config(url: String) = AppConfig(apiBaseUrl = url, googleWebClientId = "", kakaoNativeAppKey = "")

    @Test
    fun `FR-AUTH-07 AC3 버전이 있으면 v 쿼리를 붙이고 없으면 null이다`() {
        val withVersion = profileImageOf("user-1", "ver-9")
        assertEquals(
            "https://dev.example.com/api/v1/users/user-1/profile-image?v=ver-9",
            profileImageUrl(config("https://dev.example.com"), withVersion!!),
        )
        // 끝 슬래시가 있는 서버 주소도 같다
        assertEquals(
            "https://dev.example.com/api/v1/users/user-1/profile-image?v=ver-9",
            profileImageUrl(config("https://dev.example.com/"), ProfileImage("user-1", "ver-9")),
        )
        // 버전이 없으면 사진 모델이 없어 주소를 만들 일도 없다 (요청하지 않음)
        assertNull(profileImageOf("user-1", null))
    }

    @Test
    fun `FR-AUTH-07 AC3 사진이 바뀌어 버전이 달라지면 주소(캐시 키)도 달라진다`() {
        val a = profileImageUrl(config("https://dev.example.com"), ProfileImage("user-1", "ver-1"))
        val b = profileImageUrl(config("https://dev.example.com"), ProfileImage("user-1", "ver-2"))
        assertEquals(false, a == b)
    }

    @Test
    fun `서버 주소가 비어 있으면 주소를 만들지 않는다`() {
        assertNull(profileImageUrl(config(""), ProfileImage("user-1", "ver-1")))
    }
}
