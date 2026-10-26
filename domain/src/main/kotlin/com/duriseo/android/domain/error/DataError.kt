package com.duriseo.android.domain.error

/**
 * 데이터 계층이 화면에 돌려주는 오류 (ARCHITECTURE.md 3장).
 *
 * 서버 `message`는 담지 않는다. 화면에 쓰지 않고 로그용이기 때문이다 (API.md 1.1).
 */
sealed interface DataError {

    /**
     * 서버가 오류 객체로 답한 경우. 모든 오류는 HTTP 상태와 `{code, message, details, traceId}`다 (API.md 1.1).
     *
     * @property code 서버가 보낸 오류 코드 문자열 그대로
     * @property details 코드별 추가 정보. 없으면 null
     * @property traceId 서버 로그를 찾는 값. 디버그 로그에만 남긴다
     */
    data class ApiError(
        val code: String,
        val details: ErrorDetails?,
        val traceId: String?,
    ) : DataError {
        /** [code]를 [ErrorCode]로 해석한 값. 모르는 코드면 null (일반 오류로 처리한다). */
        val knownCode: ErrorCode? get() = ErrorCode.fromCode(code)
    }

    /** 연결 없음, 시간 초과 같은 네트워크 오류 (NFR-OFF-02). */
    data object Offline : DataError
}
