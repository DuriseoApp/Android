package com.duriseo.android.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** 설정 DataStore 파일 이름 (확장자 `.preferences_pb`는 DataStore가 붙인다) */
internal const val SETTINGS_DATASTORE_NAME = "duriseo_settings"

/**
 * 설정용 DataStore Preferences를 만든다 (ARCHITECTURE.md 1장 `data:local`).
 * Hilt 모듈과 테스트가 같은 함수를 쓰도록 파일과 스코프를 밖에서 받는다.
 *
 * @param produceFile 저장 파일. 확장자는 `.preferences_pb`여야 한다.
 */
internal fun createSettingsDataStore(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    scope = scope,
    produceFile = produceFile,
)
