package com.duriseo.android.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorCodeTest {

    /** API.md 5장 표의 code 열, 표 순서 그대로 */
    private val apiCodes = listOf(
        "VALIDATION_FAILED",
        "DATE_OUT_OF_RANGE",
        "INVALID_TARGET",
        "TOKEN_EXPIRED",
        "SESSION_INVALID",
        "SESSION_REPLACED",
        "SOCIAL_TOKEN_INVALID",
        "NOT_AUTHOR",
        "GUEST_NOT_ALLOWED",
        "GUEST_ONLY",
        "NOT_FOUND",
        "INVITE_NOT_FOUND",
        "SOCIAL_NOT_REGISTERED",
        "NO_IMPORT_PENDING",
        "INVITE_EXPIRED",
        "INVITE_USED",
        "INVITE_SELF",
        "INVITE_UNAVAILABLE",
        "ALREADY_COUPLED",
        "NOT_COUPLE",
        "SOCIAL_ALREADY_USED",
        "IMAGE_TOO_LARGE",
        "IMAGE_TYPE_NOT_ALLOWED",
        "INVITE_RATE_LIMITED",
        "INVITE_REISSUE_TOO_SOON",
        "INTERNAL_ERROR",
    )

    @Test
    fun `API 5장 오류 코드 문자열을 모두 ErrorCode로 해석한다`() {
        assertEquals(26, apiCodes.size)
        apiCodes.forEach { code ->
            val resolved = ErrorCode.fromCode(code)
            assertEquals(code, resolved?.code)
        }
        assertEquals(apiCodes, ErrorCode.entries.map { it.code })
    }

    @Test
    fun `API 1_1 모르는 code는 알려지지 않은 코드로 두고 원래 문자열을 남긴다`() {
        val error = DataError.ApiError(code = "SOMETHING_NEW", details = null, traceId = null)

        assertNull(error.knownCode)
        assertEquals("SOMETHING_NEW", error.code)
    }

    @Test
    fun `API 1_1 이유 문자열 네 가지를 FieldErrorReason으로 해석한다`() {
        assertEquals(FieldErrorReason.REQUIRED, FieldError("title", "REQUIRED").reason)
        assertEquals(FieldErrorReason.TOO_LONG, FieldError("nickname", "TOO_LONG").reason)
        assertEquals(FieldErrorReason.INVALID, FieldError("end", "INVALID").reason)
        assertEquals(FieldErrorReason.OUT_OF_RANGE, FieldError("start", "OUT_OF_RANGE").reason)
        assertNull(FieldError("x", "UNKNOWN").reason)
    }
}
