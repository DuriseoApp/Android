package com.duriseo.android.domain.auth

/** 세션이 서버에서 끊긴 이유 (API.md 1장, 5장) */
enum class SessionEndReason {
    /** 401 `SESSION_REPLACED`: 다른 기기 로그인으로 끊김. 안내 스낵바를 보여 준다 (D-51) */
    REPLACED,

    /** 401 `SESSION_INVALID`: 로그아웃·탈퇴·만료·재사용. 안내 없이 시작 화면으로 */
    INVALID,
}

/**
 * 세션 종료를 알린다. `data:remote`의 Authenticator가 부르고 구현(`SessionManager`)은 `data:repository`에 있다.
 * OkHttp 스레드에서 부르므로 기다리지 않는다.
 */
interface SessionEndNotifier {
    fun end(reason: SessionEndReason)
}

/**
 * 기기의 소셜 SDK 로그인 상태를 지운다: Kakao SDK 로그아웃, Credential Manager 자격 증명 상태 지우기 (API.md 1장).
 * 실패는 구현이 삼킨다. Android 구현은 `core:common`.
 */
interface SocialSessionCleaner {
    suspend fun clear()
}
