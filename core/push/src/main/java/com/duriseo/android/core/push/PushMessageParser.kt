package com.duriseo.android.core.push

import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import com.duriseo.android.domain.sync.SyncAction
import com.duriseo.android.domain.sync.SyncResource
import com.duriseo.android.domain.sync.SyncSignal

/** FCM 데이터 메시지를 앱이 읽은 결과 (API.md 6장·7장) */
sealed interface PushMessage {
    /**
     * `type=PUSH`: 앱이 알림으로 그린다. [title]·[body]는 사용자의 알림 문구(상대 이름·일정 제목 등)라서
     * [toString]에 넣지 않는다 (로그에 남기지 않는다, NFR-SEC-01).
     */
    data class Visible(
        val notificationId: String,
        val kind: NotificationKind,
        val title: String,
        val body: String,
        val target: NotificationTarget?,
    ) : PushMessage {
        override fun toString(): String = "Visible(kind=$kind)"
    }

    /** `type=SYNC`: 화면 갱신용 신호. 사용자에게 보이지 않는다 */
    data class Sync(val signal: SyncSignal) : PushMessage
}

/**
 * FCM `data` 맵을 [PushMessage]로 읽는 순수 함수 (Android 의존 없음).
 *
 * - `type=PUSH`: `notificationId`·`kind`·`title`·`body`가 있어야 한다. `kind`를 모르면(새 종류) 버린다.
 *   `targetType`을 모르거나 없으면 이동 대상 없음이다 (API.md 3.5). `targetId`는 없을 수 있다 (연결·해제·자동 기념일).
 * - `type=SYNC`: `resource`·`action`을 알아야 한다. `id`는 없을 수 있다.
 * - 그 밖의 `type`이나 모양이 맞지 않는 메시지는 null이다 (버린다).
 */
object PushMessageParser {
    const val KEY_TYPE = "type"
    const val KEY_NOTIFICATION_ID = "notificationId"
    const val KEY_KIND = "kind"
    const val KEY_TITLE = "title"
    const val KEY_BODY = "body"
    const val KEY_TARGET_TYPE = "targetType"
    const val KEY_TARGET_ID = "targetId"
    const val KEY_RESOURCE = "resource"
    const val KEY_ACTION = "action"
    const val KEY_ID = "id"

    private const val TYPE_PUSH = "PUSH"
    private const val TYPE_SYNC = "SYNC"

    fun parse(data: Map<String, String>): PushMessage? = when (data[KEY_TYPE]) {
        TYPE_PUSH -> parsePush(data)
        TYPE_SYNC -> parseSync(data)
        else -> null
    }

    private fun parsePush(data: Map<String, String>): PushMessage.Visible? {
        val notificationId = data[KEY_NOTIFICATION_ID]?.takeIf { it.isNotBlank() } ?: return null
        val kind = NotificationKind.fromCode(data[KEY_KIND]) ?: return null
        val title = data[KEY_TITLE]?.takeIf { it.isNotBlank() } ?: return null
        val body = data[KEY_BODY] ?: return null
        val targetType = NotificationTargetType.fromCode(data[KEY_TARGET_TYPE])
        val target = targetType?.let { NotificationTarget(it, data[KEY_TARGET_ID]?.takeIf { id -> id.isNotBlank() }) }
        return PushMessage.Visible(notificationId, kind, title, body, target)
    }

    private fun parseSync(data: Map<String, String>): PushMessage.Sync? {
        val resource = SyncResource.entries.firstOrNull { it.name == data[KEY_RESOURCE] } ?: return null
        val action = SyncAction.entries.firstOrNull { it.name == data[KEY_ACTION] } ?: return null
        val id = data[KEY_ID]?.takeIf { it.isNotBlank() }
        return PushMessage.Sync(SyncSignal(resource, action, id))
    }
}
