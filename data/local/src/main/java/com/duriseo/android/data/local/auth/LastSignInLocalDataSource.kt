package com.duriseo.android.data.local.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.duriseo.android.data.local.cleanup.DeviceLevelKeys
import com.duriseo.android.domain.me.SocialProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이 기기에서 마지막으로 로그인에 성공한 소셜 수단 하나 (FR-AUTH-10, D-188). 설정 DataStore의 문자열 키
 * `last_sign_in_provider`(값 `GOOGLE`·`KAKAO`)에 둔다. 기기 단위 키라서 로그아웃·세션 끊김에 지우지 않고
 * ([DeviceLevelKeys]), 탈퇴 성공 때만 [clear]로 지운다 (Q-25). 모르는 값은 없음으로 읽는다.
 */
@Singleton
class LastSignInLocalDataSource @Inject constructor(
    private val settings: DataStore<Preferences>,
) {
    val provider: Flow<SocialProvider?> = settings.data
        .map { prefs -> parse(prefs[KEY]) }
        .distinctUntilChanged()

    suspend fun save(provider: SocialProvider) {
        settings.edit { it[KEY] = provider.name }
    }

    suspend fun clear() {
        settings.edit { it.remove(KEY) }
    }

    private fun parse(value: String?): SocialProvider? = when (value) {
        SocialProvider.GOOGLE.name -> SocialProvider.GOOGLE
        SocialProvider.KAKAO.name -> SocialProvider.KAKAO
        else -> null
    }

    private companion object {
        val KEY = DeviceLevelKeys.LAST_SIGN_IN_PROVIDER
    }
}
