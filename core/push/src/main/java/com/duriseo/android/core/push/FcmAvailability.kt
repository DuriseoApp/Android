package com.duriseo.android.core.push

import android.content.Context
import com.duriseo.android.core.common.Logger
import com.google.firebase.FirebaseApp
import java.util.concurrent.atomic.AtomicBoolean

/**
 * FCM을 쓸 수 있는지. `app/google-services.json`이 없는 빌드(CI, 설정 파일이 없는 환경)는 google-services 플러그인이
 * 적용되지 않아 `FirebaseApp`이 초기화되지 않는다. 이때 FCM은 꺼진 채로 동작한다: 토큰 등록·푸시 수신 없이 나머지 기능은 그대로다
 * (docs/SETUP.md). 꺼져 있으면 경고 로그를 한 번만 남긴다.
 */
object FcmAvailability {
    private val warned = AtomicBoolean(false)

    fun isAvailable(context: Context): Boolean {
        val available = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
        if (!available && warned.compareAndSet(false, true)) {
            Logger.w(TAG, "Firebase 설정(google-services.json)이 없어 푸시(FCM)를 끈 채 실행해요")
        }
        return available
    }

    private const val TAG = "push"
}
