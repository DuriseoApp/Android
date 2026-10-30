package com.duriseo.android.data.local.token

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

/** 키셋을 두는 SharedPreferences 파일 이름 (unit-2.md D절) */
internal const val KEYSET_PREFS_NAME = "duriseo_auth_keyset"

/** 키셋을 담는 SharedPreferences 항목 이름 */
internal const val KEYSET_PREF_KEY = "keyset"

/** 키셋을 감싸는 Android Keystore 마스터 키 */
internal const val MASTER_KEY_URI = "android-keystore://duriseo_auth_master_key"

/**
 * Android Keystore 마스터 키로 감싼 Tink 키셋(AES256_GCM)에서 [Aead]를 만든다.
 * 키셋이 없으면 새로 만들고, Keystore 키가 사라졌거나 키셋이 깨졌으면 예외를 던진다
 * ([EncryptedTokenStorage]가 받아서 저장소를 비운다). 실기기에서만 확인할 수 있다.
 */
internal fun createKeystoreAead(context: Context): Aead {
    AeadConfig.register()
    val keysetHandle = AndroidKeysetManager.Builder()
        .withSharedPref(context, KEYSET_PREF_KEY, KEYSET_PREFS_NAME)
        .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
        .withMasterKeyUri(MASTER_KEY_URI)
        .build()
        .keysetHandle
    return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
}
