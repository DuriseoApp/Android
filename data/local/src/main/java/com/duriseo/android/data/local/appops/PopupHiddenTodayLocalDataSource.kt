package com.duriseo.android.data.local.appops

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.duriseo.android.data.local.cleanup.DeviceLevelKeys
import com.duriseo.android.domain.appops.HiddenTodayIds
import com.duriseo.android.domain.appops.PopupHiddenTodayStore
import com.duriseo.android.domain.appops.PopupIdSpace
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "오늘 하루 안 보기" 기록 (FR-APP-01 AC2, FR-APP-02 AC3, D-179). 설정 DataStore에 `id`마다 키 하나(값은 숨긴 날짜 `YYYY-MM-DD`,
 * 기기 시간대의 그 날)를 둔다. 공지와 규칙은 키 접두어가 달라 같은 `id`도 서로 다른 것이다 (D-178).
 * 기기 단위 키라서 로그아웃·탈퇴·세션 끊김으로 지우지 않는다 (`LocalDataCleaner`·`DeviceLevelKeys`).
 * 지난 날짜(와 읽을 수 없는 값)는 [hiddenIds]를 부를 때 정리한다.
 */
@Singleton
class PopupHiddenTodayLocalDataSource @Inject constructor(
    private val settings: DataStore<Preferences>,
) : PopupHiddenTodayStore {

    override suspend fun hiddenIds(today: LocalDate): HiddenTodayIds {
        val notices = mutableSetOf<String>()
        val rules = mutableSetOf<String>()
        val stale = mutableListOf<String>()
        settings.data.first().asMap().forEach { (key, value) ->
            val name = key.name
            val (set, prefix) = when {
                name.startsWith(DeviceLevelKeys.POPUP_HIDDEN_NOTICE_PREFIX) ->
                    notices to DeviceLevelKeys.POPUP_HIDDEN_NOTICE_PREFIX
                name.startsWith(DeviceLevelKeys.POPUP_HIDDEN_RULE_PREFIX) ->
                    rules to DeviceLevelKeys.POPUP_HIDDEN_RULE_PREFIX
                else -> return@forEach
            }
            val date = (value as? String)?.let { parseDate(it) }
            when {
                date == null || date.isBefore(today) -> stale += name
                date == today -> set += name.removePrefix(prefix)
            }
        }
        if (stale.isNotEmpty()) {
            settings.edit { prefs -> stale.forEach { prefs.remove(stringPreferencesKey(it)) } }
        }
        return HiddenTodayIds(notices = notices, rules = rules)
    }

    override suspend fun hideToday(space: PopupIdSpace, id: String, today: LocalDate) {
        settings.edit { it[keyOf(space, id)] = today.toString() }
    }

    private fun keyOf(space: PopupIdSpace, id: String): Preferences.Key<String> {
        val prefix = when (space) {
            PopupIdSpace.NOTICE -> DeviceLevelKeys.POPUP_HIDDEN_NOTICE_PREFIX
            PopupIdSpace.UPDATE_RULE -> DeviceLevelKeys.POPUP_HIDDEN_RULE_PREFIX
        }
        return stringPreferencesKey(prefix + id)
    }

    private fun parseDate(text: String): LocalDate? = try {
        LocalDate.parse(text)
    } catch (e: DateTimeParseException) {
        null
    }
}
