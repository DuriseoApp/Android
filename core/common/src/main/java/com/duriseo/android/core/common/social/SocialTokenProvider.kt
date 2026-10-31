package com.duriseo.android.core.common.social

import android.app.Activity
import com.duriseo.android.domain.me.SocialProvider

/** 소셜 로그인 한 번의 결과 (unit-2.md G절) */
sealed interface SocialTokenResult {
    /**
     * [token]은 Google이면 ID 토큰, Kakao면 액세스 토큰이다 (API.md 4.2). 서버로 보내는 데만 쓰고
     * 저장하지 않는다. [toString]은 값을 찍지 않는다 (NFR-SEC-01).
     */
    data class Success(val provider: SocialProvider, val token: String) : SocialTokenResult {
        override fun toString(): String = "Success(provider=$provider, token=***)"
    }

    /** 사용자가 선택창·로그인 창을 닫았다. 안내 없이 그 화면에 머문다 (D-88) */
    data object Cancelled : SocialTokenResult

    /** SDK 실패(기기에 Google 계정이 없음 포함)나 설정값 없음. 공통 오류 안내 (D-88). [cause]는 디버그 로그용 */
    data class Failed(val cause: Throwable? = null) : SocialTokenResult
}

/**
 * Google·Kakao SDK로 서버에 보낼 소셜 토큰을 받는다. 시작 화면(`feature:auth`)과 계정 화면(`feature:settings`)이
 * 함께 쓰려고 `core:common`에 있다 (확인 필요 16). ViewModel 테스트는 가짜 구현을 쓴다.
 */
interface SocialTokenProvider {
    suspend fun requestToken(activity: Activity, provider: SocialProvider): SocialTokenResult
}
