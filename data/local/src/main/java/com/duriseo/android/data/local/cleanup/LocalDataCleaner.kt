package com.duriseo.android.data.local.cleanup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
            val kept = prefs.asMap().filterKeys { key -> DeviceLevelKeys.isDeviceLevel(key) }
            prefs.clear()
            @Suppress("UNCHECKED_CAST")
            kept.forEach { (key, value) -> prefs[key as Preferences.Key<Any>] = value }
        }
    }
}

/**
 * 로그아웃·탈퇴·세션 끊김에도 지우지 않는 기기 단위 설정 키. 사용자가 아니라 이 기기에 대한 기록이다.
 * 알림 권한을 이미 물었는지 (FR-NOTI-01 AC3, D-144): 지우면 같은 기기에서 다시 로그인할 때 시스템 권한창을 또 띄운다.
 * 캘린더 튜토리얼을 이미 보여 줬는지 (FR-CAL-06 AC4, D-168): 기기마다 한 번이라 로그아웃·탈퇴·연결 해제로 지우지 않는다.
 * 최근 로그인 수단 (FR-AUTH-10, D-188): 문자열 키라 [ALL]이 아니라 이름([isDeviceLevel])으로 가린다. 탈퇴 성공 때만 따로 지운다 (Q-25).
 * 공지·업데이트 팝업 "오늘 하루 안 보기" (FR-APP-01 AC2, FR-APP-02 AC3, D-179): `id`마다 키가 하나라서 이름 앞부분([isDeviceLevel])으로 가린다.
 */
internal object DeviceLevelKeys {
    val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")

    val CALENDAR_TUTORIAL_SHOWN = booleanPreferencesKey("calendar_tutorial_shown")

    /** 마지막으로 로그인에 성공한 소셜 수단 `GOOGLE`·`KAKAO` (FR-AUTH-10) */
    val LAST_SIGN_IN_PROVIDER = stringPreferencesKey("last_sign_in_provider")

    /** 이름이 정해진 키(Boolean) */
    val ALL: List<Preferences.Key<Boolean>> = listOf(NOTIFICATION_PERMISSION_ASKED, CALENDAR_TUTORIAL_SHOWN)

    /** 공지 `id`별 "오늘 하루 안 보기" 키 이름 앞부분. 값은 숨긴 날짜 `YYYY-MM-DD`(기기 시간대) */
    const val POPUP_HIDDEN_NOTICE_PREFIX = "popup_hidden_notice_"

    /** 업데이트 규칙 `id`별 키 이름 앞부분. 공지와 `id` 공간이 따로라서 접두어도 따로다 (D-178) */
    const val POPUP_HIDDEN_RULE_PREFIX = "popup_hidden_rule_"

    /** 로그아웃·탈퇴 정리에서 지우지 않는 키인가 */
    fun isDeviceLevel(key: Preferences.Key<*>): Boolean =
        ALL.any { it.name == key.name } ||
            key.name == LAST_SIGN_IN_PROVIDER.name ||
            key.name.startsWith(POPUP_HIDDEN_NOTICE_PREFIX) ||
            key.name.startsWith(POPUP_HIDDEN_RULE_PREFIX)
}
