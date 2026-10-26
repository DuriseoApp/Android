package com.duriseo.android.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 설정 DataStore. 토큰은 여기에 평문으로 두지 않는다 (암호화 저장은 단위 2, NFR-SEC-01).
 * 키는 쓰는 단위에서 `Preferences.Key`로 정의한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = createSettingsDataStore(
        produceFile = { context.preferencesDataStoreFile(SETTINGS_DATASTORE_NAME) },
    )
}
