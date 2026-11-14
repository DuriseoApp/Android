package com.duriseo.android.data.repository.notification

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 알림 권한 "이미 물었는지" 기록 하나 (FR-NOTI-01 AC3, D-144). 설정 DataStore에 둔다 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationPermissionGateTest {
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

    private fun harness(): RepoHarness =
        RepoHarness(server, tempFolder.root, online = true, initialTokens = testTokens()).also { created = it }

    @Test
    fun `FR-NOTI-01 AC3 세 시점이 기록 하나를 함께 쓰고 한 번만 묻는다`() = runTestWithTimeout {
        val h = harness()

        // 일정 알림 설정, 연결 완료 시트를 닫은 직후, 직접 기념일 저장 직후가 같은 기록을 부른다: 동시에 불러도 한 번만 true
        val answers = (1..3).map { async { h.notificationPermissionGate.shouldAskNow() } }.awaitAll()

        assertEquals(1, answers.count { it })
        assertFalse(h.notificationPermissionGate.shouldAskNow())
    }

    @Test
    fun `FR-NOTI-01 AC3 처음에는 물어야 하고 로그아웃으로 설정을 지우면 다시 처음이다`() = runTestWithTimeout {
        val h = harness()
        assertTrue(h.notificationPermissionGate.shouldAskNow())
        assertFalse(h.notificationPermissionGate.shouldAskNow())

        // 로그아웃·탈퇴·세션 끊김은 설정 DataStore를 비운다 (LocalDataCleaner): 계정이 바뀌면 처음부터다
        h.settings.edit { it.clear() }

        assertTrue(h.notificationPermissionGate.shouldAskNow())
    }
}
