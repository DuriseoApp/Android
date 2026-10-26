package com.duriseo.android.data.remote.call

import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.error.FieldError
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** 오류 객체 `{code, message, details, traceId}` (API.md 1.1). `message`는 로그용이다. */
@Serializable
internal data class ApiErrorBody(
    val code: String,
    val message: String? = null,
    val details: JsonElement? = null,
    val traceId: String? = null,
)

/**
 * `details`를 코드별로 읽는다 (API.md 1.1 표). 표에 정한 코드만 읽고 그 밖의 코드는 null이다.
 * 모양이 약속과 다르면 null (앱이 죽지 않게).
 */
internal fun parseErrorDetails(code: String, details: JsonElement?): ErrorDetails? {
    val obj = details as? JsonObject ?: return null
    return when (ErrorCode.fromCode(code)) {
        ErrorCode.VALIDATION_FAILED, ErrorCode.DATE_OUT_OF_RANGE -> {
            val fields = obj["fields"] as? JsonArray ?: return null
            ErrorDetails.Fields(fields.mapNotNull(::parseFieldError))
        }

        ErrorCode.INVITE_REISSUE_TOO_SOON, ErrorCode.INVITE_RATE_LIMITED -> {
            val seconds = (obj["retryAfterSeconds"] as? JsonPrimitive)?.intOrNull ?: return null
            ErrorDetails.RetryAfter(seconds)
        }

        else -> null
    }
}

private fun parseFieldError(element: JsonElement): FieldError? {
    val obj = element as? JsonObject ?: return null
    val field = (obj["field"] as? JsonPrimitive)?.contentOrNull ?: return null
    val reason = (obj["reason"] as? JsonPrimitive)?.contentOrNull ?: return null
    return FieldError(field = field, rawReason = reason)
}
