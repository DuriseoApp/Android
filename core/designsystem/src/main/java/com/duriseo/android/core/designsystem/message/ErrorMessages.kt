package com.duriseo.android.core.designsystem.message

import androidx.annotation.StringRes
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode

/**
 * 오류를 사용자에게 보여 줄 문구 리소스로 바꾼다 (스낵바). 서버 `message`는 쓰지 않는다 (API.md 1.1).
 *
 * - [DataError.Offline] -> "인터넷 연결이 필요해요" (NFR-OFF-02)
 * - [DataError.ApiError] -> 공통 오류 문구. 모르는 `code`도 같다 (ARCHITECTURE.md 3장).
 *   `code`별로 다른 안내가 필요한 화면(예: 입력 오류, 다른 기기 로그인)은 SCREENS.md의 문구를 따로 쓴다.
 */
@StringRes
fun DataError.toMessageRes(): Int = when (this) {
    DataError.Offline -> R.string.offline_required
    is DataError.ApiError -> R.string.error_common
}

/**
 * 세션이 서버에서 끊겼음을 뜻하는 코드. 세션 종료 흐름(`SessionManager`)이 이미 로컬을 정리하고 시작 화면으로 보내므로
 * 화면이 스낵바를 따로 띄우지 않는다 (API.md 1장). `TOKEN_EXPIRED`는 여기에 없다: 토큰 갱신이 5xx 같은 이유로 실패해
 * 요청 오류로 화면까지 올라오면 세션은 아직 살아 있으므로 공통 오류를 보여 준다 (API.md 3.1, D-88).
 */
private val SessionEndCodes = setOf(
    ErrorCode.SESSION_REPLACED,
    ErrorCode.SESSION_INVALID,
)

/**
 * [toMessageRes]와 같지만 세션 종료 코드(`SESSION_REPLACED`·`SESSION_INVALID`)는 "보여 줄 것 없음"이라 null이다
 * (SCREENS.md SCR-SET-01 "로그아웃·탈퇴 실패").
 */
@StringRes
fun DataError.toMessageResOrNull(): Int? =
    if (this is DataError.ApiError && knownCode in SessionEndCodes) null else toMessageRes()
