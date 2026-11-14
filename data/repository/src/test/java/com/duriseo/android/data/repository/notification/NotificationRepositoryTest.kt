package com.duriseo.android.data.repository.notification

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/** 알림 목록 리포지토리 (API.md 3.5): Room 구독, 서버 순서 저장, 커서 페이지, 안 읽은 개수, 읽음은 기기 먼저 (D-127) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationRepositoryTest {
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

    /** 하니스 시계는 2026-10-30T00:00:00Z다. 기본 일시는 13일 전이라 30일 안이다 */
    private fun item(id: String, read: Boolean = false, createdAt: String = "2026-10-17T06:30:00Z", type: String = "EVENT_REMINDER") =
        """{"id":"$id","type":"$type","title":"제목 $id","body":"본문 $id","targetType":"EVENT","targetId":"e-$id",
        "read":$read,"createdAt":"$createdAt"}"""

    private fun page(cursor: String?, vararg items: String) =
        """{"items":[${items.joinToString(",")}],"nextCursor":${cursor?.let { "\"$it\"" } ?: "null"}}"""

    private suspend fun RepoHarness.ids(): List<String> = notificationRepository.observeNotifications().first().items.map { it.id }

    @Test
    fun `첫 페이지는 교체하고 다음 페이지는 이어 붙인다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page("c1", item("n3"), item("n2"))))
        h.notificationRepository.refreshFirstPage()

        val first = h.notificationRepository.observeNotifications().first()
        // 서버 순서가 그대로다
        assertEquals(listOf("n3", "n2"), first.items.map { it.id })
        assertTrue(first.hasMore)
        assertTrue(first.isLoaded)

        server.enqueue(jsonResponse(200, page(null, item("n1"))))
        h.notificationRepository.loadMore()

        val all = h.notificationRepository.observeNotifications().first()
        assertEquals(listOf("n3", "n2", "n1"), all.items.map { it.id })
        assertFalse(all.hasMore)
        server.takeRequest()
        assertEquals("/api/v1/notifications?cursor=c1", server.takeRequest().path)

        // 첫 페이지를 다시 받으면 교체한다 (다음 페이지로 이어 붙인 것도 사라진다)
        server.enqueue(jsonResponse(200, page("c9", item("n4"))))
        h.notificationRepository.refreshFirstPage()
        assertEquals(listOf("n4"), h.ids())
    }

    @Test
    fun `GET notifications는 다음 페이지가 없으면 요청 없이 성공이다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, item("n1"))))
        h.notificationRepository.refreshFirstPage()
        server.takeRequest()

        val result = h.notificationRepository.loadMore()

        assertTrue(result is DataResult.Success)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `GET notifications는 다음 페이지에서 이미 있는 알림을 겹쳐 넣지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page("c1", item("n3"), item("n2"))))
        h.notificationRepository.refreshFirstPage()
        // 새 알림이 생겨 페이지 경계가 밀려 n2가 다음 페이지에 또 온다
        server.enqueue(jsonResponse(200, page(null, item("n2", read = true), item("n1"))))
        h.notificationRepository.loadMore()

        val items = h.notificationRepository.observeNotifications().first().items
        assertEquals(listOf("n3", "n2", "n1"), items.map { it.id })
        // 이미 있던 행은 그대로다
        assertFalse(items[1].read)
    }

    @Test
    fun `캐시를 지운 뒤 도착한 이전 응답은 저장하지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, item("late"))).setHeadersDelay(1500, TimeUnit.MILLISECONDS))

        val stale = async(Dispatchers.Default) { h.notificationRepository.refreshFirstPage() }
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS) != null)
        h.session.clearUserData() // 로그아웃·계정 전환처럼 캐시를 지운다
        val result = stale.await()

        assertTrue(result is DataResult.Success)
        val list = h.notificationRepository.observeNotifications().first()
        assertEquals(emptyList<String>(), list.items.map { it.id })
        assertFalse(list.isLoaded)
    }

    @Test
    fun `캐시를 지운 뒤 도착한 이전 안 읽은 개수 응답은 저장하지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, """{"count":5}""").setHeadersDelay(1500, TimeUnit.MILLISECONDS))

        val stale = async(Dispatchers.Default) { h.notificationRepository.refreshUnreadCount() }
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS) != null)
        h.session.clearUserData()
        stale.await()

        assertEquals(0, h.notificationRepository.observeUnreadCount().first())
    }

    @Test
    fun `로그아웃 정리는 알림 테이블을 모두 지운다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page("c1", item("n1"))))
        h.notificationRepository.refreshFirstPage()
        server.enqueue(jsonResponse(200, """{"count":1}"""))
        h.notificationRepository.refreshUnreadCount()

        h.session.clearUserData()

        val list = h.notificationRepository.observeNotifications().first()
        assertTrue(list.items.isEmpty())
        assertFalse(list.isLoaded)
        assertFalse(list.hasMore)
        assertEquals(0, h.notificationRepository.observeUnreadCount().first())
    }

    @Test
    fun `GET notifications unread-count는 온라인이면 서버가 알려 준 개수를 쓴다`() = runTestWithTimeout {
        val h = harness()
        // 목록에는 안 읽은 것이 1개뿐이지만 서버는 3개라고 한다 (아직 목록에 안 받은 것이 있다)
        server.enqueue(jsonResponse(200, page("c1", item("n1"))))
        h.notificationRepository.refreshFirstPage()
        server.enqueue(jsonResponse(200, """{"count":3}"""))
        h.notificationRepository.refreshUnreadCount()

        assertEquals(3, h.notificationRepository.observeUnreadCount().first())
    }

    @Test
    fun `D-141 오프라인이면 안 읽은 개수는 기기에 저장된 30일 안의 안 읽은 알림 개수다`() = runTestWithTimeout {
        val h = harness(online = true)
        server.enqueue(jsonResponse(200, page(null, item("n1"), item("n2", read = true), item("n3"))))
        h.notificationRepository.refreshFirstPage()
        server.enqueue(jsonResponse(200, """{"count":9}"""))
        h.notificationRepository.refreshUnreadCount()

        h.monitor.online = false

        assertEquals(2, h.notificationRepository.observeUnreadCount().first())
    }

    @Test
    fun `D-141 기기에 저장된 30일이 지난 알림은 목록과 개수에서 빠지고 지워진다`() = runTestWithTimeout {
        val h = harness()
        // 하니스 시계 2026-10-30 기준: 2026-09-29는 31일 전, 2026-10-01은 29일 전
        server.enqueue(jsonResponse(200, page(null, item("old", createdAt = "2026-09-29T00:00:00Z"), item("new", createdAt = "2026-10-01T00:00:00Z"))))
        h.notificationRepository.refreshFirstPage()

        assertEquals(listOf("new"), h.ids())

        h.monitor.online = false
        // 오프라인에서 다시 불러오려 해도 지우는 일은 한다 (요청은 나가지 않는다)
        h.notificationRepository.refreshFirstPage()
        assertEquals(1, h.notificationRepository.observeUnreadCount().first())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `오프라인이면 읽기는 요청 없이 Offline이다`() = runTestWithTimeout {
        val h = harness(online = false)

        assertEquals(DataResult.Failure(DataError.Offline), h.notificationRepository.refreshFirstPage())
        assertEquals(DataResult.Failure(DataError.Offline), h.notificationRepository.loadMore())
        assertEquals(DataResult.Failure(DataError.Offline), h.notificationRepository.refreshUnreadCount())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `FR-NOTI-05 AC6 markRead는 기기에서 먼저 읽음으로 바꾸고 안 읽은 개수를 하나 줄인다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, item("n2"), item("n1"))))
        h.notificationRepository.refreshFirstPage()
        server.enqueue(jsonResponse(200, """{"count":2}"""))
        h.notificationRepository.refreshUnreadCount()
        server.takeRequest()
        server.takeRequest()
        // 서버는 늦게 응답한다: 그 사이에도 기기에서는 이미 읽음이다
        server.enqueue(MockResponse().setResponseCode(204).setHeadersDelay(1500, TimeUnit.MILLISECONDS))
        server.enqueue(jsonResponse(200, """{"count":1}"""))

        h.notificationRepository.markRead("n1")

        val items = h.notificationRepository.observeNotifications().first().items
        assertTrue(items.first { it.id == "n1" }.read)
        assertFalse(items.first { it.id == "n2" }.read)
        assertEquals(1, h.notificationRepository.observeUnreadCount().first())
        // 서버 요청은 뒤에서 나간다
        val put = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(put)
        assertEquals("PUT", put!!.method)
        assertEquals("/api/v1/notifications/n1/read", put.path)
        // 성공하면 안 읽은 개수를 서버 값으로 다시 맞춘다
        assertEquals("/api/v1/notifications/unread-count", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
    }

    @Test
    fun `FR-NOTI-05 AC6 읽음 요청이 실패해도 기기의 읽음은 그대로이고 예외 없이 끝난다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, item("n1"))))
        h.notificationRepository.refreshFirstPage()
        server.takeRequest()
        server.enqueue(jsonResponse(500, """{"code":"INTERNAL_ERROR","message":"x","details":null,"traceId":"t"}"""))

        h.notificationRepository.markRead("n1")

        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertTrue(h.notificationRepository.observeNotifications().first().items.single().read)
    }

    @Test
    fun `FR-NOTI-05 AC6 오프라인이면 기기에서만 읽음으로 바꾸고 요청은 보내지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, page(null, item("n1"))))
        h.notificationRepository.refreshFirstPage()
        h.monitor.online = false

        h.notificationRepository.markRead("n1")

        assertTrue(h.notificationRepository.observeNotifications().first().items.single().read)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `D-142 목록에 없는 알림도 markRead하면 서버에 읽음을 보낸다 푸시를 눌러 들어온 경우`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(200, """{"count":0}"""))

        h.notificationRepository.markRead("pushed")

        val put = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(put)
        assertEquals("/api/v1/notifications/pushed/read", put!!.path)
        assertEquals("/api/v1/notifications/unread-count", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
    }

    @Test
    fun `D-141 알림 목록이 열려 있는지를 저장소가 기억한다`() = runTestWithTimeout {
        val h = harness()
        assertFalse(h.notificationRepository.isListOpen)

        h.notificationRepository.setListOpen(true)
        assertTrue(h.notificationRepository.isListOpen)

        h.notificationRepository.setListOpen(false)
        assertFalse(h.notificationRepository.isListOpen)
    }
}
