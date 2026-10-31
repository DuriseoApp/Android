package com.duriseo.android.data.local.token

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.duriseo.android.data.local.cleanup.LocalDataCleaner
import com.duriseo.android.domain.auth.TokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Provider
import javax.inject.Singleton

/** 토큰 DataStore 파일 이름. 설정 DataStore(`duriseo_settings`)와 파일이 따로다. */
internal const val TOKEN_DATASTORE_NAME = "auth_tokens"

/** 토큰용 DataStore. 설정 DataStore와 섞이지 않게 따로 만든다. */
internal fun createTokenDataStore(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope, produceFile = produceFile)

@Module
@InstallIn(SingletonComponent::class)
object TokenStorageModule {

    /** 토큰 DataStore는 밖으로 내놓지 않는다 (`DataStore<Preferences>` 바인딩은 설정용 하나뿐). */
    @Provides
    @Singleton
    fun provideTokenStorage(
        @ApplicationContext context: Context,
        // Room을 토큰 저장소를 만드는 시점에 열지 않으려고 Provider로 늦게 받는다
        localData: Provider<LocalDataCleaner>,
    ): TokenStorage = EncryptedTokenStorage(
        dataStore = createTokenDataStore { context.preferencesDataStoreFile(TOKEN_DATASTORE_NAME) },
        aeadProvider = { createKeystoreAead(context) },
        resetKeys = { resetKeystoreKeyset(context) },
        clearUserData = { localData.get().clearUserData() },
    )
}
