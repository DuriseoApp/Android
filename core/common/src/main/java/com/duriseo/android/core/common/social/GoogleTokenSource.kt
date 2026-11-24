package com.duriseo.android.core.common.social

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.domain.me.SocialProvider
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google 로그인: Credential Manager "Google로 로그인" 버튼 흐름으로 ID 토큰을 받는다 (API.md 4.2).
 * `serverClientId`는 웹 클라이언트 ID이고, 서버가 ID 토큰의 `aud`를 이 값과 비교한다.
 * 실기기에서만 확인할 수 있다 (unit-2.md 위험 2).
 */
@Singleton
class GoogleTokenSource @Inject constructor(
    private val config: AppConfig,
) {
    suspend fun request(activity: Activity): SocialTokenResult {
        val serverClientId = config.googleWebClientId
        if (serverClientId.isBlank()) {
            // 설정값이 없는 빌드(CI). 기능만 동작하지 않는다
            Logger.w(SOCIAL_LOG_TAG, "Google: local.properties duriseo.google.webClientId가 비어 있어 로그인할 수 없어요")
            return SocialTokenResult.Failed(IllegalStateException("Google 웹 클라이언트 ID가 설정되지 않았어요"))
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()

        Logger.i(SOCIAL_LOG_TAG, "Google: Credential Manager 요청 시작")
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                Logger.i(SOCIAL_LOG_TAG, "Google: ID 토큰 받음")
                SocialTokenResult.Success(SocialProvider.GOOGLE, idToken)
            } else {
                Logger.w(SOCIAL_LOG_TAG, "Google: 예상하지 못한 자격 증명 종류 ${credential.type}")
                SocialTokenResult.Failed(IllegalStateException("예상하지 못한 자격 증명 종류예요"))
            }
        } catch (e: GetCredentialCancellationException) {
            if (e.isReauthFailure()) {
                // 이름은 취소지만 사용자가 취소한 게 아니다. 계정을 고른 직후 Google이 거부한 것으로,
                // 대개 Android OAuth 클라이언트(패키지 이름·SHA-1)가 등록되지 않았을 때다. 공통 오류로 알린다
                Logger.w(SOCIAL_LOG_TAG, "Google: 실패 (취소로 온 재인증 실패) ${e.type}: ${e.errorMessage}", e)
                SocialTokenResult.Failed(e)
            } else {
                Logger.i(SOCIAL_LOG_TAG, "Google: 사용자가 취소 (${e.type}: ${e.errorMessage})")
                SocialTokenResult.Cancelled
            }
        } catch (e: GetCredentialException) {
            // NoCredentialException(기기에 Google 계정 없음) 포함.
            // "[28444] Developer console is not set up correctly"면 Google Cloud의 Android OAuth 클라이언트(패키지 이름 + 이 기기의
            // 서명 SHA-1, 앱 시작 로그 참고)나 웹 클라이언트 ID가 맞지 않는다
            Logger.w(SOCIAL_LOG_TAG, "Google: 실패 ${e::class.simpleName} type=${e.type} message=${e.errorMessage}", e)
            SocialTokenResult.Failed(e)
        } catch (e: GoogleIdTokenParsingException) {
            Logger.w(SOCIAL_LOG_TAG, "Google: ID 토큰 해석 실패", e)
            SocialTokenResult.Failed(e)
        }
    }

    /** `[16] Account reauth failed`: 취소 예외로 오지만 실제로는 설정 오류다 */
    private fun GetCredentialCancellationException.isReauthFailure(): Boolean =
        errorMessage?.toString()?.contains(REAUTH_FAILED, ignoreCase = true) == true

    private companion object {
        const val REAUTH_FAILED = "reauth failed"
    }
}
