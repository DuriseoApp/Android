package com.duriseo.android.domain.couple

/** 초대코드 입력 제한 안내 (FR-COUPLE-02 AC4, SCREENS.md SCR-COUPLE-03 "입력 제한") */
object InviteRateLimit {
    private const val SECONDS_PER_MINUTE = 60

    /**
     * 안내 문구의 [N]분: `retryAfterSeconds`를 분 단위로 올림한 값, 최소 1 (API.md 4.4 12단계 표).
     * 오류를 받은 순간 한 번 정하고 화면에서 줄어들지 않는다.
     */
    fun minutesFor(retryAfterSeconds: Int): Int =
        maxOf(1, (retryAfterSeconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE)
}
