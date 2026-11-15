package com.duriseo.android.core.push

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.duriseo.android.core.common.push.PushIntentExtras
import com.duriseo.android.core.common.push.toPushTapTarget
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import com.duriseo.android.domain.notification.PushTapTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 알림 채널 5개 (D-136)와 푸시 Intent 엑스트라 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationChannelsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `D-136 채널 5개를 종류 이름으로 만든다`() {
        NotificationChannels.createAll(context)

        val channels = context.getSystemService(NotificationManager::class.java).notificationChannels
        assertEquals(
            mapOf(
                "event_reminder" to "일정 알림",
                "partner_created" to "상대 일정·메모 추가",
                "memo_pinned" to "메모 고정",
                "anniversary" to "기념일",
                "couple" to "연결·해제",
            ),
            channels.associate { it.id to it.name.toString() },
        )
        // 중요도는 기본 (소리·배지 있음, 확인 필요 4)
        assertEquals(setOf(NotificationManager.IMPORTANCE_DEFAULT), channels.map { it.importance }.toSet())
    }

    @Test
    fun `D-136 채널을 두 번 만들어도 5개다`() {
        NotificationChannels.createAll(context)
        NotificationChannels.createAll(context)

        assertEquals(5, context.getSystemService(NotificationManager::class.java).notificationChannels.size)
    }

    @Test
    fun `D-136 알림 종류는 채널에 나뉜다 연결과 해제는 한 채널이다`() {
        assertEquals("event_reminder", NotificationKind.EVENT_REMINDER.toChannel().channelId)
        assertEquals("partner_created", NotificationKind.PARTNER_CREATED.toChannel().channelId)
        assertEquals("memo_pinned", NotificationKind.MEMO_PINNED.toChannel().channelId)
        assertEquals("anniversary", NotificationKind.ANNIVERSARY.toChannel().channelId)
        assertEquals("couple", NotificationKind.COUPLE_LINKED.toChannel().channelId)
        assertEquals("couple", NotificationKind.COUPLE_UNLINKED.toChannel().channelId)
    }

    @Test
    fun `FR-NOTI-03 푸시 Intent의 엑스트라를 PushTapTarget으로 읽는다`() {
        val intent = Intent()
            .putExtra(PushIntentExtras.NOTIFICATION_ID, "n1")
            .putExtra(PushIntentExtras.KIND, "MEMO_PINNED")
            .putExtra(PushIntentExtras.TARGET_TYPE, "MEMO")
            .putExtra(PushIntentExtras.TARGET_ID, "m1")

        assertEquals(
            PushTapTarget("n1", NotificationKind.MEMO_PINNED, NotificationTarget(NotificationTargetType.MEMO, "m1")),
            intent.toPushTapTarget(),
        )
    }

    @Test
    fun `FR-NOTI-03 푸시 Intent가 아니면 읽지 않는다`() {
        assertNull(Intent().toPushTapTarget())
        assertNull(Intent().putExtra(PushIntentExtras.NOTIFICATION_ID, "n1").toPushTapTarget()) // kind 없음
        assertEquals(false, PushIntentExtras.isPushIntent(Intent()))
        assertEquals(true, PushIntentExtras.isPushIntent(Intent().putExtra(PushIntentExtras.NOTIFICATION_ID, "n1")))
    }
}
