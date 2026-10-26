package com.duriseo.android.core.designsystem.message

import androidx.annotation.StringRes
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.domain.error.DataError

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
