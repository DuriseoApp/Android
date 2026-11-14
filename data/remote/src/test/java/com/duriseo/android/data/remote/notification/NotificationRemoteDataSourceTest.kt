package com.duriseo.android.data.remote.notification

import com.duriseo.android.data.remote.FakeTokenStorage
import com.duriseo.android.data.remote.RecordingSessionEnd
import com.duriseo.android.data.remote.authedClient
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.errorJson
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.retrofitFor
import com.duriseo.android.data.remote.testTokens
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** 알림 API 3개 경로 (API.md 3.5) */
class NotificationRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: NotificationRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val client = authedClient(server, FakeTokenStorage(testTokens(access = "my-access")), RecordingSessionEnd())
        remote = NotificationRemoteDataSource(
            retrofitFor(server, client).create(NotificationApi::class.java),
            ApiCaller(createApiJson()),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun <T> DataResult<T>.value(): T = (this as DataResult.Success).value

    private fun item(id: String, type: String = "EVENT_REMINDER", targetType: String? = "EVENT", targetId: String? = "e1", read: Boolean = false) =
        """{"id":"$id","type":"$type","title":"제목 $id","body":"본문 $id",
        "targetType":${targetType?.let { "\"$it\"" } ?: "null"},"targetId":${targetId?.let { "\"$it\"" } ?: "null"},
        "read":$read,"createdAt":"2026-10-17T06:30:00Z"}"""

    @Test
    fun `API GET notifications는 cursor와 size를 쿼리로 보내고 항목을 서버 순서대로 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[${item("n2")},${item("n1", type = "COUPLE_LINKED", targetType = "SETTINGS", targetId = null, read = true)}],"nextCursor":"c2"}""",
            ),
        )

        val page = remote.list(cursor = "c1", size = 20).value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/notifications?cursor=c1&size=20", request.path)
        assertEquals("Bearer my-access", request.getHeader("Authorization"))
        assertEquals(listOf("n2", "n1"), page.items.map { it.id })
        assertEquals("c2", page.nextCursor)
        val first = page.items[0]
        assertEquals(NotificationKind.EVENT_REMINDER, first.kind)
        assertEquals(NotificationTarget(NotificationTargetType.EVENT, "e1"), first.target)
        assertFalse(first.read)
        assertEquals(Instant.parse("2026-10-17T06:30:00Z"), first.createdAt)
        // 연결 알림: targetType SETTINGS, targetId null
        assertEquals(NotificationTarget(NotificationTargetType.SETTINGS, null), page.items[1].target)
        assertTrue(page.items[1].read)
    }

    @Test
    fun `API GET notifications는 첫 페이지에 cursor를 보내지 않고 마지막 페이지의 nextCursor는 null이다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[${item("n1")}],"nextCursor":null}"""))

        val page = remote.list().value()

        assertEquals("/api/v1/notifications", server.takeRequest().path)
        assertNull(page.nextCursor)
    }

    @Test
    fun `API GET notifications는 모르는 종류를 건너뛰고 모르는 targetType은 이동 대상 없음으로 본다`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[${item("n1", type = "NEW_KIND")},${item("n2", targetType = "FUTURE", targetId = "x")}],"nextCursor":null}""",
            ),
        )

        val page = remote.list().value()

        assertEquals(listOf("n2"), page.items.map { it.id })
        assertNull(page.items[0].target)
    }

    @Test
    fun `API GET notifications unread-count는 count를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, """{"count":3}"""))

        assertEquals(3, remote.unreadCount().value())

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/notifications/unread-count", request.path)
    }

    @Test
    fun `API PUT notifications read는 본문 없이 보내고 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = remote.markRead("n1")

        assertTrue(result is DataResult.Success)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/notifications/n1/read", request.path)
        assertEquals(0L, request.bodySize)
    }

    @Test
    fun `API PUT notifications read의 404는 NOT_FOUND 오류다 R-12`() = runTest {
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        val error = (remote.markRead("nx") as DataResult.Failure).error as DataError.ApiError

        assertEquals(ErrorCode.NOT_FOUND, error.knownCode)
    }
}
