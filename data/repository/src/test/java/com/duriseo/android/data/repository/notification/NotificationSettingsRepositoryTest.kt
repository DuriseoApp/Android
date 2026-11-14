package com.duriseo.android.data.repository.notification

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.notification.NotificationSettingKind
import com.duriseo.android.domain.notification.NotificationSettings
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 알림 설정 `GET`·`PATCH` (FR-NOTI-02, D-145) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationSettingsRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private var created: RepoHarness? = null

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        created?.close()
        runCatching { server.shutdown() }
    }

    private fun harness(online: Boolean = true): RepoHarness =
        RepoHarness(server, tempFolder.root, online = online, initialTokens = testTokens()).also { created = it }

    private fun settingsJson(e: Boolean = true, c: Boolean = true, m: Boolean = true, a: Boolean = true) =
        """{"eventReminder":$e,"partnerCreated":$c,"memoPinned":$m,"anniversary":$a}"""

    @Test
    fun `FR-NOTI-02 AC2 한 번도 받지 못했으면 값이 없다`() = runTestWithTimeout {
        val h = harness()
        assertNull(h.notificationSettingsRepository.observe().first())
    }

    @Test
    fun `FR-NOTI-02 AC2 GET 응답을 기기에 저장해 구독한다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, settingsJson(c = false)))

        assertTrue(h.notificationSettingsRepository.refresh() is DataResult.Success)

        assertEquals("/api/v1/me/notification-settings", server.takeRequest().path)
        assertEquals(NotificationSettings(true, false, true, true), h.notificationSettingsRepository.observe().first())
    }

    @Test
    fun `FR-NOTI-02 AC2 PATCH는 바뀐 종류 하나만 보내고 응답을 저장한다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, settingsJson(m = false)))

        val result = h.notificationSettingsRepository.update(NotificationSettingKind.MEMO_PINNED, false)

        assertTrue(result is DataResult.Success)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("""{"memoPinned":false}""", request.body.readUtf8())
        assertFalse(h.notificationSettingsRepository.observe().first()!!.memoPinned)
    }

    @Test
    fun `FR-NOTI-02 AC2 PATCH가 실패하면 저장하지 않아 원래 값 그대로다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, settingsJson()))
        h.notificationSettingsRepository.refresh()
        server.enqueue(jsonResponse(500, """{"code":"INTERNAL_ERROR","message":"x","details":null,"traceId":"t"}"""))

        val result = h.notificationSettingsRepository.update(NotificationSettingKind.ANNIVERSARY, false)

        assertTrue(result is DataResult.Failure)
        assertTrue(h.notificationSettingsRepository.observe().first()!!.anniversary)
    }

    @Test
    fun `FR-NOTI-02 AC2 오프라인이면 요청하지 않고 Offline이다`() = runTestWithTimeout {
        val h = harness(online = false)

        val patch = h.notificationSettingsRepository.update(NotificationSettingKind.EVENT_REMINDER, false)
        val get = h.notificationSettingsRepository.refresh()

        assertEquals(DataResult.Failure(DataError.Offline), patch)
        assertEquals(DataResult.Failure(DataError.Offline), get)
        assertEquals(0, server.requestCount)
    }
}
