package com.duriseo.android.data.local.tutorial

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.duriseo.android.data.local.cleanup.DeviceLevelKeys
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캘린더 튜토리얼을 이미 보여 줬는지 기록 하나 (FR-CAL-06 AC4, D-168). 설정 DataStore에 두되 기기 단위 키라서
 * 로그아웃·탈퇴·세션 끊김에 지우지 않는다 (`LocalDataCleaner`·`DeviceLevelKeys`).
 */
@Singleton
class TutorialPreferencesLocalDataSource @Inject constructor(
    private val settings: DataStore<Preferences>,
) {
    /**
     * 아직 안 보여 줬으면 "보여 줬음"으로 적고 true를 돌려준다. 읽기와 쓰기를 한 번의 `edit`에서 해서
     * 동시에 불러도 true는 한 번이다.
     */
    suspend fun markShownIfFirst(): Boolean {
        var first = false
        settings.edit { prefs ->
            if (prefs[SHOWN] != true) {
                prefs[SHOWN] = true
                first = true
            }
        }
        return first
    }

    private companion object {
        val SHOWN = DeviceLevelKeys.CALENDAR_TUTORIAL_SHOWN
    }
}
