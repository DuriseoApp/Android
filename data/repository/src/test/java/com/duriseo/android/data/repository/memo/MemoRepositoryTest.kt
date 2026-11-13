package com.duriseo.android.data.repository.memo

import com.duriseo.android.data.local.memo.MemoDao
import com.duriseo.android.data.local.memo.MemoEntity
import com.duriseo.android.data.local.memo.MemoLocalDataSource
import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.coupleMeJson
import com.duriseo.android.data.repository.errorJson
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.memo.FieldChange
import com.duriseo.android.domain.memo.MemoPatch
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.domain.memo.NewMemo
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/** 메모 리포지토리 (API.md 3.4 "메모"): Room 구독, 응답 순서 저장, 보기별 커서 페이지, 온라인 쓰기 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MemoRepositoryTest {
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

    private fun memo(
        id: String,
        title: String? = id,
        date: String? = null,
        pinned: Boolean = false,
        favorite: Boolean = false,
        author: String = "u1",
    ) = """{"id":"$id","title":${title?.let { "\"$it\"" } ?: "null"},"body":null,"memoDate":${date?.let { "\"$it\"" } ?: "null"},
        "authorId":"$author","isPinned":$pinned,"pinnedAt":${if (pinned) "\"2026-10-18T01:00:00Z\"" else "null"},"isFavorite":$favorite,
        "createdAt":"2026-10-08T00:12:00Z","updatedAt":"2026-10-08T00:12:00Z"}"""

    private fun page(cursor: String?, total: Int, favorites: Int, vararg m: String) =
        """{"items":[${m.joinToString(",")}],"nextCursor":${cursor?.let { "\"$it\"" } ?: "null"},"totalCount":$total,"favoriteCount":$favorites}"""

    private fun items(vararg m: String) = """{"items":[${m.joinToString(",")}]}"""

    private val october = LocalDate.of(2026, 9, 30) to LocalDate.of(2026, 11, 1)

    private suspend fun RepoHarness.titles(view: MemoView): List<String?> =
        memoRepository.observeList(view).first().items.map { it.title }

    private suspend fun RepoHarness.pinnedIds(): List<String> = memoRepository.observePinned().first().map { it.id }

    private suspend fun RepoHarness.datedIds(from: LocalDate = october.first, to: LocalDate = october.second): List<String> =
        memoRepository.observeDated(from, to).first().map { it.id }

    /** 모든 Room 읽기·쓰기가 예외인 창구 (서버는 성공했는데 기기 반영이 실패하는 경우를 만든다) */
    private class FailingMemoLocal(dao: MemoDao) : MemoLocalDataSource(dao) {
        private fun fail(): Nothing = throw IllegalStateException("Room 실패")

        override suspend fun nextCursor(listType: String): String? = fail()
        override suspend fun loadedViews(): List<String> = fail()
        override suspend fun replaceList(listType: String, items: List<MemoEntity>, nextCursor: String?, totalCount: Int, favoriteCount: Int) = fail()
        override suspend fun appendList(listType: String, items: List<MemoEntity>, nextCursor: String?, totalCount: Int, favoriteCount: Int) = fail()
        override suspend fun replacePinned(items: List<MemoEntity>) = fail()
        override suspend fun replaceDated(from: LocalDate, to: LocalDate, entities: List<MemoEntity>) = fail()
        override suspend fun replaceDetail(entity: MemoEntity) = fail()
        override suspend fun hasDetail(memoId: String): Boolean = fail()
        override suspend fun delete(memoId: String) = fail()
    }

    /** 경로별로 응답을 정하는 서버 (쓰기 뒤 여러 요청이 따라 나가서 순서에 기대지 않는다) */
    private class PathServer(val responses: Map<String, MockResponse>) : Dispatcher() {
        val requests = CopyOnWriteArrayList<String>()

        /** 요청 본문. 키는 "메서드 경로" */
        val bodies = ConcurrentHashMap<String, String>()

        override fun dispatch(request: RecordedRequest): MockResponse {
            val key = "${request.method} ${request.path?.substringBefore('?')}"
            requests += "${request.method} ${request.path}"
            bodies[key] = request.body.readUtf8()
            return responses[key] ?: MockResponse().setResponseCode(500)
        }
    }

    @Test
    fun `GET memos는 보기별로 첫 페이지를 교체하고 다음 페이지를 이어 붙인다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page("c1", 3, 0, memo("b"), memo("a"))))
        h.memoRepository.refreshList(MemoView.NEWEST)

        val first = h.memoRepository.observeList(MemoView.NEWEST).first()
        // 응답 순서가 그대로다 (이름 순서가 아님)
        assertEquals(listOf("b", "a"), first.items.map { it.id })
        assertTrue(first.hasMore)
        assertTrue(first.isLoaded)

        server.enqueue(jsonResponse(200, page(null, 3, 0, memo("0"))))
        h.memoRepository.loadMoreList(MemoView.NEWEST)

        val second = h.memoRepository.observeList(MemoView.NEWEST).first()
        assertEquals(listOf("b", "a", "0"), second.items.map { it.id })
        assertFalse(second.hasMore)
        server.takeRequest()
        assertEquals("/api/v1/memos?view=newest&cursor=c1", server.takeRequest().path)

        // 다음 페이지가 없으면 요청하지 않는다
        h.memoRepository.loadMoreList(MemoView.NEWEST)
        assertEquals(2, server.requestCount)

        // 다른 보기는 따로 저장한다: 오래된순을 받아도 최신순은 그대로다
        server.enqueue(jsonResponse(200, page(null, 3, 0, memo("0"), memo("a"), memo("b"))))
        h.memoRepository.refreshList(MemoView.OLDEST)
        assertEquals(listOf("0", "a", "b"), h.memoRepository.observeList(MemoView.OLDEST).first().items.map { it.id })
        assertEquals(listOf("b", "a", "0"), h.memoRepository.observeList(MemoView.NEWEST).first().items.map { it.id })
        assertEquals("/api/v1/memos?view=oldest", server.takeRequest().path)

        // 화면에 다시 들어가 첫 페이지를 받으면 이어 붙인 것까지 교체한다
        server.enqueue(jsonResponse(200, page("c9", 1, 0, memo("new"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        val replaced = h.memoRepository.observeList(MemoView.NEWEST).first()
        assertEquals(listOf("new"), replaced.items.map { it.id })
        assertTrue(replaced.hasMore)
    }

    @Test
    fun `totalCount와 favoriteCount를 저장한다`() = runTestWithTimeout {
        val h = harness()
        // 받기 전: 아직 안 받은 보기는 isLoaded가 false이고 개수는 0이다
        assertFalse(h.memoRepository.observeList(MemoView.FAVORITES).first().isLoaded)
        assertEquals(0, h.memoRepository.observeFavoriteCount().first())

        server.enqueue(jsonResponse(200, page("c1", 42, 5, memo("a", favorite = true))))
        h.memoRepository.refreshList(MemoView.FAVORITES)

        val list = h.memoRepository.observeList(MemoView.FAVORITES).first()
        assertEquals(42, list.totalCount)
        assertEquals(5, h.memoRepository.observeFavoriteCount().first())

        // 다음 페이지 응답의 개수로 갱신한다
        server.enqueue(jsonResponse(200, page(null, 41, 4, memo("b", favorite = true))))
        h.memoRepository.loadMoreList(MemoView.FAVORITES)
        assertEquals(41, h.memoRepository.observeList(MemoView.FAVORITES).first().totalCount)
        assertEquals(4, h.memoRepository.observeFavoriteCount().first())

        // 0개: 행이 없고 개수는 0이지만 받은 보기다
        server.enqueue(jsonResponse(200, page(null, 0, 0)))
        h.memoRepository.refreshList(MemoView.FAVORITES)
        val empty = h.memoRepository.observeList(MemoView.FAVORITES).first()
        assertTrue(empty.items.isEmpty())
        assertEquals(0, empty.totalCount)
        assertTrue(empty.isLoaded)
    }

    @Test
    fun `NFR-OFF-01 오프라인이면 목록 다시 불러오기는 조용히 실패하고 저장된 보기가 남는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, 1, 0, memo("a"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        h.monitor.online = false

        val result = h.memoRepository.refreshList(MemoView.NEWEST)

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(listOf<String?>("a"), h.titles(MemoView.NEWEST))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `pinned는 응답 순서대로 저장한다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, items(memo("z", pinned = true), memo("a", pinned = true), memo("m", pinned = true))))
        h.memoRepository.refreshPinned()

        assertEquals(listOf("z", "a", "m"), h.pinnedIds())

        // 다시 받으면 교체한다 (해제된 것은 사라진다)
        server.enqueue(jsonResponse(200, items(memo("a", pinned = true))))
        h.memoRepository.refreshPinned()
        assertEquals(listOf("a"), h.pinnedIds())
        assertEquals("/api/v1/memos/pinned", server.takeRequest().path)
    }

    @Test
    fun `dated는 범위를 교체하고 이웃 달을 지우지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, items(memo("late", date = "2026-10-20"), memo("early", date = "2026-10-05"), memo("same", date = "2026-10-20"))))
        h.memoRepository.refreshDated(october.first, october.second)

        // 메모 날짜순, 같은 날은 응답 위치 그대로
        assertEquals(listOf("early", "late", "same"), h.datedIds())

        val december = LocalDate.of(2026, 11, 30) to LocalDate.of(2027, 1, 1)
        server.enqueue(jsonResponse(200, items(memo("dec", date = "2026-12-05"))))
        h.memoRepository.refreshDated(december.first, december.second)

        // 10월을 다시 받으면 그 범위만 교체한다 (서버에서 지워진 것은 사라지고 12월은 그대로)
        server.enqueue(jsonResponse(200, items(memo("only", date = "2026-10-01"))))
        h.memoRepository.refreshDated(october.first, october.second)

        assertEquals(listOf("only"), h.datedIds())
        assertEquals(listOf("dec"), h.datedIds(december.first, december.second))
    }

    @Test
    fun `dated는 1950-01-01 ~ 2050-12-31 안으로 잘라 요청하고 밖이면 요청하지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, items()))

        h.memoRepository.refreshDated(LocalDate.of(1949, 12, 31), LocalDate.of(1950, 1, 31))
        val outside = h.memoRepository.refreshDated(LocalDate.of(2051, 1, 1), LocalDate.of(2051, 1, 31))

        assertEquals("/api/v1/memos/dated?from=1950-01-01&to=1950-01-31", server.takeRequest().path)
        assertTrue(outside is DataResult.Success)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `이웃 달 범위를 교체해도 서로의 날짜를 지우지 않는다`() = runTestWithTimeout {
        val h = harness()
        val november = LocalDate.of(2026, 10, 31) to LocalDate.of(2026, 12, 1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.queryParameter("from")) {
                "2026-09-30" -> jsonResponse(200, items(memo("oct", date = "2026-10-05"), memo("both", date = "2026-10-31")))
                else -> jsonResponse(200, items(memo("both", date = "2026-10-31"), memo("nov", date = "2026-11-20")))
            }
        }

        h.memoRepository.refreshDated(october.first, october.second)
        h.memoRepository.refreshDated(november.first, november.second)

        assertEquals(listOf("oct", "both", "nov"), h.datedIds(october.first, november.second))
    }

    @Test
    fun `상세를 받으면 저장하고 목록의 같은 메모보다 최근에 받은 값을 쓴다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, 1, 0, memo("a", title = "옛 제목"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        assertEquals("옛 제목", h.memoRepository.observeMemo("a").first()?.title)

        server.enqueue(jsonResponse(200, memo("a", title = "새 제목", favorite = true)))
        val result = h.memoRepository.refreshMemo("a")

        assertTrue(result is DataResult.Success)
        val detail = h.memoRepository.observeMemo("a").first()
        assertEquals("새 제목", detail?.title)
        assertTrue(detail!!.isFavorite)
        assertNull(h.memoRepository.observeMemo("none").first())
    }

    @Test
    fun `상세가 404면 기기에서 지운다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, 2, 1, memo("gone", favorite = true), memo("stay"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        server.enqueue(jsonResponse(200, page(null, 1, 1, memo("gone", favorite = true))))
        h.memoRepository.refreshList(MemoView.FAVORITES)
        server.enqueue(jsonResponse(200, items(memo("gone", pinned = true))))
        h.memoRepository.refreshPinned()
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        val result = h.memoRepository.refreshMemo("gone")

        assertTrue((result as DataResult.Failure).error.hasCode(ErrorCode.NOT_FOUND))
        assertNull(h.memoRepository.observeMemo("gone").first())
        assertEquals(listOf<String?>("stay"), h.titles(MemoView.NEWEST))
        assertEquals(emptyList<String?>(), h.titles(MemoView.FAVORITES))
        assertEquals(emptyList<String>(), h.pinnedIds())
        // 보기의 개수도 하나 줄어든다 (곧 이어지는 다시 불러오기가 맞춘다)
        assertEquals(1, h.memoRepository.observeList(MemoView.NEWEST).first().totalCount)
        assertEquals(0, h.memoRepository.observeFavoriteCount().first())
    }

    @Test
    fun `POST는 응답을 상세로 저장하고 notifyPartner는 POST에만 보낸다`() = runTestWithTimeout {
        val h = harness()
        val path = PathServer(
            mapOf(
                "POST /api/v1/memos" to jsonResponse(201, memo("n1", title = "새 메모")),
                "GET /api/v1/memos" to jsonResponse(200, page(null, 1, 0, memo("n1", title = "새 메모"))),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items()),
            ),
        )
        server.dispatcher = path

        val result = h.memoRepository.create(NewMemo("  새 메모 ", null, null, notifyPartner = true))

        assertEquals("n1", (result as DataResult.Success).value.id)
        val body = Json.parseToJsonElement(path.bodies.getValue("POST /api/v1/memos")).jsonObject
        assertEquals(setOf("title", "notifyPartner"), body.keys)
        assertEquals("새 메모", h.memoRepository.observeMemo("n1").first()?.title)
    }

    @Test
    fun `PATCH는 바꾼 필드만 보낸다`() = runTestWithTimeout {
        val h = harness()
        val path = PathServer(
            mapOf(
                "PATCH /api/v1/memos/a1" to jsonResponse(200, memo("a1", title = "새 제목")),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items()),
            ),
        )
        server.dispatcher = path

        val result = h.memoRepository.update("a1", MemoPatch(title = FieldChange.Set("새 제목")))

        assertTrue(result is DataResult.Success)
        val body = Json.parseToJsonElement(path.bodies.getValue("PATCH /api/v1/memos/a1")).jsonObject
        assertEquals(setOf("title"), body.keys)
        // 응답이 상세로 저장된다
        assertEquals("새 제목", h.memoRepository.observeMemo("a1").first()?.title)
    }

    @Test
    fun `FR-MEMO-03 AC4 PATCH는 바뀐 것이 없으면 요청 없이 성공이다`() = runTestWithTimeout {
        val h = harness()

        val result = h.memoRepository.update("a1", MemoPatch())

        assertTrue(result is DataResult.Success)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `쓰기 성공 뒤 목록 고정 달을 다시 불러온다`() = runTestWithTimeout {
        val h = harness()
        // 최신순·오래된순을 받아 두고 보이는 달을 알린다. 즐겨찾기 보기는 받은 적이 없다
        server.enqueue(jsonResponse(200, page(null, 1, 0, memo("x"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        server.enqueue(jsonResponse(200, page(null, 1, 0, memo("x"))))
        h.memoRepository.refreshList(MemoView.OLDEST)
        h.memoRepository.setVisibleRange(october.first, october.second)
        server.takeRequest()
        server.takeRequest()
        val path = PathServer(
            mapOf(
                "POST /api/v1/memos" to jsonResponse(201, memo("n1", date = "2026-10-20")),
                "GET /api/v1/memos" to jsonResponse(200, page(null, 2, 0, memo("n1", date = "2026-10-20"), memo("x"))),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items()),
                "GET /api/v1/memos/dated" to jsonResponse(200, items(memo("n1", date = "2026-10-20"))),
            ),
        )
        server.dispatcher = path

        h.memoRepository.create(NewMemo("n1", null, LocalDate.of(2026, 10, 20)))

        assertEquals(
            setOf(
                "POST /api/v1/memos",
                "GET /api/v1/memos?view=newest",
                "GET /api/v1/memos?view=oldest",
                "GET /api/v1/memos/pinned",
                "GET /api/v1/memos/dated?from=2026-09-30&to=2026-11-01",
            ),
            path.requests.toSet(),
        )
        assertEquals(listOf("n1", "x"), h.memoRepository.observeList(MemoView.NEWEST).first().items.map { it.id })
        assertEquals(listOf("n1"), h.datedIds())
    }

    @Test
    fun `쓰기 성공 뒤 다시 불러오다 예외가 나도 쓰기 결과는 성공이다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, 1, 0, memo("x"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        server.takeRequest()
        server.dispatcher = PathServer(
            mapOf(
                "DELETE /api/v1/memos/a1" to MockResponse().setResponseCode(204),
                // 깨진 본문: 파싱 실패가 예외로 번지더라도 쓰기 결과를 바꾸지 않는다
                "GET /api/v1/memos" to jsonResponse(200, "{깨진 JSON"),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items()),
            ),
        )

        val result = h.memoRepository.delete("a1")

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun `Room 반영이 실패해도 서버 성공이면 쓰기 결과는 성공이다`() = runTestWithTimeout {
        val h = harness()
        server.dispatcher = PathServer(
            mapOf(
                "POST /api/v1/memos" to jsonResponse(201, memo("n1")),
                "PATCH /api/v1/memos/n1" to jsonResponse(200, memo("n1")),
                "DELETE /api/v1/memos/n1" to MockResponse().setResponseCode(204),
            ),
        )
        // Room 호출이 모두 예외인 저장소 (DB를 닫는 것보다 결정적이다)
        val repo = MemoRepositoryImpl(h.memoRemote, FailingMemoLocal(h.database.memoDao()), h.monitor, h.cache)

        val created = repo.create(NewMemo("n1", null, null))
        val updated = repo.update("n1", MemoPatch(title = FieldChange.Set("새 제목")))
        val deleted = repo.delete("n1")

        assertTrue(created is DataResult.Success)
        assertTrue(updated is DataResult.Success)
        assertTrue(deleted is DataResult.Success)
    }

    @Test
    fun `삭제 성공 뒤 늦게 도착한 이전 상세 응답은 저장하지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, memo("a1")).setHeadersDelay(1500, TimeUnit.MILLISECONDS))
        val stale = async(Dispatchers.Default) { h.memoRepository.refreshMemo("a1") }
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS) != null)
        server.dispatcher = PathServer(
            mapOf(
                "DELETE /api/v1/memos/a1" to MockResponse().setResponseCode(204),
                "GET /api/v1/memos" to jsonResponse(200, page(null, 0, 0)),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items()),
            ),
        )

        val deleted = h.memoRepository.delete("a1")
        stale.await()

        assertTrue(deleted is DataResult.Success)
        assertNull(h.memoRepository.observeMemo("a1").first())
    }

    @Test
    fun `고정 목록을 받으면 받은 적이 있다고 표시하고 빈 목록도 그렇다`() = runTestWithTimeout {
        val h = harness()
        assertFalse(h.memoRepository.observePinnedLoaded().first())

        server.enqueue(jsonResponse(200, items()))
        h.memoRepository.refreshPinned()

        assertTrue(h.memoRepository.observePinnedLoaded().first())
        assertEquals(emptyList<String>(), h.pinnedIds())
    }

    @Test
    fun `삭제 성공은 기기에서도 지우고 목록을 다시 불러온다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, 2, 0, memo("a1"), memo("a2"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        server.takeRequest()
        // 다시 불러오기가 실패해도 지운 메모는 보이지 않는다
        server.dispatcher = PathServer(
            mapOf(
                "DELETE /api/v1/memos/a1" to MockResponse().setResponseCode(204),
                "GET /api/v1/memos" to jsonResponse(500, errorJson("INTERNAL_ERROR")),
                "GET /api/v1/memos/pinned" to jsonResponse(500, errorJson("INTERNAL_ERROR")),
            ),
        )

        val result = h.memoRepository.delete("a1")

        assertTrue(result is DataResult.Success)
        assertEquals(listOf<String?>("a2"), h.titles(MemoView.NEWEST))
        assertEquals(1, h.memoRepository.observeList(MemoView.NEWEST).first().totalCount)
    }

    @Test
    fun `고정과 즐겨찾기 뒤 목록과 개수를 다시 불러온다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, 1, 0, memo("a1"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        server.takeRequest()
        val path = PathServer(
            mapOf(
                "PUT /api/v1/memos/a1/pin" to MockResponse().setResponseCode(204),
                "DELETE /api/v1/memos/a1/pin" to MockResponse().setResponseCode(204),
                "PUT /api/v1/memos/a1/favorite" to MockResponse().setResponseCode(204),
                "DELETE /api/v1/memos/a1/favorite" to MockResponse().setResponseCode(204),
                "GET /api/v1/memos" to jsonResponse(200, page(null, 0, 0)),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items(memo("a1", pinned = true))),
            ),
        )
        server.dispatcher = path

        // 고정: 보기에서는 빠지고 고정 목록에 나온다 (D-130)
        assertTrue(h.memoRepository.setPinned("a1", true) is DataResult.Success)
        assertEquals(setOf("PUT /api/v1/memos/a1/pin", "GET /api/v1/memos?view=newest", "GET /api/v1/memos/pinned"), path.requests.toSet())
        assertEquals(emptyList<String?>(), h.titles(MemoView.NEWEST))
        assertEquals(listOf("a1"), h.pinnedIds())

        // 즐겨찾기: 개수를 다시 읽는다 (응답의 favoriteCount)
        val favoritePath = PathServer(
            path.responses + mapOf("GET /api/v1/memos" to jsonResponse(200, page(null, 0, 3))),
        )
        server.dispatcher = favoritePath
        assertTrue(h.memoRepository.setFavorite("a1", true) is DataResult.Success)
        assertTrue(favoritePath.requests.contains("PUT /api/v1/memos/a1/favorite"))
        assertEquals(3, h.memoRepository.observeFavoriteCount().first())
    }

    @Test
    fun `해제와 즐겨찾기 해제는 DELETE 경로를 쓴다`() = runTestWithTimeout {
        val h = harness()
        val path = PathServer(
            mapOf(
                "DELETE /api/v1/memos/a1/pin" to MockResponse().setResponseCode(204),
                "DELETE /api/v1/memos/a1/favorite" to MockResponse().setResponseCode(204),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items()),
            ),
        )
        server.dispatcher = path

        h.memoRepository.setPinned("a1", false)
        h.memoRepository.setFavorite("a1", false)

        assertTrue(path.requests.contains("DELETE /api/v1/memos/a1/pin"))
        assertTrue(path.requests.contains("DELETE /api/v1/memos/a1/favorite"))
    }

    @Test
    fun `상세를 저장해 둔 메모는 고정 뒤 상세도 다시 받는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, memo("a1", pinned = false)))
        h.memoRepository.refreshMemo("a1")
        server.takeRequest()
        server.dispatcher = PathServer(
            mapOf(
                "PUT /api/v1/memos/a1/pin" to MockResponse().setResponseCode(204),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items()),
                "GET /api/v1/memos/a1" to jsonResponse(200, memo("a1", pinned = true)),
            ),
        )

        h.memoRepository.setPinned("a1", true)

        assertTrue(h.memoRepository.observeMemo("a1").first()!!.isPinned)
    }

    @Test
    fun `D-121 쓰기가 404면 기기에서 지운다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, 2, 0, memo("a1"), memo("a2"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        val result = h.memoRepository.setPinned("a1", true)

        assertTrue((result as DataResult.Failure).error.hasCode(ErrorCode.NOT_FOUND))
        assertEquals(listOf<String?>("a2"), h.titles(MemoView.NEWEST))
    }

    @Test
    fun `D-121 수정이 403이면 목록과 상세를 다시 불러온다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, 1, 0, memo("a1", author = "u1"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        server.takeRequest()
        val path = PathServer(
            mapOf(
                "PATCH /api/v1/memos/a1" to jsonResponse(403, errorJson("NOT_AUTHOR")),
                "GET /api/v1/memos" to jsonResponse(200, page(null, 1, 0, memo("a1", title = "상대 것", author = "u2"))),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items()),
                "GET /api/v1/memos/a1" to jsonResponse(200, memo("a1", title = "상대 것", author = "u2")),
            ),
        )
        server.dispatcher = path

        val result = h.memoRepository.update("a1", MemoPatch(title = FieldChange.Set("x")))

        assertTrue((result as DataResult.Failure).error.hasCode(ErrorCode.NOT_AUTHOR))
        assertTrue(path.requests.contains("GET /api/v1/memos/a1"))
        assertEquals("u2", h.memoRepository.observeMemo("a1").first()?.authorId)
        assertEquals("상대 것", h.memoRepository.observeMemo("a1").first()?.title)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 메모 쓰기 요청을 보내지 않고 안내한다`() = runTestWithTimeout {
        val h = harness(online = false)

        val results = listOf(
            h.memoRepository.create(NewMemo("제목", null, null)),
            h.memoRepository.update("a1", MemoPatch(title = FieldChange.Set("x"))),
            h.memoRepository.delete("a1"),
            h.memoRepository.setPinned("a1", true),
            h.memoRepository.setFavorite("a1", true),
        )

        results.forEach { assertEquals(DataResult.Failure(DataError.Offline), it) }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `보이는 범위는 마지막으로 알린 값이고 캐시를 지우면 비워진다`() = runTestWithTimeout {
        val h = harness()
        val november = LocalDate.of(2026, 10, 31) to LocalDate.of(2026, 12, 1)
        h.memoRepository.setVisibleRange(october.first, october.second)
        h.memoRepository.setVisibleRange(november.first, november.second)
        val path = PathServer(
            mapOf(
                "DELETE /api/v1/memos/a1" to MockResponse().setResponseCode(204),
                "GET /api/v1/memos/pinned" to jsonResponse(200, items()),
                "GET /api/v1/memos/dated" to jsonResponse(200, items()),
            ),
        )
        server.dispatcher = path

        h.memoRepository.delete("a1")
        assertTrue(path.requests.contains("GET /api/v1/memos/dated?from=2026-10-31&to=2026-12-01"))
        assertFalse(path.requests.contains("GET /api/v1/memos/dated?from=2026-09-30&to=2026-11-01"))

        h.session.clearUserData()
        path.requests.clear()
        h.memoRepository.delete("a1")
        assertFalse(path.requests.any { it.startsWith("GET /api/v1/memos/dated") })
    }

    @Test
    fun `캐시를 지운 뒤 도착한 이전 응답은 저장하지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, 1, 0, memo("late"))).setHeadersDelay(1500, TimeUnit.MILLISECONDS))

        val stale = async(Dispatchers.Default) { h.memoRepository.refreshList(MemoView.NEWEST) }
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS) != null)
        h.session.clearUserData() // 로그아웃·초기화처럼 캐시를 지운다
        val result = stale.await()

        assertTrue(result is DataResult.Success)
        val list = h.memoRepository.observeList(MemoView.NEWEST).first()
        assertEquals(emptyList<String>(), list.items.map { it.id })
        assertFalse(list.isLoaded)
    }

    @Test
    fun `캐시를 지운 뒤 도착한 이전 날짜 범위 응답은 저장하지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, items(memo("late", date = "2026-10-20"))).setHeadersDelay(1500, TimeUnit.MILLISECONDS))

        val stale = async(Dispatchers.Default) { h.memoRepository.refreshDated(october.first, october.second) }
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS) != null)
        h.session.clearUserData()
        stale.await()

        assertEquals(emptyList<String>(), h.datedIds())
    }

    @Test
    fun `로그아웃 정리는 메모 테이블을 모두 지운다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page("c1", 1, 1, memo("a", favorite = true))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        server.enqueue(jsonResponse(200, items(memo("p", pinned = true))))
        h.memoRepository.refreshPinned()
        server.enqueue(jsonResponse(200, memo("a")))
        h.memoRepository.refreshMemo("a")

        h.session.clearUserData()

        val list = h.memoRepository.observeList(MemoView.NEWEST).first()
        assertTrue(list.items.isEmpty())
        assertFalse(list.isLoaded)
        assertFalse(list.hasMore)
        assertEquals(emptyList<String>(), h.pinnedIds())
        assertNull(h.memoRepository.observeMemo("a").first())
        assertEquals(0, h.memoRepository.observeFavoriteCount().first())
    }

    @Test
    fun `FR-COUPLE-03 공간이 바뀌면 이전 공간의 메모 캐시를 비운다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(200, page(null, 1, 1, memo("personal", favorite = true))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        // 연결: 개인 공간 -> 커플 공간
        server.enqueue(jsonResponse(200, coupleMeJson()))

        h.meRepository.refreshMe()

        val list = h.memoRepository.observeList(MemoView.NEWEST).first()
        assertEquals(emptyList<String>(), list.items.map { it.id })
        assertFalse(list.isLoaded)
        assertEquals(0, h.memoRepository.observeFavoriteCount().first())
    }

    @Test
    fun `FR-COUPLE-03 가져오기를 정하면 메모 캐시를 비우고 다시 받게 한다`() = runTestWithTimeout {
        val h = harness()
        h.preload(com.duriseo.android.data.repository.fixtureCoupleMe(importPending = true))
        server.enqueue(jsonResponse(200, page(null, 1, 0, memo("a"))))
        h.memoRepository.refreshList(MemoView.NEWEST)
        server.enqueue(MockResponse().setResponseCode(204)) // POST /couple/import
        server.enqueue(jsonResponse(200, coupleMeJson()))

        h.coupleRepository.decideImport(true)

        assertEquals(emptyList<String?>(), h.titles(MemoView.NEWEST))
    }

    @Test
    fun `해제 뒤 초기화는 메모 캐시를 지운다`() = runTestWithTimeout {
        val h = harness()
        h.preload(com.duriseo.android.data.repository.fixtureCoupleMe())
        server.enqueue(jsonResponse(200, items(memo("a", pinned = true))))
        h.memoRepository.refreshPinned()
        server.enqueue(MockResponse().setResponseCode(204)) // DELETE /couple
        server.enqueue(jsonResponse(200, com.duriseo.android.data.repository.meJson(accountType = "MEMBER")))

        h.coupleRepository.unlink()

        assertEquals(emptyList<String>(), h.pinnedIds())
    }
}
