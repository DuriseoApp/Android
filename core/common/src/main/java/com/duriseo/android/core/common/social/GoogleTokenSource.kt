package com.duriseo.android.core.common.social

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
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
            return SocialTokenResult.Failed(IllegalStateException("Google 웹 클라이언트 ID가 설정되지 않았어요"))
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()

        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                SocialTokenResult.Success(SocialProvider.GOOGLE, idToken)
            } else {
                SocialTokenResult.Failed(IllegalStateException("예상하지 못한 자격 증명 종류예요"))
            }
        } catch (e: GetCredentialCancellationException) {
            SocialTokenResult.Cancelled
        } catch (e: GetCredentialException) {
            // NoCredentialException(기기에 Google 계정 없음) 포함
            SocialTokenResult.Failed(e)
        } catch (e: GoogleIdTokenParsingException) {
            SocialTokenResult.Failed(e)
        }
    }
}
