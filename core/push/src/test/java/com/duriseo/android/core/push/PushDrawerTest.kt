package com.duriseo.android.core.push

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.duriseo.android.domain.auth.AuthTokens
import com.duriseo.android.domain.auth.TokenState
import com.duriseo.android.domain.auth.TokenStorage
import com.duriseo.android.domain.notification.NotificationKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** 로그인 세션이 있을 때만 푸시를 그리고, 로그아웃하면 그려진 알림을 모두 지운다 (NFR-SEC-02) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PushDrawerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    private class FakeTokenStorage(initial: TokenState) : TokenStorage {
        val mutableState = MutableStateFlow(initial)
        override val state: StateFlow<TokenState> = mutableState
        override suspend fun restore() = Unit
        override suspend fun save(tokens: AuthTokens) = Unit
        override suspend fun clear() = Unit
    }

    private fun tokens() = AuthTokens(
        accessToken = "a",
        accessTokenExpiresAt = Instant.parse("2026-11-14T00:00:00Z"),
        refreshToken = "r",
        refreshTokenExpiresAt = Instant.parse("2026-12-14T00:00:00Z"),
    )

    private val message = PushMessage.Visible(
        notificationId = "n1",
        kind = NotificationKind.PARTNER_CREATED,
        title = "제목",
        body = "본문",
        target = null,
    )

    @Test
    fun `NFR-SEC-02 세션이 없으면 PUSH를 그리지 않는다`() = runBlocking {
        val drawer = PushDrawer(PushNotifier(), FakeTokenStorage(TokenState.Empty))

        val drawn = drawer.draw(context, message)

        assertFalse(drawn)
        assertEquals(0, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun `NFR-SEC-02 세션이 있으면 PUSH를 그린다`() = runBlocking {
        val tokens = tokens()
        val drawer = PushDrawer(PushNotifier(), FakeTokenStorage(TokenState.Present(tokens)))

        val drawn = drawer.draw(context, message)

        assertTrue(drawn)
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun `NFR-SEC-02 로그아웃하면 그려진 알림을 모두 지운다`() = runBlocking {
        val tokens = tokens()
        PushDrawer(PushNotifier(), FakeTokenStorage(TokenState.Present(tokens))).draw(context, message)
        assertEquals(1, shadowOf(manager).allNotifications.size)

        PushNotificationCleaner(context).cancelAll()

        assertEquals(0, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun `NFR-SEC-02 그리는 사이 로그아웃되면 그린 알림을 지운다`() = runBlocking {
        val storage = FakeTokenStorage(TokenState.Present(tokens()))
        // 세션을 확인한 뒤 그리는 도중에 로그아웃(토큰 삭제)이 끼어든 상황
        val notifier = object : PushNotifier() {
            override fun show(context: Context, message: PushMessage.Visible): Boolean {
                val drawn = super.show(context, message)
                storage.mutableState.value = TokenState.Empty
                return drawn
            }
        }

        val drawn = PushDrawer(notifier, storage).draw(context, message)

        assertFalse(drawn)
        assertEquals(0, shadowOf(manager).allNotifications.size)
    }
}
