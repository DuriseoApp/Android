package com.duriseo.android.data.local.cleanup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.duriseo.android.data.local.auth.LastSignInLocalDataSource
import com.duriseo.android.data.local.db.DuriseoDatabase
import com.duriseo.android.domain.me.SocialProvider
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

/** 로그아웃 정리(`clearUserData`)가 기기 단위 기록을 남기는지 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalDataCleanerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: DuriseoDatabase
    private lateinit var settings: DataStore<Preferences>
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DuriseoDatabase::class.java,
        ).allowMainThreadQueries().build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        settings = PreferenceDataStoreFactory.create(scope = scope) { File(tempFolder.root, "settings.preferences_pb") }
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
    }

    @Test
    fun `최근 로그인 수단은 로그아웃 정리에서 남는다`() = runBlocking {
        val lastSignIn = LastSignInLocalDataSource(settings)
        val other = stringPreferencesKey("sample_setting")
        settings.edit { it[other] = "사용자 설정" }
        lastSignIn.save(SocialProvider.KAKAO)

        LocalDataCleaner(database, settings, Dispatchers.IO).clearUserData()

        assertEquals(SocialProvider.KAKAO, lastSignIn.provider.first())
        assertNull(settings.data.first()[other])
    }
}
