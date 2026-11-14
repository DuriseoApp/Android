package com.duriseo.android.domain.notification

import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

/**
 * 알림 종류별 푸시 켜기·끄기 (API.md 3.1 `NotificationSettings`, FR-NOTI-02). 켜짐이 true다.
 * 연결·해제 알림은 끌 수 없어 필드가 없다 (PRD 8장).
 */
data class NotificationSettings(
    val eventReminder: Boolean,
    val partnerCreated: Boolean,
    val memoPinned: Boolean,
    val anniversary: Boolean,
) {
    operator fun get(kind: NotificationSettingKind): Boolean = when (kind) {
        NotificationSettingKind.EVENT_REMINDER -> eventReminder
        NotificationSettingKind.PARTNER_CREATED -> partnerCreated
        NotificationSettingKind.MEMO_PINNED -> memoPinned
        NotificationSettingKind.ANNIVERSARY -> anniversary
    }

    fun with(kind: NotificationSettingKind, enabled: Boolean): NotificationSettings = when (kind) {
        NotificationSettingKind.EVENT_REMINDER -> copy(eventReminder = enabled)
        NotificationSettingKind.PARTNER_CREATED -> copy(partnerCreated = enabled)
        NotificationSettingKind.MEMO_PINNED -> copy(memoPinned = enabled)
        NotificationSettingKind.ANNIVERSARY -> copy(anniversary = enabled)
    }
}

/** 스위치 4개 (SCR-SET-02). 이름은 API.md `NotificationSettings`의 필드와 같다 */
enum class NotificationSettingKind { EVENT_REMINDER, PARTNER_CREATED, MEMO_PINNED, ANNIVERSARY }

/**
 * 알림 설정 (API.md 3.1 `GET`·`PATCH /me/notification-settings`, D-145). 화면은 기기에 저장된 마지막 서버 값만 구독한다.
 * 한 번도 받지 못했으면 [observe]는 null이다 (스위치 비활성, Q-14와 같은 상황).
 */
interface NotificationSettingsRepository {
    /** 마지막으로 서버에서 받은 값. 한 번도 못 받았으면 null */
    fun observe(): Flow<NotificationSettings?>

    /** `GET`: 화면에 들어올 때마다 부른다. 성공하면 기기에 저장한다. 오프라인이면 요청 없이 `Offline` */
    suspend fun refresh(): DataResult<Unit>

    /** `PATCH`: 바뀐 종류 하나만 보낸다. 쓰기라서 오프라인이면 요청 없이 `Offline`. 성공하면 응답(전체 설정)을 저장한다 */
    suspend fun update(kind: NotificationSettingKind, enabled: Boolean): DataResult<Unit>
}
