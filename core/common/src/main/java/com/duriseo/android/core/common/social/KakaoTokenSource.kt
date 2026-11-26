package com.duriseo.android.core.common.social

import android.app.Activity
import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.domain.me.SocialProvider
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ApiError
import com.kakao.sdk.common.model.AuthError
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
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
    private val sdkState: KakaoSdkState,
) {
    suspend fun request(activity: Activity): SocialTokenResult {
        notReady()?.let { return it }
        return try {
            startLogin(activity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // SDK가 던진 예외(초기화 누락 등)도 앱을 죽이지 않고 실패로 돌려준다 (공통 오류로 끝난다)
            Logger.w(SOCIAL_LOG_TAG, "Kakao: SDK 호출 중 예외 ${e.javaClass.simpleName}")
            SocialTokenResult.Failed(e)
        }
    }

    /** SDK를 초기화하지 않은 빌드(앱 키 없음, CI 등)면 SDK를 부르면 죽으므로 부르지 않고 실패를 돌려준다. 부를 수 있으면 null */
    internal fun notReady(): SocialTokenResult.Failed? {
        if (sdkState.isUsable(config)) return null
        Logger.w(SOCIAL_LOG_TAG, "Kakao: SDK를 초기화하지 않았어요 (local.properties duriseo.kakao.nativeAppKey 확인)")
        return SocialTokenResult.Failed(IllegalStateException("카카오 SDK가 초기화되지 않았어요"))
    }

    private suspend fun startLogin(activity: Activity): SocialTokenResult {
        val client = UserApiClient.instance
        val talkAvailable = client.isKakaoTalkLoginAvailable(activity)
        Logger.i(SOCIAL_LOG_TAG, "Kakao: 로그인 시작 (카카오톡 로그인 가능=$talkAvailable)")
        if (talkAvailable) {
            val viaTalk = login("카카오톡") { callback -> client.loginWithKakaoTalk(activity, callback = callback) }
            // 취소는 그대로 끝내고, 카카오톡 로그인이 실패한 경우만 계정 로그인으로 다시 한다
            if (viaTalk !is SocialTokenResult.Failed) return viaTalk
            Logger.i(SOCIAL_LOG_TAG, "Kakao: 카카오톡 로그인이 실패해 카카오 계정(웹) 로그인으로 다시 해요")
        }
        return login("카카오 계정") { callback -> client.loginWithKakaoAccount(activity, callback = callback) }
    }

    private suspend fun login(
        via: String,
        start: ((OAuthToken?, Throwable?) -> Unit) -> Unit,
    ): SocialTokenResult = suspendCancellableCoroutine { continuation ->
        start { token, error ->
            val result = when {
                error != null ->
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        Logger.i(SOCIAL_LOG_TAG, "Kakao($via): 사용자가 취소")
                        SocialTokenResult.Cancelled
                    } else {
                        // 예: "Android keyHash validation failed" → Kakao Developers 플랫폼에 이 기기의 키 해시(앱 시작 로그 참고) 등록,
                        // KOE101 → 네이티브 앱 키 확인, KOE006·KOE009 → 패키지 이름·플랫폼 등록 확인
                        Logger.w(SOCIAL_LOG_TAG, "Kakao($via): 실패 ${describe(error)}", error)
                        SocialTokenResult.Failed(error)
                    }

                token != null -> {
                    Logger.i(SOCIAL_LOG_TAG, "Kakao($via): 액세스 토큰 받음")
                    SocialTokenResult.Success(SocialProvider.KAKAO, token.accessToken)
                }
                else -> {
                    Logger.w(SOCIAL_LOG_TAG, "Kakao($via): 오류도 토큰도 없음")
                    SocialTokenResult.Failed(IllegalStateException("카카오가 토큰을 주지 않았어요"))
                }
            }
            if (continuation.isActive) continuation.resume(result)
        }
    }

    /** 카카오 오류의 종류·원인·상태 코드를 한 줄로 (Kakao Developers 문서의 오류 코드와 맞춰 본다) */
    private fun describe(error: Throwable): String = when (error) {
        is ClientError -> "ClientError reason=${error.reason} message=${error.message}"
        is AuthError ->
            "AuthError status=${error.statusCode} reason=${error.reason} " +
                "error=${error.response.error} description=${error.response.errorDescription}"
        is ApiError -> "ApiError status=${error.statusCode} reason=${error.reason} code=${error.response.code} message=${error.response.msg}"
        else -> "${error::class.simpleName}: ${error.message}"
    }
}
