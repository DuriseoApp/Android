package com.duriseo.android.data.local.me

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기기에 저장하는 **확인한 공간 ID** (API.md 4.4의 8단계, D-98, FR-COUPLE-03 AC6). 서버에는 없다.
 * 설정 DataStore에 두므로 로그아웃·탈퇴·세션 끊김·앱 초기화 때 다른 설정과 함께 지워진다.
 *
 * 저장 시점은 `data:repository`의 `MeRepositoryImpl`이 정한다:
 * ① `GET /me`가 개인 공간을 줄 때마다, ② 연결 완료 시트를 닫거나 [시작하기]를 마쳤을 때, ③ 초기화할 때.
 */
@Singleton
class AcknowledgedSpaceLocalDataSource @Inject constructor(
    private val settings: DataStore<Preferences>,
) {
    fun observe(): Flow<String?> = settings.data.map { it[KEY] }.distinctUntilChanged()

    suspend fun get(): String? = observe().first()

    suspend fun set(spaceId: String) {
        settings.edit { it[KEY] = spaceId }
    }

    private companion object {
        val KEY = stringPreferencesKey("acknowledged_space_id")
    }
}
