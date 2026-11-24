package com.duriseo.android.data.repository.tutorial

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

/** 캘린더 튜토리얼 "봤음" 기록(기기 단위, 설정 DataStore)과 "다시 보기" 요청 (FR-CAL-06 AC4·AC5, FR-SET-02 AC5) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CalendarTutorialRepositoryTest {
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
    fun `FR-CAL-06 AC1 처음에는 튜토리얼을 보여 줘야 한다`() = runTestWithTimeout {
        val h = harness()

        assertTrue(h.calendarTutorialRepository.markShownIfFirst())
    }

    @Test
    fun `FR-CAL-06 AC4 처음 뜰 때 본 것으로 저장해서 두 번째부터는 보여 주지 않는다`() = runTestWithTimeout {
        val h = harness()

        assertTrue(h.calendarTutorialRepository.markShownIfFirst())
        assertFalse(h.calendarTutorialRepository.markShownIfFirst())
        assertFalse(h.calendarTutorialRepository.markShownIfFirst())
    }

    @Test
    fun `FR-CAL-06 AC4 동시에 불러도 한 번만 보여 준다`() = runTestWithTimeout {
        val h = harness()

        val answers = (1..3).map { async { h.calendarTutorialRepository.markShownIfFirst() } }.awaitAll()

        assertEquals(1, answers.count { it })
    }

    @Test
    fun `FR-CAL-06 AC4 로그아웃·탈퇴·연결 해제로 사용자 데이터를 지워도 본 기록은 남는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe())
        assertTrue(h.calendarTutorialRepository.markShownIfFirst())

        // 로그아웃·탈퇴·세션 끊김·앱 초기화는 같은 정리(LocalDataCleaner)를 거친다: 설정은 비우지만 기기 단위 기록은 남는다
        h.session.clearAll()

        assertNull(h.settingValue())
        assertFalse(h.calendarTutorialRepository.markShownIfFirst())
    }

    @Test
    fun `FR-SET-02 AC5 다시 보기를 요청하면 요청이 남고 가져가면 사라진다`() = runTestWithTimeout {
        val h = harness()
        val repo = h.calendarTutorialRepository
        assertFalse(repo.replayRequested.value)

        repo.requestReplay()
        assertTrue(repo.replayRequested.value)

        repo.consumeReplay()
        assertFalse(repo.replayRequested.value)
    }

    @Test
    fun `FR-SET-02 AC5 이미 본 기기에서도 다시 보기 요청은 받는다`() = runTestWithTimeout {
        val h = harness()
        val repo = h.calendarTutorialRepository
        assertTrue(repo.markShownIfFirst())

        repo.requestReplay()

        assertTrue(repo.replayRequested.value)
        // 다시 보기는 "봤음" 기록과 별개라 기록은 그대로다
        assertFalse(repo.markShownIfFirst())
    }

    @Test
    fun `FR-CAL-06 AC5 오프라인에서도 기록과 다시 보기가 같다`() = runTestWithTimeout {
        val h = RepoHarness(server, tempFolder.root, online = false, initialTokens = testTokens()).also { created = it }

        assertTrue(h.calendarTutorialRepository.markShownIfFirst())
        h.calendarTutorialRepository.requestReplay()
        assertTrue(h.calendarTutorialRepository.replayRequested.value)
        // 서버를 부르지 않는다
        assertEquals(0, server.requestCount)
    }
}
