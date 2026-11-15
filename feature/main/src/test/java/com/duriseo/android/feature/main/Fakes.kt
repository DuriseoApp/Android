package com.duriseo.android.feature.main

import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTapHandler
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.destinationOf

/**
 * 누른 알림 처리 가짜. 기본은 실제 규칙([destinationOf])대로 갈 곳을 정하고, [override]를 주면 그 값을 돌려준다
 * (삭제된 대상 같은 데이터 쪽 결과를 흉내 낼 때). 읽음 처리 요청 기록은 [opened]다.
 */
class FakeNotificationTapHandler : NotificationTapHandler {
    val opened = mutableListOf<String>()
    var override: NotificationDestination? = null

    override suspend fun open(notificationId: String, kind: NotificationKind, target: NotificationTarget?): NotificationDestination {
        opened += notificationId
        return override ?: destinationOf(kind, target)
    }
}
