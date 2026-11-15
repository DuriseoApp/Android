package com.duriseo.android.core.push

import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import com.duriseo.android.domain.sync.SyncAction
import com.duriseo.android.domain.sync.SyncResource
import com.duriseo.android.domain.sync.SyncSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** FCM data 메시지 읽기 (API.md 6장 SYNC, 7장 PUSH) */
class PushMessageParserTest {

    private fun push(
        kind: String = "EVENT_REMINDER",
        notificationId: String? = "n1",
        title: String? = "회식",
        body: String? = "1시간 뒤 · 오후 7:30",
        targetType: String? = "EVENT",
        targetId: String? = "e1",
    ): Map<String, String> = buildMap {
        put("type", "PUSH")
        put("kind", kind)
        notificationId?.let { put("notificationId", it) }
        title?.let { put("title", it) }
        body?.let { put("body", it) }
        targetType?.let { put("targetType", it) }
        targetId?.let { put("targetId", it) }
    }

    @Test
    fun `PUSH data를 종류 제목 본문 대상으로 읽고 모르는 kind는 버린다`() {
        val message = PushMessageParser.parse(push()) as PushMessage.Visible

        assertEquals("n1", message.notificationId)
        assertEquals(NotificationKind.EVENT_REMINDER, message.kind)
        assertEquals("회식", message.title)
        assertEquals("1시간 뒤 · 오후 7:30", message.body)
        assertEquals(NotificationTarget(NotificationTargetType.EVENT, "e1"), message.target)

        // 서버가 새 종류를 더해도 옛 앱은 그리지 않고 버린다
        assertNull(PushMessageParser.parse(push(kind = "NEW_KIND")))
        assertNull(PushMessageParser.parse(push(kind = "")))
    }

    @Test
    fun `PUSH 여섯 종류를 모두 읽는다`() {
        val kinds = listOf(
            "EVENT_REMINDER", "PARTNER_CREATED", "MEMO_PINNED", "ANNIVERSARY", "COUPLE_LINKED", "COUPLE_UNLINKED",
        )

        kinds.forEach { kind ->
            val message = PushMessageParser.parse(push(kind = kind)) as PushMessage.Visible
            assertEquals(kind, message.kind.name)
        }
    }

    @Test
    fun `PUSH 연결 해제 알림은 SETTINGS 대상이고 targetId가 없다 D-85`() {
        val message = PushMessageParser.parse(
            push(kind = "COUPLE_UNLINKED", targetType = "SETTINGS", targetId = null),
        ) as PushMessage.Visible

        assertEquals(NotificationTarget(NotificationTargetType.SETTINGS, null), message.target)
    }

    @Test
    fun `PUSH 자동 기념일은 targetId 없이 ANNIVERSARY 대상이다 D-138`() {
        val message = PushMessageParser.parse(
            push(kind = "ANNIVERSARY", targetType = "ANNIVERSARY", targetId = null),
        ) as PushMessage.Visible

        assertEquals(NotificationTarget(NotificationTargetType.ANNIVERSARY, null), message.target)
    }

    @Test
    fun `PUSH 이동 대상이 없거나 모르는 targetType이면 대상 없음으로 읽는다`() {
        assertNull((PushMessageParser.parse(push(targetType = null, targetId = null)) as PushMessage.Visible).target)
        assertNull((PushMessageParser.parse(push(targetType = "FUTURE", targetId = "x")) as PushMessage.Visible).target)
    }

    @Test
    fun `PUSH 알림 ID나 제목이나 본문이 없으면 버린다`() {
        assertNull(PushMessageParser.parse(push(notificationId = null)))
        assertNull(PushMessageParser.parse(push(notificationId = " ")))
        assertNull(PushMessageParser.parse(push(title = null)))
        assertNull(PushMessageParser.parse(push(title = "")))
        assertNull(PushMessageParser.parse(push(body = null)))
    }

    @Test
    fun `PUSH 로그에 찍혀도 제목과 본문은 나오지 않는다 NFR-SEC-01`() {
        val message = PushMessageParser.parse(push(title = "비밀 제목", body = "비밀 본문")) as PushMessage.Visible

        assertFalse(message.toString().contains("비밀"))
    }

    @Test
    fun `SYNC data를 리소스 action id로 읽는다`() {
        val message = PushMessageParser.parse(
            mapOf("type" to "SYNC", "resource" to "EVENT", "id" to "0192", "action" to "UPDATED"),
        ) as PushMessage.Sync

        assertEquals(SyncSignal(SyncResource.EVENT, SyncAction.UPDATED, "0192"), message.signal)
    }

    @Test
    fun `SYNC id가 없는 LINKED와 UNLINKED도 읽는다`() {
        val linked = PushMessageParser.parse(mapOf("type" to "SYNC", "resource" to "SPACE", "action" to "LINKED")) as PushMessage.Sync
        val unlinked = PushMessageParser.parse(mapOf("type" to "SYNC", "resource" to "SPACE", "action" to "UNLINKED")) as PushMessage.Sync

        assertEquals(SyncSignal(SyncResource.SPACE, SyncAction.LINKED, null), linked.signal)
        assertEquals(SyncSignal(SyncResource.SPACE, SyncAction.UNLINKED, null), unlinked.signal)
    }

    @Test
    fun `SYNC 모르는 리소스나 action이면 버린다`() {
        assertNull(PushMessageParser.parse(mapOf("type" to "SYNC", "resource" to "PHOTO", "action" to "UPDATED")))
        assertNull(PushMessageParser.parse(mapOf("type" to "SYNC", "resource" to "EVENT", "action" to "MOVED")))
        assertNull(PushMessageParser.parse(mapOf("type" to "SYNC", "action" to "UPDATED")))
    }

    @Test
    fun `type이 없거나 모르면 버린다`() {
        assertNull(PushMessageParser.parse(emptyMap()))
        assertNull(PushMessageParser.parse(mapOf("type" to "OTHER", "resource" to "EVENT", "action" to "UPDATED")))
    }
}
