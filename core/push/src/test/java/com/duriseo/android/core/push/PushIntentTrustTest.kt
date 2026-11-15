package com.duriseo.android.core.push

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.duriseo.android.core.common.push.PushIntentExtras
import com.duriseo.android.core.common.push.PushIntentTrust
import com.duriseo.android.core.common.push.clearPushExtras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 우리 알림이 만든 Intent만 푸시 진입으로 받는다 (FR-NOTI-03, Intent 위조 방지) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PushIntentTrustTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `FR-NOTI-03 우리 알림이 표시한 Intent만 믿는다`() {
        val stamped = PushIntentTrust.stamp(context, Intent().putExtra(PushIntentExtras.NOTIFICATION_ID, "n1"))
        val forged = Intent().putExtra(PushIntentExtras.NOTIFICATION_ID, "n1")
        val wrong = Intent().putExtra(PushIntentExtras.NOTIFICATION_ID, "n1").putExtra(PushIntentTrust.EXTRA, "guess")

        assertTrue(PushIntentTrust.isTrusted(context, stamped))
        assertFalse(PushIntentTrust.isTrusted(context, forged))
        assertFalse(PushIntentTrust.isTrusted(context, wrong))
        assertFalse(PushIntentTrust.isTrusted(context, null))
    }

    @Test
    fun `FR-NOTI-03 표시 값은 프로세스가 바뀌어도 같다`() {
        val first = PushIntentTrust.stamp(context, Intent()).getStringExtra(PushIntentTrust.EXTRA)
        val second = PushIntentTrust.stamp(context, Intent()).getStringExtra(PushIntentTrust.EXTRA)

        assertEquals(first, second)
    }

    @Test
    fun `FR-NOTI-03 처리한 엑스트라는 지운다`() {
        val intent = PushIntentTrust.stamp(context, Intent().putExtra(PushIntentExtras.NOTIFICATION_ID, "n1"))

        intent.clearPushExtras()

        assertFalse(PushIntentExtras.isPushIntent(intent))
        assertFalse(PushIntentTrust.isTrusted(context, intent))
    }
}
