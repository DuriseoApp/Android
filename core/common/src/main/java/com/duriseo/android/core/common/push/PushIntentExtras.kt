package com.duriseo.android.core.common.push

import android.content.Intent
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import com.duriseo.android.domain.notification.PushTapTarget

/**
 * 푸시 알림을 누를 때 `MainActivity`로 가는 Intent의 엑스트라 이름 (API.md 7장 data 키와 같다).
 * 알림을 그리는 `core:push`가 넣고, 받는 `feature:main`의 `MainActivity`(단위 7 D 묶음)가 읽는다.
 * 두 모듈이 서로 의존할 수 없어 이름을 여기에 둔다.
 */
object PushIntentExtras {
    const val NOTIFICATION_ID = "notificationId"
    const val KIND = "kind"
    const val TARGET_TYPE = "targetType"
    const val TARGET_ID = "targetId"

    /** 푸시를 눌러 들어온 Intent인지 가려내는 표시. [NOTIFICATION_ID]가 있으면 푸시 Intent다 */
    fun isPushIntent(intent: Intent?): Boolean = !intent?.getStringExtra(NOTIFICATION_ID).isNullOrBlank()
}

/**
 * 푸시 Intent의 엑스트라를 읽는다. 푸시 Intent가 아니거나 모르는 `kind`면 null이다.
 * 모르는 `targetType`은 이동 대상 없음으로 본다 (API.md 3.5).
 */
fun Intent.toPushTapTarget(): PushTapTarget? {
    val notificationId = getStringExtra(PushIntentExtras.NOTIFICATION_ID)?.takeIf { it.isNotBlank() } ?: return null
    val kind = NotificationKind.fromCode(getStringExtra(PushIntentExtras.KIND)) ?: return null
    val targetType = NotificationTargetType.fromCode(getStringExtra(PushIntentExtras.TARGET_TYPE))
    val target = targetType?.let { NotificationTarget(it, getStringExtra(PushIntentExtras.TARGET_ID)) }
    return PushTapTarget(notificationId, kind, target)
}
