package com.duriseo.android.domain.error

/**
 * 서버 오류 코드 (API.md 5장, 표 순서 그대로). 앱은 오류의 `code`로만 분기한다 (API.md 1.1).
 *
 * 새 코드를 지어내지 않는다. 서버가 이 목록에 없는 코드를 보내면 [fromCode]가 null을 돌려주고,
 * 화면은 일반 오류로 처리한다 (ARCHITECTURE.md 3장).
 */
enum class ErrorCode {
    VALIDATION_FAILED,
    DATE_OUT_OF_RANGE,
    INVALID_TARGET,
    TOKEN_EXPIRED,
    SESSION_INVALID,
    SESSION_REPLACED,
    SOCIAL_TOKEN_INVALID,
    NOT_AUTHOR,
    GUEST_NOT_ALLOWED,
    GUEST_ONLY,
    NOT_FOUND,
    INVITE_NOT_FOUND,
    SOCIAL_NOT_REGISTERED,
    NO_IMPORT_PENDING,
    INVITE_EXPIRED,
    INVITE_USED,
    INVITE_SELF,
    INVITE_UNAVAILABLE,
    ALREADY_COUPLED,
    NOT_COUPLE,
    SOCIAL_ALREADY_USED,
    IMAGE_TOO_LARGE,
    IMAGE_TYPE_NOT_ALLOWED,
    INVITE_RATE_LIMITED,
    INVITE_REISSUE_TOO_SOON,
    INTERNAL_ERROR;

    /** 서버가 보내는 문자열. enum 이름과 같다. */
    val code: String get() = name

    companion object {
        /** 문자열을 [ErrorCode]로 해석한다. 모르는 값이면 null. */
        fun fromCode(code: String): ErrorCode? = entries.firstOrNull { it.name == code }
    }
}
