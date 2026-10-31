package com.duriseo.android.data.repository.couple

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.errorJson
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.me.toEntity
import com.duriseo.android.data.repository.meJson
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.auth.TokenState
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.MemberColor
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** 커플 연결·해제·공간 설정 (API.md 3.2, 4.4, 4.5). 쓰기 성공 뒤 `GET /me`로 Room을 채운다 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CoupleRepositoryTest {
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

    private fun coupleMeJson(
        startDate: String? = null,
        myColor: String = "pick-orange",
        togetherColor: String = "pick-purple",
        importPending: Boolean = false,
    ): String {
        val start = startDate?.let { "\"$it\"" } ?: "null"
        return """{"id":"11111111-1111-1111-1111-111111111111","accountType":"MEMBER","nickname":"하나",
            "timeZone":"Asia/Seoul","providers":["GOOGLE"],"hasProfileImage":false,"createdAt":"2026-10-12T03:00:00Z",
            "space":{"id":"33333333-3333-3333-3333-333333333333","type":"COUPLE","myColor":"$myColor",
              "partner":{"id":"44444444-4444-4444-4444-444444444444","nickname":"둘","color":"pick-blue","hasProfileImage":false},
              "togetherColor":"$togetherColor","startDate":$start,"importPending":$importPending}}"""
    }

    private val joinResultJson = """{"spaceId":"33333333-3333-3333-3333-333333333333","restored":false,"importPending":false}"""

    private val spaceJson = """{"id":"33333333-3333-3333-3333-333333333333","type":"COUPLE","togetherColor":"pick-purple",
        "startDate":"2025-05-20","members":[{"userId":"u1","nickname":"하나","color":"pick-orange","hasProfileImage":false}]}"""

    private fun requestPaths(): List<String> = (0 until server.requestCount).map { server.takeRequest().let { r -> "${r.method} ${r.path}" } }

    @Test
    fun `FR-COUPLE-02 AC1 연결 응답 뒤 내 정보를 다시 읽어 Room에 넣는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(200, joinResultJson))
        server.enqueue(jsonResponse(200, coupleMeJson()))

        val result = h.coupleRepository.join("K7Q29X")

        assertEquals("33333333-3333-3333-3333-333333333333", (result as DataResult.Success).value.spaceId)
        assertEquals(listOf("POST /api/v1/couple/join", "GET /api/v1/me"), requestPaths())
        val me = h.roomMe()!!
        assertEquals(SpaceType.COUPLE, me.space.type)
        assertEquals("둘", me.space.partner?.nickname)
    }

    @Test
    fun `FR-COUPLE-02 AC1 연결이 실패하면 내 정보를 다시 읽지 않고 Room도 그대로다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(404, errorJson("INVITE_NOT_FOUND")))

        val result = h.coupleRepository.join("K7Q29X")

        assertEquals(ErrorCode.INVITE_NOT_FOUND, ((result as DataResult.Failure).error as DataError.ApiError).knownCode)
        assertEquals(1, server.requestCount)
        assertEquals(SpaceType.PERSONAL, h.roomMe()!!.space.type)
    }

    @Test
    fun `FR-COUPLE-02 AC1 연결은 됐는데 내 정보 읽기가 실패해도 연결 결과는 성공이다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, joinResultJson))
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))

        val result = h.coupleRepository.join("K7Q29X")

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun `FR-COUPLE-06 AC2 복구된 연결도 importPending이 true면 Room에 그대로 담긴다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, """{"spaceId":"s1","restored":true,"importPending":true}"""))
        server.enqueue(jsonResponse(200, coupleMeJson(importPending = true)))

        val result = h.coupleRepository.join("K7Q29X") as DataResult.Success

        assertTrue(result.value.restored)
        assertTrue(h.roomMe()!!.space.importPending)
    }

    @Test
    fun `FR-COUPLE-03 AC1 가져오기 결정 뒤 Room의 importPending이 false가 된다`() = runTestWithTimeout {
        val h = harness()
        h.meLocal.replace(fixtureMe().let { it.copy(space = it.space.copy(importPending = true)) }.toEntity())
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(200, coupleMeJson(importPending = false)))

        val result = h.coupleRepository.decideImport(true)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf("POST /api/v1/couple/import", "GET /api/v1/me"), requestPaths())
        assertEquals(false, h.roomMe()!!.space.importPending)
    }

    @Test
    fun `FR-COUPLE-04 AC1 PATCH space 뒤 Room의 처음 만난 날이 바뀐다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(200, spaceJson))
        server.enqueue(jsonResponse(200, coupleMeJson(startDate = "2025-05-20")))

        val result = h.coupleRepository.updateStartDate(LocalDate.of(2025, 5, 20))

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf("PATCH /api/v1/space", "GET /api/v1/me"), requestPaths())
        assertEquals(LocalDate.of(2025, 5, 20), h.roomMe()!!.space.startDate)
    }

    @Test
    fun `FR-CAL-04 AC2 PATCH space colors 뒤 Room의 색이 바뀐다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(200, spaceJson))
        server.enqueue(jsonResponse(200, coupleMeJson(myColor = "pick-rose", togetherColor = "pick-green")))

        val result = h.coupleRepository.updateColors(
            ColorChange(members = listOf(MemberColor("u1", "pick-rose")), together = "pick-green"),
        )

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf("PATCH /api/v1/space/colors", "GET /api/v1/me"), requestPaths())
        val space = h.roomMe()!!.space
        assertEquals("pick-rose", space.myColor)
        assertEquals("pick-green", space.togetherColor)
    }

    @Test
    fun `FR-CAL-04 AC2 바꾼 것이 없으면 요청하지 않고 성공이다`() = runTestWithTimeout {
        val h = harness()

        val result = h.coupleRepository.updateColors(ColorChange())

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `FR-COUPLE-05 AC3 해제하면 토큰은 남기고 Room과 설정을 지운 뒤 새 개인 공간을 받는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(id = "old-user", accountType = AccountType.MEMBER))
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(200, meJson(id = "old-user", accountType = "MEMBER")))

        val result = h.coupleRepository.unlink()

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf("DELETE /api/v1/couple", "GET /api/v1/me"), requestPaths())
        assertNull(h.settingValue())
        assertTrue(h.tokens.state.value is TokenState.Present)
        assertEquals(SpaceType.PERSONAL, h.roomMe()!!.space.type)
    }

    @Test
    fun `FR-COUPLE-05 AC3 NOT_COUPLE이면 이미 해제된 것으로 보고 초기화한다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(409, errorJson("NOT_COUPLE")))
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER")))

        val result = h.coupleRepository.unlink()

        assertEquals(DataResult.Success(Unit), result)
        assertNull(h.settingValue())
        assertTrue(h.tokens.state.value is TokenState.Present)
    }

    @Test
    fun `D-88 해제가 서버 오류로 실패하면 로컬 데이터를 지우지 않는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))

        val result = h.coupleRepository.unlink()

        assertTrue(result is DataResult.Failure)
        assertEquals("저장된 설정", h.settingValue())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 초대코드 만들기 연결 가져오기 결정 날짜 색 해제 요청을 보내지 않는다`() = runTestWithTimeout {
        val h = harness(online = false)

        val results = listOf(
            h.coupleRepository.issueInviteCode(),
            h.coupleRepository.join("K7Q29X"),
            h.coupleRepository.decideImport(true),
            h.coupleRepository.updateStartDate(LocalDate.of(2025, 5, 20)),
            h.coupleRepository.updateColors(ColorChange(together = "pick-green")),
            h.coupleRepository.unlink(),
        )

        results.forEach { assertEquals(DataResult.Failure(DataError.Offline), it) }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 해제 요청이 막혀 로컬 데이터가 그대로다`() = runTestWithTimeout {
        val h = harness(online = false)
        h.preload(fixtureMe(accountType = AccountType.MEMBER))

        h.coupleRepository.unlink()

        assertEquals("저장된 설정", h.settingValue())
    }

    @Test
    fun `FR-COUPLE-01 AC1 오프라인이면 현재 초대코드 읽기도 요청 없이 Offline이다`() = runTestWithTimeout {
        val h = harness(online = false)

        val result = h.coupleRepository.currentInviteCode()

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `FR-COUPLE-01 AC1 유효한 코드가 없으면 current는 null 성공이다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        val result = h.coupleRepository.currentInviteCode()

        assertEquals(DataResult.Success(null), result)
    }
}
