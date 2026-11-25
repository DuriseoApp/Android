package com.duriseo.android.data.local.appops

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.duriseo.android.data.local.cleanup.LocalDataCleaner
import com.duriseo.android.data.local.db.DuriseoDatabase
import com.duriseo.android.domain.appops.HiddenTodayIds
import com.duriseo.android.domain.appops.PopupIdSpace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** "오늘 하루 안 보기" 기록 (D-179). 로그아웃 정리(`LocalDataCleaner`)에도 남아야 한다 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PopupHiddenTodayLocalDataSourceTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: DuriseoDatabase
    private lateinit var settings: DataStore<Preferences>
    private lateinit var scope: CoroutineScope
    private lateinit var source: PopupHiddenTodayLocalDataSource

    private val today = LocalDate.of(2026, 11, 25)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DuriseoDatabase::class.java,
        ).allowMainThreadQueries().build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        settings = PreferenceDataStoreFactory.create(scope = scope) { File(tempFolder.root, "settings.preferences_pb") }
        source = PopupHiddenTodayLocalDataSource(settings)
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
    }

    @Test
    fun `D-179 id별로 오늘 숨긴 것만 돌려주고 공지와 규칙은 따로 기억한다`() = runBlocking {
        source.hideToday(PopupIdSpace.NOTICE, "same", today)
        source.hideToday(PopupIdSpace.UPDATE_RULE, "rule-1", today)

        assertEquals(HiddenTodayIds(notices = setOf("same"), rules = setOf("rule-1")), source.hiddenIds(today))
    }

    @Test
    fun `D-179 다음 날에는 다시 보이고 지난 날짜 기록은 정리한다`() = runBlocking {
        source.hideToday(PopupIdSpace.NOTICE, "old", today)
        source.hideToday(PopupIdSpace.UPDATE_RULE, "old-rule", today)

        val tomorrow = today.plusDays(1)
        assertEquals(HiddenTodayIds.None, source.hiddenIds(tomorrow))

        // 정리됐으므로 다시 오늘로 돌아가도(시계를 되돌려도) 남아 있지 않다
        assertEquals(HiddenTodayIds.None, source.hiddenIds(today))
        assertNull(settings.data.first().asMap().keys.firstOrNull { it.name.startsWith("popup_hidden_") })
    }

    @Test
    fun `D-179 읽을 수 없는 값은 정리한다`() = runBlocking {
        settings.edit { it[stringPreferencesKey("popup_hidden_notice_broken")] = "어제?" }

        assertEquals(HiddenTodayIds.None, source.hiddenIds(today))
        assertNull(settings.data.first().asMap().keys.firstOrNull { it.name.startsWith("popup_hidden_") })
    }

    @Test
    fun `D-179 로그아웃 정리에도 남는다`() = runBlocking {
        val other = stringPreferencesKey("sample_setting")
        settings.edit { it[other] = "사용자 설정" }
        source.hideToday(PopupIdSpace.NOTICE, "keep-me", today)
        source.hideToday(PopupIdSpace.UPDATE_RULE, "keep-rule", today)

        LocalDataCleaner(database, settings, Dispatchers.IO).clearUserData()

        assertEquals(HiddenTodayIds(setOf("keep-me"), setOf("keep-rule")), source.hiddenIds(today))
        assertNull(settings.data.first()[other])
    }
}
