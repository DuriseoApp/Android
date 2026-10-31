package com.duriseo.android.data.local.token

import android.content.Context
import com.duriseo.android.core.common.Logger
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.KeyStore

/** 키셋을 두는 SharedPreferences 파일 이름 (unit-2.md D절) */
internal const val KEYSET_PREFS_NAME = "duriseo_auth_keyset"

/** 키셋을 담는 SharedPreferences 항목 이름 */
internal const val KEYSET_PREF_KEY = "keyset"

/** 키셋을 감싸는 Android Keystore 마스터 키 */
internal const val MASTER_KEY_URI = "android-keystore://duriseo_auth_master_key"

/** [MASTER_KEY_URI]의 Keystore 별칭 (`android-keystore://` 뒤) */
internal const val MASTER_KEY_ALIAS = "duriseo_auth_master_key"

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

/**
 * 복호화할 수 없는 키셋을 버린다: 키셋 SharedPreferences([KEYSET_PREFS_NAME])와 Keystore 마스터 키를 지운다.
 * 지우지 않으면 깨진 키셋을 읽다가 이후 저장도 계속 실패한다. 다음에 [createKeystoreAead]를 부르면 새로 만든다.
 * 이미 저장된 토큰은 복호화할 수 없는 상태라 어차피 쓰지 못한다. 한 단계가 실패해도 나머지는 계속한다.
 * 실기기(Keystore)에서만 확인할 수 있다 (unit-2.md).
 */
internal fun resetKeystoreKeyset(context: Context) {
    try {
        context.getSharedPreferences(KEYSET_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    } catch (e: Exception) {
        Logger.w("token", "키셋 저장소를 지우지 못했어요: ${e.javaClass.simpleName}")
    }
    try {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(MASTER_KEY_ALIAS)
    } catch (e: Exception) {
        Logger.w("token", "Keystore 마스터 키를 지우지 못했어요: ${e.javaClass.simpleName}")
    }
}
