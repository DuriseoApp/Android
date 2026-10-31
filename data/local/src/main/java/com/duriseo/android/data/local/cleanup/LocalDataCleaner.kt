package com.duriseo.android.data.local.cleanup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    /** Room(`clearAllTables`는 메인 스레드에서 부르면 안 된다)과 설정을 비운다. */
    open suspend fun clearUserData() {
        withContext(io) {
            database.clearAllTables()
        }
        settings.edit { it.clear() }
    }
}
