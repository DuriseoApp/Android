package com.duriseo.android.core.common.social

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.domain.auth.SocialSessionCleaner
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**
 * 로그아웃·탈퇴·세션 끊김 때 기기의 소셜 SDK 상태를 지운다 (API.md 1장). 실패는 무시한다.
 * Kakao 앱 연결 끊기는 서버가 한다 (API.md 4.6).
 */
@Singleton
class DefaultSocialSessionCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AppConfig,
) : SocialSessionCleaner {

    override suspend fun clear() {
        clearKakao()
        clearGoogle()
    }

    private suspend fun clearKakao() {
        // SDK를 초기화하지 않았으면(키 없음) 부르지 않는다
        if (config.kakaoNativeAppKey.isBlank()) return
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                UserApiClient.instance.logout { error ->
                    if (error != null) Logger.d(TAG, "카카오 로그아웃 실패(무시): ${error.javaClass.simpleName}")
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.d(TAG, "카카오 로그아웃 실패(무시): ${e.javaClass.simpleName}")
        }
    }

    private suspend fun clearGoogle() {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.d(TAG, "Google 자격 증명 상태 지우기 실패(무시): ${e.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "social"
    }
}
