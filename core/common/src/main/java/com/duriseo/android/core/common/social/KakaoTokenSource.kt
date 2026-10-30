package com.duriseo.android.core.common.social

import android.app.Activity
import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.domain.me.SocialProvider
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 카카오 로그인: 카카오톡이 있으면 카카오톡으로, 사용자가 취소하면 취소, 그 밖의 실패면 카카오 계정(웹)으로 다시 (unit-2.md G절).
 * 서버에는 `OAuthToken.accessToken`을 보낸다 (API.md 4.2). 콜백 API를 코루틴으로 감싼다.
 * 실기기에서만 확인할 수 있다 (unit-2.md 위험 2).
 */
@Singleton
class KakaoTokenSource @Inject constructor(
    private val config: AppConfig,
) {
    suspend fun request(activity: Activity): SocialTokenResult {
        if (config.kakaoNativeAppKey.isBlank()) {
            // SDK를 초기화하지 않은 빌드(CI). SDK를 부르면 죽으므로 여기서 막는다
            return SocialTokenResult.Failed(IllegalStateException("카카오 네이티브 앱 키가 설정되지 않았어요"))
        }

        val client = UserApiClient.instance
        if (client.isKakaoTalkLoginAvailable(activity)) {
            val viaTalk = login { callback -> client.loginWithKakaoTalk(activity, callback = callback) }
            // 취소는 그대로 끝내고, 카카오톡 로그인이 실패한 경우만 계정 로그인으로 다시 한다
            if (viaTalk !is SocialTokenResult.Failed) return viaTalk
        }
        return login { callback -> client.loginWithKakaoAccount(activity, callback = callback) }
    }

    private suspend fun login(
        start: ((OAuthToken?, Throwable?) -> Unit) -> Unit,
    ): SocialTokenResult = suspendCancellableCoroutine { continuation ->
        start { token, error ->
            val result = when {
                error != null ->
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        SocialTokenResult.Cancelled
                    } else {
                        SocialTokenResult.Failed(error)
                    }

                token != null -> SocialTokenResult.Success(SocialProvider.KAKAO, token.accessToken)
                else -> SocialTokenResult.Failed(IllegalStateException("카카오가 토큰을 주지 않았어요"))
            }
            if (continuation.isActive) continuation.resume(result)
        }
    }
}
