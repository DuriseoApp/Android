package com.duriseo.android.domain.error

/** `VALIDATION_FAILED`·`DATE_OUT_OF_RANGE`의 `details.fields` 항목 이유 (API.md 1.1). */
enum class FieldErrorReason {
    /** 필수값 없음 */
    REQUIRED,

    /** 길이 초과 */
    TOO_LONG,

    /** 형식·값이 틀림 */
    INVALID,

    /** 1950-01-01 ~ 2050-12-31 밖 */
    OUT_OF_RANGE;

    companion object {
        fun fromCode(code: String): FieldErrorReason? = entries.firstOrNull { it.name == code }
    }
}

/**
 * 입력 오류 한 건. [field]는 요청 필드 이름, [rawReason]은 서버가 보낸 문자열 그대로.
 * 모르는 이유 문자열은 [reason]이 null이다.
 */
data class FieldError(
    val field: String,
    val rawReason: String,
) {
    val reason: FieldErrorReason? get() = FieldErrorReason.fromCode(rawReason)
}

/** 오류 코드마다 달린 추가 정보 (API.md 1.1 표). 표에 없는 코드의 `details`는 null이다. */
sealed interface ErrorDetails {
    /** `VALIDATION_FAILED`, `DATE_OUT_OF_RANGE` */
    data class Fields(val fields: List<FieldError>) : ErrorDetails

    /** `INVITE_REISSUE_TOO_SOON`, `INVITE_RATE_LIMITED` */
    data class RetryAfter(val retryAfterSeconds: Int) : ErrorDetails
}
