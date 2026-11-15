package com.duriseo.android.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import com.duriseo.android.domain.notification.NotificationKind

/**
 * 알림 채널 5개 (PRD D-136). 이름은 알림 종류 이름과 같다. ID는 영어 고정값이고 중요도는 기본이다
 * (소리·배지 있음, 헤드업은 시스템 기본, 진행 기록 확인 필요 4). 연결·해제 알림 둘은 한 채널이다.
 */
enum class PushChannel(val channelId: String, @StringRes val nameRes: Int) {
    EVENT_REMINDER("event_reminder", R.string.push_channel_event_reminder),
    PARTNER_CREATED("partner_created", R.string.push_channel_partner_created),
    MEMO_PINNED("memo_pinned", R.string.push_channel_memo_pinned),
    ANNIVERSARY("anniversary", R.string.push_channel_anniversary),
    COUPLE("couple", R.string.push_channel_couple),
}

/** 알림 종류가 가는 채널 */
fun NotificationKind.toChannel(): PushChannel = when (this) {
    NotificationKind.EVENT_REMINDER -> PushChannel.EVENT_REMINDER
    NotificationKind.PARTNER_CREATED -> PushChannel.PARTNER_CREATED
    NotificationKind.MEMO_PINNED -> PushChannel.MEMO_PINNED
    NotificationKind.ANNIVERSARY -> PushChannel.ANNIVERSARY
    NotificationKind.COUPLE_LINKED, NotificationKind.COUPLE_UNLINKED -> PushChannel.COUPLE
}

object NotificationChannels {
    /**
     * 채널 5개를 만든다. 이미 있는 채널은 이름만 갱신되고 사용자가 바꾼 설정은 그대로다 (시스템 동작).
     * minSdk가 26이라 채널은 항상 있다.
     */
    fun createAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            PushChannel.entries.map { channel ->
                NotificationChannel(channel.channelId, context.getString(channel.nameRes), NotificationManager.IMPORTANCE_DEFAULT)
            },
        )
    }
}
