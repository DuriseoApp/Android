package com.duriseo.android.core.push

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.duriseo.android.domain.notification.DrawnNotificationCleaner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 이 앱이 그린 알림을 모두 지운다. 로그아웃한 기기에 이전 사용자의 알림이 남지 않게 한다 */
@Singleton
class PushNotificationCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
) : DrawnNotificationCleaner {
    override fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
