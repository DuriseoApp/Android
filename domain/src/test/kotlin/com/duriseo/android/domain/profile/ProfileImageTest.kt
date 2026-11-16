package com.duriseo.android.domain.profile

import com.duriseo.android.domain.error.DataError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileImageTest {
    @Test
    fun `FR-AUTH-07 AC4 버전이 없으면 사진 모델이 null이라 요청하지 않는다`() {
        assertNull(profileImageOf("user-1", null))
        assertNull(profileImageOf("user-1", ""))
        assertNull(profileImageOf(null, "v1"))
        assertEquals(ProfileImage("user-1", "v1"), profileImageOf("user-1", "v1"))
    }

    @Test
    fun `NFR-FILE-01 오류 코드로 사진 크기 오류와 형식 오류를 가린다`() {
        fun api(code: String) = DataError.ApiError(code = code, details = null, traceId = null)
        assertEquals(PhotoError.TOO_LARGE, api("IMAGE_TOO_LARGE").photoErrorOrNull())
        assertEquals(PhotoError.TYPE_NOT_ALLOWED, api("IMAGE_TYPE_NOT_ALLOWED").photoErrorOrNull())
        assertNull(api("INTERNAL_ERROR").photoErrorOrNull())
        assertNull(api("모르는 코드").photoErrorOrNull())
        assertNull(DataError.Offline.photoErrorOrNull())
    }
}
