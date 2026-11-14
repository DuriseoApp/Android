package com.duriseo.android.data.local.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.duriseo.android.domain.notification.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 설정 DataStore에 두는 알림 관련 값 두 가지 (로그아웃·탈퇴·세션 끊김 때 설정과 함께 지워진다, `LocalDataCleaner`).
 * ① 알림 권한을 "이미 물었는지" 기록 하나 (FR-NOTI-01 AC3, D-144). ② 마지막으로 서버에서 받은 알림 설정 4개 (D-145).
 */
@Singleton
class NotificationPreferencesLocalDataSource @Inject constructor(
    private val settings: DataStore<Preferences>,
) {
    /**
     * 아직 안 물었으면 "물었음"으로 적고 true를 돌려준다. 읽기와 쓰기를 한 번의 `edit`에서 해서
     * 세 시점이 동시에 불러도 true는 한 번이다.
     */
    suspend fun markPermissionAskedIfFirst(): Boolean {
        var first = false
        settings.edit { prefs ->
            if (prefs[PERMISSION_ASKED] != true) {
                prefs[PERMISSION_ASKED] = true
                first = true
            }
        }
        return first
    }

    /** 저장된 설정. 4개가 다 있어야 값이다. 한 번도 못 받았으면 null */
    fun observeSettings(): Flow<NotificationSettings?> = settings.data.map { p ->
        val e = p[EVENT_REMINDER]
        val c = p[PARTNER_CREATED]
        val m = p[MEMO_PINNED]
        val a = p[ANNIVERSARY]
        if (e == null || c == null || m == null || a == null) null else NotificationSettings(e, c, m, a)
    }.distinctUntilChanged()

    suspend fun saveSettings(value: NotificationSettings) {
        settings.edit {
            it[EVENT_REMINDER] = value.eventReminder
            it[PARTNER_CREATED] = value.partnerCreated
            it[MEMO_PINNED] = value.memoPinned
            it[ANNIVERSARY] = value.anniversary
        }
    }

    private companion object {
        val PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val EVENT_REMINDER = booleanPreferencesKey("notification_setting_event_reminder")
        val PARTNER_CREATED = booleanPreferencesKey("notification_setting_partner_created")
        val MEMO_PINNED = booleanPreferencesKey("notification_setting_memo_pinned")
        val ANNIVERSARY = booleanPreferencesKey("notification_setting_anniversary")
    }
}
