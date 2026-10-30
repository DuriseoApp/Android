package com.duriseo.android.data.repository.auth

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.auth.SessionEndReason
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 세션 끊김 (API.md 1장, FR-AUTH-02 AC3): 토큰·Room·설정·소셜 SDK 상태를 모두 지우고 한 번만 알린다 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionManagerTest {
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

    private fun harness(signedIn: Boolean = true): RepoHarness =
        RepoHarness(server, tempFolder.root, initialTokens = if (signedIn) testTokens() else null)
            .also { created = it }

    @Test
    fun `FR-AUTH-02 AC3 세션이 끝나면 토큰과 Room과 설정을 모두 지운다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe())
        val event = async(start = CoroutineStart.UNDISPATCHED) { h.session.events.take(1).toList() }

        h.session.end(SessionEndReason.REPLACED)

        assertEquals(listOf(SessionEndReason.REPLACED), withTimeoutOrNull(5_000) { event.await() })
        // 이벤트가 나온 시점에는 정리가 끝나 있다
        assertNull(h.tokens.current())
        assertNull(h.roomMe())
        assertNull(h.settingValue())
        assertEquals(1, h.social.clearCount.get())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `FR-AUTH-02 AC3 SESSION_INVALID로 끝나면 INVALID를 알린다`() = runTestWithTimeout {
        val h = harness()
        val event = async(start = CoroutineStart.UNDISPATCHED) { h.session.events.take(1).toList() }

        h.session.end(SessionEndReason.INVALID)

        assertEquals(listOf(SessionEndReason.INVALID), withTimeoutOrNull(5_000) { event.await() })
    }

    @Test
    fun `FR-AUTH-02 AC3 세션 종료는 한 번만 알린다`() = runTestWithTimeout {
        val h = harness()
        val events = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(1_000) { h.session.events.take(2).toList() }
        }

        h.session.end(SessionEndReason.REPLACED)
        h.session.end(SessionEndReason.INVALID)

        // 두 번째는 무시돼 2개가 모이지 않고 시간 초과(null)로 끝난다
        assertNull(events.await())
        assertEquals(1, h.social.clearCount.get())
    }

    @Test
    fun `토큰이 없으면 세션 종료를 알리지 않는다`() = runTestWithTimeout {
        val h = harness(signedIn = false)
        h.tokens.restore()
        val events = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(500) { h.session.events.take(1).toList() }
        }

        h.session.end(SessionEndReason.INVALID)

        assertNull(events.await())
        assertEquals(0, h.social.clearCount.get())
    }
}
