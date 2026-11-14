package com.duriseo.android.core.push

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.duriseo.android.core.common.push.PushIntentExtras
import com.duriseo.android.core.common.push.PushIntentTrust
import com.duriseo.android.core.designsystem.theme.NotificationAccentArgb
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `type=PUSH` 메시지를 알림으로 그린다 (API.md 7장, D-136). 제목·본문은 서버가 만든 문구 그대로이고 앱이 가리지 않는다 (D-46).
 * 누르면 `MainActivity`로 가는 Intent에 `notificationId`·`kind`·`targetType`·`targetId`를 담는다 ([PushIntentExtras]).
 * 푸시를 누른 뒤의 읽음 처리·이동은 `MainActivity`가 한다 (D 묶음). 제목·본문·ID를 로그에 남기지 않는다.
 */
@Singleton
open class PushNotifier @Inject constructor() {

    /** 알림 권한이 없으면(Android 13 이상 거부) 그리지 않고 false다. 권한은 알림 목록에는 영향이 없다 */
    @SuppressLint("MissingPermission") // areNotificationsEnabled()로 확인한 뒤에만 notify한다
    open fun show(context: Context, message: PushMessage.Visible): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        // 채널이 없으면(앱이 시작 전에 FCM이 먼저 깨운 경우) 먼저 만든다. 이미 있으면 그대로다
        NotificationChannels.createAll(context)

        val notification = NotificationCompat.Builder(context, message.kind.toChannel().channelId)
            .setSmallIcon(R.drawable.ic_stat_duriseo)
            .setColor(NotificationAccentArgb)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(createContentIntent(context, message))
            .build()
        // 같은 알림 ID는 같은 알림을 갱신한다 (태그 = 알림 ID)
        manager.notify(message.notificationId, NOTIFY_ID, notification)
        return true
    }

    /** [show]로 그린 알림 하나를 지운다 (태그 = 알림 ID) */
    fun cancel(context: Context, notificationId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId, NOTIFY_ID)
    }

    private fun createContentIntent(context: Context, message: PushMessage.Visible): PendingIntent? {
        val intent = pushLaunchIntent(context, message) ?: return null
        return PendingIntent.getActivity(
            context,
            message.notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        /** 알림마다 태그(알림 ID)가 다르므로 번호는 하나로 둔다 */
        const val NOTIFY_ID = 1
    }
}

/**
 * 앱의 시작 화면(`MainActivity`)으로 가는 Intent. 앱이 이미 떠 있으면 그 화면을 다시 쓰고(`onNewIntent`) 새로 만들지 않는다.
 * 시작 화면 컴포넌트는 런처 Intent에서 찾는다 (`core:push`는 `feature:main`을 모른다). 엑스트라는 [PushIntentExtras] 이름이다.
 * `MAIN`·`LAUNCHER` 액션을 빼고 컴포넌트만 지정해, 이미 떠 있는 앱에 엑스트라가 전달되지 않는 "런처 재진입" 동작을 피한다.
 */
internal fun pushLaunchIntent(context: Context, message: PushMessage.Visible): Intent? {
    val component = context.packageManager.getLaunchIntentForPackage(context.packageName)?.component ?: return null
    return Intent().apply {
        setComponent(component)
        // 우리 알림이 만든 Intent만 받게 표시한다 (`MainActivity`가 확인한다, 위조 방지)
        PushIntentTrust.stamp(context, this)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(PushIntentExtras.NOTIFICATION_ID, message.notificationId)
        putExtra(PushIntentExtras.KIND, message.kind.name)
        message.target?.let { target ->
            putExtra(PushIntentExtras.TARGET_TYPE, target.type.name)
            target.id?.let { putExtra(PushIntentExtras.TARGET_ID, it) }
        }
    }
}
