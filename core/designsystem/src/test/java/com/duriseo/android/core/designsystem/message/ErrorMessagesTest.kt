package com.duriseo.android.core.designsystem.message

import com.duriseo.android.core.designsystem.R
import com.duriseo.android.domain.error.DataError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorMessagesTest {

    private fun apiError(code: String) = DataError.ApiError(code = code, details = null, traceId = null)

    @Test
    fun `NFR-OFF-02 오프라인이면 인터넷 연결이 필요하다는 문구를 보여 준다`() {
        assertEquals(R.string.offline_required, DataError.Offline.toMessageResOrNull())
    }

    @Test
    fun `D-88 INTERNAL_ERROR와 모르는 코드는 공통 오류 문구를 보여 준다`() {
        assertEquals(R.string.error_common, apiError("INTERNAL_ERROR").toMessageResOrNull())
        assertEquals(R.string.error_common, apiError("SOMETHING_NEW").toMessageResOrNull())
    }

    @Test
    fun `D-88 소셜 로그인과 게스트 관련 오류 코드는 공통 오류 문구를 보여 준다`() {
        listOf("SOCIAL_TOKEN_INVALID", "GUEST_ONLY", "GUEST_NOT_ALLOWED").forEach { code ->
            assertEquals(code, R.string.error_common, apiError(code).toMessageResOrNull())
        }
    }

    @Test
    fun `API 1장 세션이 끊긴 코드는 보여 줄 문구가 없다`() {
        listOf("SESSION_REPLACED", "SESSION_INVALID", "TOKEN_EXPIRED").forEach { code ->
            assertNull(code, apiError(code).toMessageResOrNull())
        }
    }
}
