package com.duriseo.android.core.common.push

import android.content.Context
import android.content.Intent
import java.security.MessageDigest
import java.util.UUID

/**
 * 우리 알림이 만든 Intent만 푸시 진입으로 받기 위한 표시. `MainActivity`는 런처 때문에 외부에 열려 있어
 * 다른 앱이 같은 엑스트라로 알림 읽음·이동을 일으킬 수 있다 (Intent 위조).
 *
 * 기기에 한 번 만든 무작위 값을 앱 전용 저장소에 두고, 알림 Intent에 [EXTRA]로 넣는다 ([stamp]).
 * 받는 쪽은 같은 값인지 확인한다 ([isTrusted]). 다른 앱은 이 값을 읽을 수 없다. 프로세스마다 새로 만들지 않는 까닭은,
 * 프로세스가 죽은 뒤에 알림을 눌러도 알림이 만들어질 때의 값이 그대로 맞아야 해서다. 값은 로그에 남기지 않는다.
 */
object PushIntentTrust {
    const val EXTRA = "pushTrust"

    private const val PREFS = "push_intent_trust"
    private const val KEY = "token"

    /** 알림 Intent에 표시를 넣는다 */
    fun stamp(context: Context, intent: Intent): Intent = intent.putExtra(EXTRA, token(context))

    /** 우리 알림이 만든 Intent인지. 표시가 없거나 다르면 false다 */
    fun isTrusted(context: Context, intent: Intent?): Boolean {
        val given = intent?.getStringExtra(EXTRA) ?: return false
        return MessageDigest.isEqual(given.toByteArray(), token(context).toByteArray())
    }

    @Synchronized
    private fun token(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }
        // 알림 Intent에 넣기 전에 저장을 마쳐, 프로세스가 죽어도 값이 달라지지 않게 한다
        return UUID.randomUUID().toString().also { prefs.edit().putString(KEY, it).commit() }
    }
}

/** 이미 처리한 푸시 엑스트라를 지운다: 화면 재생성·기록에서 다시 열기로 같은 알림을 또 처리하지 않게 한다 */
fun Intent.clearPushExtras() {
    removeExtra(PushIntentExtras.NOTIFICATION_ID)
    removeExtra(PushIntentExtras.KIND)
    removeExtra(PushIntentExtras.TARGET_TYPE)
    removeExtra(PushIntentExtras.TARGET_ID)
    removeExtra(PushIntentTrust.EXTRA)
}
