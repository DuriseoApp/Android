package com.duriseo.android.data.repository.notification

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.domain.notification.NotificationKind
import com.duriseo.android.domain.notification.NotificationTarget
import com.duriseo.android.domain.notification.NotificationTargetType
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 누른 알림 처리: 읽음 + 갈 곳 (FR-NOTI-03, FR-NOTI-05 AC5·AC6) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationTapHandlerTest {
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

    private val notFound = """{"code":"NOT_FOUND","message":"x","details":null,"traceId":"t"}"""

    /**
     * 읽음 `PUT`이 뒤에서 나가 단건 `GET`과 순서가 섞이므로 경로로 응답한다:
     * 일정·메모 단건은 404, 읽음은 204, 안 읽은 개수는 0
     */
    private fun answerByPath() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/api/v1/events/") || path.startsWith("/api/v1/memos/") -> jsonResponse(404, notFound)
                    path.endsWith("/unread-count") -> jsonResponse(200, """{"count":0}""")
                    else -> MockResponse().setResponseCode(204)
                }
            }
        }
    }

    @Test
    fun `FR-NOTI-05 AC5 단건 GET이 404면 삭제된 일정이다`() = runTestWithTimeout {
        val h = harness()
        answerByPath()

        val destination = h.notificationTapHandler.open(
            "n1", NotificationKind.EVENT_REMINDER, NotificationTarget(NotificationTargetType.EVENT, "e1"),
        )

        assertEquals(NotificationDestination.EventDeleted, destination)
    }

    @Test
    fun `FR-NOTI-05 AC5 Room에 있어도 서버가 404면 삭제로 본다`() = runTestWithTimeout {
        val h = harness()
        // 기기에 일정이 이미 있다 (단건 GET 200으로 Room에 저장)
        server.enqueue(
            jsonResponse(
                200,
                """{"id":"e1","title":"일정","note":null,"isAllDay":false,"start":"2026-10-12T10:30:00Z","end":"2026-10-12T12:30:00Z",
                    "isTogether":false,"authorId":"11111111-1111-1111-1111-111111111111","updatedBy":null,"reminder":null,
                    "createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-02T00:00:00Z"}""",
            ),
        )
        h.eventRepository.refreshEvent("e1")
        assertTrue(h.eventRepository.observeEvent("e1").first() != null)
        answerByPath() // 이제 서버에서는 404

        val destination = h.notificationTapHandler.open(
            "n1", NotificationKind.EVENT_REMINDER, NotificationTarget(NotificationTargetType.EVENT, "e1"),
        )

        assertEquals(NotificationDestination.EventDeleted, destination)
        // 기기에서도 사라진다
        assertEquals(null, h.eventRepository.observeEvent("e1").first())
    }

    @Test
    fun `FR-NOTI-05 AC5 단건 GET이 404면 삭제된 메모다`() = runTestWithTimeout {
        val h = harness()
        answerByPath()

        val destination = h.notificationTapHandler.open(
            "n1", NotificationKind.MEMO_PINNED, NotificationTarget(NotificationTargetType.MEMO, "m1"),
        )

        assertEquals(NotificationDestination.MemoDeleted, destination)
    }

    @Test
    fun `FR-NOTI-05 AC6 오프라인이면 확인 없이 그대로 이동하고 읽음은 기기에서 바뀐다`() = runTestWithTimeout {
        val h = harness(online = false)

        val destination = h.notificationTapHandler.open(
            "n1", NotificationKind.PARTNER_CREATED, NotificationTarget(NotificationTargetType.EVENT, "e1"),
        )

        assertEquals(NotificationDestination.EventDetail("e1"), destination)
        assertEquals(0, server.requestCount)
        assertTrue(h.notificationRepository.observeNotifications().first().items.isEmpty())
    }

    @Test
    fun `FR-NOTI-03 AC1 연결과 기념일 알림은 설정과 기념일 목록으로 간다`() = runTestWithTimeout {
        val h = harness(online = false)

        val settings = h.notificationTapHandler.open("n1", NotificationKind.COUPLE_UNLINKED, NotificationTarget(NotificationTargetType.SETTINGS, null))
        val anniversary = h.notificationTapHandler.open("n2", NotificationKind.ANNIVERSARY, NotificationTarget(NotificationTargetType.ANNIVERSARY, null))

        assertEquals(NotificationDestination.Settings, settings)
        assertEquals(NotificationDestination.Anniversaries, anniversary)
    }
}
