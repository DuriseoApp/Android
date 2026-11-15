package com.duriseo.android.core.push

import android.content.Context
import com.duriseo.android.core.common.Logger
import com.duriseo.android.domain.device.PushTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**
 * Firebase에서 기기의 FCM 토큰을 얻는다. Firebase가 꺼져 있거나([FcmAvailability]) 토큰을 얻지 못하면 null이다.
 * 토큰은 로그에 남기지 않는다 (NFR-SEC-01). domain은 Firebase를 모르고 이 인터페이스만 본다.
 */
@Singleton
class FirebasePushTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : PushTokenProvider {

    override suspend fun currentToken(): String? {
        if (!FcmAvailability.isAvailable(context)) return null
        return try {
            suspendCancellableCoroutine { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    val token = if (task.isSuccessful) task.result?.takeIf { it.isNotBlank() } else null
                    if (continuation.isActive) continuation.resume(token)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "FCM 토큰을 얻지 못했어요: ${e.javaClass.simpleName}")
            null
        }
    }

    private companion object {
        const val TAG = "push"
    }
}
