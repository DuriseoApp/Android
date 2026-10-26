package com.duriseo.android.data.local.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsDataStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val key = stringPreferencesKey("sample")

    @Test
    fun `DataStore는 저장한 값을 다시 읽는다`() = runTest {
        val file = File(tempFolder.root, "$SETTINGS_DATASTORE_NAME.preferences_pb")
        val dataStore = createSettingsDataStore(scope = backgroundScope) { file }

        assertNull(dataStore.data.first()[key])

        dataStore.edit { it[key] = "저장한 값" }

        assertEquals("저장한 값", dataStore.data.first()[key])
    }
}
