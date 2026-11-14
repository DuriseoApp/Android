package com.duriseo.android.domain.notification

import java.time.Instant

/** 알림 종류 (API.md 3.5 `type`, 푸시의 `kind`). 채널 5개와는 다르다: 연결·해제 둘은 한 채널("연결·해제")이다 (D-136) */
enum class NotificationKind {
    EVENT_REMINDER,
    PARTNER_CREATED,
    MEMO_PINNED,
    ANNIVERSARY,
    COUPLE_LINKED,
    COUPLE_UNLINKED,
    ;

    companion object {
        /** 서버 문자열을 종류로 읽는다. 모르는 값은 null이다 (알림 목록은 건너뛰고 푸시는 그리지 않는다) */
        fun fromCode(code: String?): NotificationKind? = entries.firstOrNull { it.name == code }
    }
}

/** 알림을 누르면 가는 곳의 종류 (API.md 3.5 `targetType`). 연결·해제는 `SETTINGS`다 (D-85) */
enum class NotificationTargetType {
    EVENT,
    MEMO,
    ANNIVERSARY,
    SETTINGS,
    ;

    companion object {
        /** 모르는 값은 null이다 (이동 대상 없음으로 본다) */
        fun fromCode(code: String?): NotificationTargetType? = entries.firstOrNull { it.name == code }
    }
}

/** 이동 대상. [id]는 `SETTINGS`와 자동 기념일에서 null이다 (API.md 3.5) */
data class NotificationTarget(val type: NotificationTargetType, val id: String?)

/**
 * 알림 목록의 한 건 (API.md 3.5 `{id, type, title, body, targetType, targetId, read, createdAt}`).
 * [title]·[body]는 서버가 만든 시점의 문구 그대로다 (D-46).
 */
data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val target: NotificationTarget?,
    val read: Boolean,
    val createdAt: Instant,
)

/**
 * 알림 목록 화면이 구독하는 값. 서버 순서(최신순) 그대로다.
 *
 * @property hasMore 다음 페이지가 있다 (`nextCursor`가 있음)
 * @property isLoaded 첫 페이지를 한 번이라도 받아 저장했다. false인 빈 목록은 "알림 없음"이 아니라 "아직 안 받음"이다
 */
data class NotificationList(
    val items: List<AppNotification> = emptyList(),
    val hasMore: Boolean = false,
    val isLoaded: Boolean = false,
)

/** 푸시 알림을 누를 때 앱에 넘기는 값 (API.md 7장 data 키). D 묶음이 읽는다 */
data class PushTapTarget(
    val notificationId: String,
    val kind: NotificationKind,
    val target: NotificationTarget?,
)
