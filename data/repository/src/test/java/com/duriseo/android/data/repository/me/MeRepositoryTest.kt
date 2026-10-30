package com.duriseo.android.data.repository.me

import app.cash.turbine.test
import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.meJson
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.result.DataResult
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

/** 내 정보 캐시와 시간대 갱신 (NFR-OFF-01, NFR-TIME-01). 메모리 Room은 Robolectric이 필요하다 (unit-2.md 확인 필요 15) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MeRepositoryTest {
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

    private fun harness(online: Boolean = true, zoneId: String = "Asia/Seoul"): RepoHarness =
        RepoHarness(server, tempFolder.root, online = online, zoneId = zoneId).also { created = it }

    @Test
    fun `GET me 결과를 Room에 저장하면 observeMe가 새 값을 낸다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, meJson(id = "user-1", accountType = "MEMBER")))

        h.meRepository.observeMe().test {
            assertNull(awaitItem())

            val result = h.meRepository.refreshMe()

            assertEquals(DataResult.Success(Unit), result)
            val me = awaitItem()
            assertEquals("user-1", me?.id)
            assertEquals(AccountType.MEMBER, me?.accountType)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("/api/v1/me", server.takeRequest().path)
    }

    @Test
    fun `GET me의 필드는 Room을 거쳐 그대로 돌아온다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, meJson(id = "user-1")))

        h.meRepository.refreshMe()

        assertEquals(fixtureMe(id = "user-1"), h.roomMe())
    }

    @Test
    fun `NFR-OFF-01 오프라인이면 refreshMe는 조용히 실패하고 저장된 내 정보는 그대로다`() = runTestWithTimeout {
        val h = harness(online = false)
        h.preload(fixtureMe(id = "cached-user"))

        val result = h.meRepository.refreshMe()

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
        assertEquals("cached-user", h.roomMe()?.id)
    }

    @Test
    fun `NFR-OFF-01 연결이 끊겨 요청이 실패해도 저장된 내 정보는 그대로다`() = runTestWithTimeout {
        val h = harness(online = true)
        h.preload(fixtureMe(id = "cached-user"))
        server.shutdown()

        val result = h.meRepository.refreshMe()

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals("cached-user", h.roomMe()?.id)
    }

    @Test
    fun `NFR-TIME-01 기기 시간대가 내 정보와 다르면 PATCH me timeZone을 보낸다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(timeZone = "Asia/Seoul"))
        server.enqueue(jsonResponse(200, meJson(timeZone = "Asia/Tokyo")))

        val result = h.meRepository.syncTimeZone("Asia/Tokyo")

        val request = server.takeRequest()
        assertEquals(DataResult.Success(Unit), result)
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/me", request.path)
        assertEquals("{\"timeZone\":\"Asia/Tokyo\"}", request.body.readUtf8())
        // 응답 Me가 Room에 들어간다
        assertEquals("Asia/Tokyo", h.roomMe()?.timeZone)
    }

    @Test
    fun `NFR-TIME-01 시간대가 같으면 보내지 않는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(timeZone = "Asia/Seoul"))

        val result = h.meRepository.syncTimeZone("Asia/Seoul")

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `NFR-TIME-01 저장된 내 정보가 없으면 보내지 않는다`() = runTestWithTimeout {
        val h = harness()

        val result = h.meRepository.syncTimeZone("Asia/Tokyo")

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `NFR-TIME-01 오프라인이면 조용히 건너뛴다`() = runTestWithTimeout {
        val h = harness(online = false)
        h.preload(fixtureMe(timeZone = "Asia/Seoul"))

        val result = h.meRepository.syncTimeZone("Asia/Tokyo")

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
        assertEquals("Asia/Seoul", h.roomMe()?.timeZone)
    }

    @Test
    fun `NFR-TIME-01 PATCH가 실패해도 저장된 내 정보는 그대로다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(timeZone = "Asia/Seoul"))
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"m","traceId":"t",
                    "details":{"fields":[{"field":"timeZone","reason":"INVALID"}]}}""",
            ),
        )

        val result = h.meRepository.syncTimeZone("Nowhere/City")

        assertEquals(true, result is DataResult.Failure)
        assertEquals("Asia/Seoul", h.roomMe()?.timeZone)
    }
}
