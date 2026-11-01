package com.duriseo.android.domain.error

/** 서버가 오류 [code]로 답한 것인가 (`message`는 보지 않는다, API.md 1.1) */
fun DataError.hasCode(code: ErrorCode): Boolean = this is DataError.ApiError && knownCode == code
