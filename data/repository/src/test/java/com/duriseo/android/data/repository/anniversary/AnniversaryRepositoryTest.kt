package com.duriseo.android.data.repository.anniversary

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.errorJson
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.anniversary.AnniversaryPatch
import com.duriseo.android.domain.anniversary.NewAnniversary
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList

/** 기념일 리포지토리 (API.md 3.3 "기념일"): Room 구독, 응답 순서 저장, 커서 페이지, 온라인 쓰기 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnniversaryRepositoryTest {
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

    private fun occ(
        name: String,
        date: String,
        id: String? = null,
        daysLeft: Int = 10,
        yearly: Boolean = false,
        author: String? = null,
    ) = """{"anniversaryId":${id?.let { "\"$it\"" } ?: "null"},"kind":"${if (id == null) "AUTO" else "CUSTOM"}","name":"$name",
        "date":"$date","daysLeft":$daysLeft,"repeatsYearly":$yearly,"authorId":${author?.let { "\"$it\"" } ?: "null"}}"""

    private fun items(vararg o: String) = """{"items":[${o.joinToString(",")}]}"""

    private fun page(cursor: String?, vararg o: String) =
        """{"items":[${o.joinToString(",")}],"nextCursor":${cursor?.let { "\"$it\"" } ?: "null"}}"""

    private fun pastPage(cursor: String?, total: Int, vararg o: String) =
        """{"items":[${o.joinToString(",")}],"nextCursor":${cursor?.let { "\"$it\"" } ?: "null"},"totalCount":$total}"""

    private fun pastItem(id: String, name: String, date: String) =
        """{"anniversaryId":"$id","name":"$name","date":"$date","authorId":"11111111-1111-1111-1111-111111111111"}"""

    private val october = LocalDate.of(2026, 9, 30) to LocalDate.of(2026, 11, 1)

    private suspend fun RepoHarness.rangeNames(from: LocalDate = october.first, to: LocalDate = october.second): List<String> =
        anniversaryRepository.observeRange(from, to).first().map { it.name }

    private suspend fun RepoHarness.upcomingNames(): List<String> =
        anniversaryRepository.observeUpcoming().first().items.map { it.name }

    /** 경로별로 응답을 정하는 서버 (쓰기 뒤 여러 요청이 따라 나가서 순서에 기대지 않는다) */
    private class PathServer(val responses: Map<String, MockResponse>) : Dispatcher() {
        val requests = CopyOnWriteArrayList<String>()

        /** 요청 본문. 키는 "메서드 경로" */
        val bodies = java.util.concurrent.ConcurrentHashMap<String, String>()

        override fun dispatch(request: RecordedRequest): MockResponse {
            val key = "${request.method} ${request.path?.substringBefore('?')}"
            requests += "${request.method} ${request.path}"
            bodies[key] = request.body.readUtf8()
            return responses[key] ?: MockResponse().setResponseCode(500)
        }
    }

    @Test
    fun `GET anniversaries는 응답 순서를 위치로 저장하고 범위를 교체한다`() = runTestWithTimeout {
        val h = harness()
        // 같은 날 응답 순서: 자동 200일 -> 직접 "가" -> 직접 "A". 이름 순서와 다르다
        server.enqueue(
            jsonResponse(
                200,
                items(
                    occ("200일", "2026-10-23"),
                    occ("가", "2026-10-23", id = "z-id", author = "u1"),
                    occ("A", "2026-10-23", id = "a-id", author = "u1"),
                    occ("추석", "2026-10-05", id = "c-id", author = "u1"),
                ),
            ),
        )
        h.anniversaryRepository.refreshRange(october.first, october.second)

        // 날짜순이고, 같은 날 안은 응답 위치 그대로다
        assertEquals(listOf("추석", "200일", "가", "A"), h.rangeNames())

        // 다시 받으면 그 범위를 지우고 새로 넣는다 (서버에서 지워진 것은 사라진다)
        server.enqueue(jsonResponse(200, items(occ("A", "2026-10-23", id = "a-id", author = "u1"), occ("200일", "2026-10-23"))))
        h.anniversaryRepository.refreshRange(october.first, october.second)

        assertEquals(listOf("A", "200일"), h.rangeNames())
    }

    @Test
    fun `GET anniversaries는 범위 밖 발생일을 건드리지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, items(occ("10월", "2026-10-10", id = "a", author = "u1"))))
        h.anniversaryRepository.refreshRange(october.first, october.second)
        val december = LocalDate.of(2026, 11, 30) to LocalDate.of(2027, 1, 1)
        server.enqueue(jsonResponse(200, items(occ("12월", "2026-12-05", id = "b", author = "u1"))))
        h.anniversaryRepository.refreshRange(december.first, december.second)

        server.enqueue(jsonResponse(200, items()))
        h.anniversaryRepository.refreshRange(october.first, october.second)

        assertEquals(emptyList<String>(), h.rangeNames())
        assertEquals(listOf("12월"), h.rangeNames(december.first, december.second))
    }

    @Test
    fun `GET anniversaries는 1950-01-01 ~ 2050-12-31 안으로 잘라 요청하고 밖이면 요청하지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, items()))

        h.anniversaryRepository.refreshRange(LocalDate.of(1949, 12, 31), LocalDate.of(1950, 1, 31))
        val outside = h.anniversaryRepository.refreshRange(LocalDate.of(2051, 1, 1), LocalDate.of(2051, 1, 31))

        assertEquals("/api/v1/anniversaries?from=1950-01-01&to=1950-01-31", server.takeRequest().path)
        assertTrue(outside is DataResult.Success)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `NFR-OFF-01 오프라인이면 다시 불러오기는 조용히 실패하고 저장된 기념일이 남는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, occ("200일", "2026-10-23"))))
        h.anniversaryRepository.refreshUpcoming()
        h.monitor.online = false

        val result = h.anniversaryRepository.refreshUpcoming()

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(listOf("200일"), h.upcomingNames())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `upcoming 첫 페이지는 목록을 교체하고 다음 페이지는 이어 붙인다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page("c1", occ("200일", "2026-10-23"), occ("생일", "2026-11-20", id = "b", author = "u1"))))
        h.anniversaryRepository.refreshUpcoming()

        val first = h.anniversaryRepository.observeUpcoming().first()
        assertEquals(listOf("200일", "생일"), first.items.map { it.name })
        assertTrue(first.hasMore)

        server.enqueue(jsonResponse(200, page(null, occ("300일", "2027-01-31"), occ("1주년", "2027-04-07"))))
        h.anniversaryRepository.loadMoreUpcoming()

        val second = h.anniversaryRepository.observeUpcoming().first()
        assertEquals(listOf("200일", "생일", "300일", "1주년"), second.items.map { it.name })
        assertFalse(second.hasMore)
        server.takeRequest()
        assertEquals("/api/v1/anniversaries/upcoming?cursor=c1", server.takeRequest().path)

        // 다음 페이지가 없으면 요청하지 않는다
        h.anniversaryRepository.loadMoreUpcoming()
        assertEquals(2, server.requestCount)

        // 화면에 다시 들어가 첫 페이지를 받으면 이어 붙인 것까지 교체한다
        server.enqueue(jsonResponse(200, page("c9", occ("100일", "2026-07-16"))))
        h.anniversaryRepository.refreshUpcoming()
        val replaced = h.anniversaryRepository.observeUpcoming().first()
        assertEquals(listOf("100일"), replaced.items.map { it.name })
        assertTrue(replaced.hasMore)
    }

    @Test
    fun `past totalCount를 저장하고 0이면 행이 없다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(
            jsonResponse(200, pastPage("p1", 3, pastItem("p-1", "제주 여행 출발", "2026-08-01"), pastItem("p-2", "영화", "2026-06-14"))),
        )
        h.anniversaryRepository.refreshPast()

        val first = h.anniversaryRepository.observePast().first()
        // 개수는 페이지와 관계없는 전체 개수다
        assertEquals(3, first.totalCount)
        assertEquals(listOf("제주 여행 출발", "영화"), first.items.map { it.name })
        assertTrue(first.hasMore)

        server.enqueue(jsonResponse(200, pastPage(null, 3, pastItem("p-3", "시험", "2026-03-01"))))
        h.anniversaryRepository.loadMorePast()
        val second = h.anniversaryRepository.observePast().first()
        assertEquals(listOf("제주 여행 출발", "영화", "시험"), second.items.map { it.name })
        assertFalse(second.hasMore)
        server.takeRequest()
        assertEquals("/api/v1/anniversaries/past?cursor=p1", server.takeRequest().path)

        server.enqueue(jsonResponse(200, pastPage(null, 0)))
        h.anniversaryRepository.refreshPast()
        val empty = h.anniversaryRepository.observePast().first()
        assertEquals(0, empty.totalCount)
        assertTrue(empty.items.isEmpty())
    }

    @Test
    fun `POST는 응답 뒤 목록과 달을 다시 불러온다`() = runTestWithTimeout {
        val h = harness()
        // 보이는 달을 먼저 불러 둔다
        server.enqueue(jsonResponse(200, items()))
        h.anniversaryRepository.refreshRange(october.first, october.second)
        server.takeRequest()
        val path = PathServer(
            mapOf(
                "POST /api/v1/anniversaries" to jsonResponse(201, anniversaryJson("n1", "새 기념일", "2026-10-20")),
                "GET /api/v1/anniversaries/upcoming" to jsonResponse(200, page(null, occ("새 기념일", "2026-10-20", id = "n1", author = "u1"))),
                "GET /api/v1/anniversaries/past" to jsonResponse(200, pastPage(null, 0)),
                "GET /api/v1/anniversaries" to jsonResponse(200, items(occ("새 기념일", "2026-10-20", id = "n1", author = "u1"))),
            ),
        )
        server.dispatcher = path

        val result = h.anniversaryRepository.create(NewAnniversary("새 기념일", LocalDate.of(2026, 10, 20), repeatsYearly = true))

        assertEquals("n1", (result as DataResult.Success).value.id)
        assertEquals(
            setOf(
                "POST /api/v1/anniversaries",
                "GET /api/v1/anniversaries/upcoming",
                "GET /api/v1/anniversaries/past",
                "GET /api/v1/anniversaries?from=2026-09-30&to=2026-11-01",
            ),
            path.requests.toSet(),
        )
        assertEquals(listOf("새 기념일"), h.upcomingNames())
        assertEquals(listOf("새 기념일"), h.rangeNames())
    }

    @Test
    fun `PATCH는 바꾼 필드만 보낸다`() = runTestWithTimeout {
        val h = harness()
        val path = PathServer(
            mapOf(
                "PATCH /api/v1/anniversaries/a1" to jsonResponse(200, anniversaryJson("a1", "새 이름", "2025-11-20")),
                "GET /api/v1/anniversaries/upcoming" to jsonResponse(200, page(null)),
                "GET /api/v1/anniversaries/past" to jsonResponse(200, pastPage(null, 0)),
            ),
        )
        server.dispatcher = path

        val result = h.anniversaryRepository.update("a1", AnniversaryPatch(name = "새 이름"))

        assertTrue(result is DataResult.Success)
        // 날짜·반복은 보내지 않는다 (등록한 날짜를 알 수 없어서, unit-5.md 확인 필요 1)
        val body = Json.parseToJsonElement(path.bodies.getValue("PATCH /api/v1/anniversaries/a1")).jsonObject
        assertEquals(setOf("name"), body.keys)
    }

    @Test
    fun `PATCH는 바뀐 것이 없으면 요청 없이 성공이다`() = runTestWithTimeout {
        val h = harness()

        val result = h.anniversaryRepository.update("a1", AnniversaryPatch())

        assertTrue(result is DataResult.Success)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `쓰기 성공 뒤 목록과 달을 다시 불러온다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, occ("삭제될 것", "2026-10-20", id = "a1", author = "u1"))))
        h.anniversaryRepository.refreshUpcoming()
        server.takeRequest()
        val path = PathServer(
            mapOf(
                "DELETE /api/v1/anniversaries/a1" to MockResponse().setResponseCode(204),
                "GET /api/v1/anniversaries/upcoming" to jsonResponse(200, page(null)),
                "GET /api/v1/anniversaries/past" to jsonResponse(200, pastPage(null, 0)),
            ),
        )
        server.dispatcher = path

        val result = h.anniversaryRepository.delete("a1")

        assertTrue(result is DataResult.Success)
        assertEquals(
            setOf("DELETE /api/v1/anniversaries/a1", "GET /api/v1/anniversaries/upcoming", "GET /api/v1/anniversaries/past"),
            path.requests.toSet(),
        )
        assertEquals(emptyList<String>(), h.upcomingNames())
    }

    @Test
    fun `FR-ANNIV-03 AC5 404면 기기에서 지운다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(
            jsonResponse(
                200,
                page(null, occ("있던 것", "2026-10-20", id = "a1", author = "u1"), occ("남을 것", "2026-10-21", id = "a2", author = "u1")),
            ),
        )
        h.anniversaryRepository.refreshUpcoming()
        server.enqueue(jsonResponse(200, pastPage(null, 1, pastItem("a1", "있던 것", "2026-01-01"))))
        h.anniversaryRepository.refreshPast()
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        val result = h.anniversaryRepository.update("a1", AnniversaryPatch(name = "x"))

        assertTrue((result as DataResult.Failure).error.hasCode(ErrorCode.NOT_FOUND))
        assertEquals(listOf("남을 것"), h.upcomingNames())
        val past = h.anniversaryRepository.observePast().first()
        assertTrue(past.items.isEmpty())
        assertEquals(0, past.totalCount)
    }

    @Test
    fun `FR-ANNIV-03 AC5 403이면 목록과 달을 다시 불러온다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, items()))
        h.anniversaryRepository.refreshRange(october.first, october.second)
        server.takeRequest()
        val path = PathServer(
            mapOf(
                "DELETE /api/v1/anniversaries/a1" to jsonResponse(403, errorJson("NOT_AUTHOR")),
                "GET /api/v1/anniversaries/upcoming" to jsonResponse(200, page(null, occ("상대 것", "2026-10-20", id = "a1", author = "u2"))),
                "GET /api/v1/anniversaries/past" to jsonResponse(200, pastPage(null, 0)),
                "GET /api/v1/anniversaries" to jsonResponse(200, items()),
            ),
        )
        server.dispatcher = path

        val result = h.anniversaryRepository.delete("a1")

        assertTrue((result as DataResult.Failure).error.hasCode(ErrorCode.NOT_AUTHOR))
        assertEquals(4, path.requests.size)
        assertEquals(listOf("상대 것"), h.upcomingNames())
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 기념일 저장 요청을 보내지 않고 안내한다`() = runTestWithTimeout {
        val h = harness(online = false)

        val result = h.anniversaryRepository.create(NewAnniversary("이름", LocalDate.of(2026, 10, 20)))

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
    }

    private fun anniversaryJson(id: String, name: String, date: String) =
        """{"id":"$id","name":"$name","date":"$date","repeatsYearly":true,"authorId":"11111111-1111-1111-1111-111111111111",
            "createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-02T00:00:00Z"}"""
}
