package com.duriseo.android.data.local.cleanup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.duriseo.android.core.common.coroutines.IoDispatcher
import com.duriseo.android.data.local.db.DuriseoDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그아웃·탈퇴·세션 끊김 때 사용자 데이터를 모두 지운다: Room의 모든 테이블과 설정 DataStore.
 * 테스트가 실패하는 정리를 흉내 내려고 `open`이다.
 * 토큰은 [com.duriseo.android.domain.auth.TokenStorage]가, 소셜 SDK 상태는 `SocialSessionCleaner`가 따로 지운다.
 */
@Singleton
open class LocalDataCleaner @Inject constructor(
    private val database: DuriseoDatabase,
    private val settings: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /**
     * Room(`clearAllTables`는 메인 스레드에서 부르면 안 된다)과 설정을 비운다.
     * 기기 단위 기록([DeviceLevelKeys])은 사용자 데이터가 아니라 남긴다. 한 번의 `edit`라서 지우는 중에 사라지지 않는다.
     */
    open suspend fun clearUserData() {
        withContext(io) {
            database.clearAllTables()
        }
        settings.edit { prefs ->
            val kept = DeviceLevelKeys.ALL.mapNotNull { key -> prefs[key]?.let { value -> Pair(key, value) } }
            prefs.clear()
            kept.forEach { (key, value) -> prefs[key] = value }
        }
    }
}

/**
 * 로그아웃·탈퇴·세션 끊김에도 지우지 않는 기기 단위 설정 키. 사용자가 아니라 이 기기에 대한 기록이다.
 * 알림 권한을 이미 물었는지 (FR-NOTI-01 AC3, D-144): 지우면 같은 기기에서 다시 로그인할 때 시스템 권한창을 또 띄운다.
 */
internal object DeviceLevelKeys {
    val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")

    val ALL: List<Preferences.Key<Boolean>> = listOf(NOTIFICATION_PERMISSION_ASKED)
}
