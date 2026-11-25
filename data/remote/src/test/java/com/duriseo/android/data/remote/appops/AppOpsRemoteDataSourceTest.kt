package com.duriseo.android.data.remote.appops

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.noAuthClient
import com.duriseo.android.data.remote.retrofitFor
import com.duriseo.android.domain.appops.AppPlatform
import com.duriseo.android.domain.appops.BuildCondition
import com.duriseo.android.domain.appops.OsCondition
import com.duriseo.android.domain.appops.PopupImage
import com.duriseo.android.domain.appops.PopupLink
import com.duriseo.android.domain.appops.UpdateMode
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** 공개 조회 `GET /app-notices`, `GET /app-update-rules` (API.md 3.7) */
class AppOpsRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: AppOpsRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        remote = AppOpsRemoteDataSource(
            retrofitFor(server, noAuthClient(server)).create(AppOpsApi::class.java),
            ApiCaller(createApiJson()),
            Dispatchers.IO,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun <T> DataResult<T>.value(): T = (this as DataResult.Success).value

    @Test
    fun `API 3_7 공지 목록을 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[{"id":"maint-1","closable":false,"title":"점검 중이에요","body":"곧 돌아와요","image":"preset:maintenance",
                "imageAlt":null,"linkText":"자세히","linkUrl":"https://example.com/notice","platform":null,
                "startsAt":"2026-11-25T00:00:00Z","endsAt":null}]}""",
            ),
        )

        val notices = remote.getNotices().value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/app-notices", request.path)
        assertNull(request.getHeader("Authorization"))
        assertEquals(1, notices.size)
        val notice = notices.single()
        assertEquals("maint-1", notice.id)
        assertEquals(false, notice.closable)
        assertEquals("점검 중이에요", notice.title)
        assertEquals(PopupLink("자세히", "https://example.com/notice"), notice.link)
        assertNull(notice.platform)
        assertEquals(Instant.parse("2026-11-25T00:00:00Z"), notice.startsAt)
        assertNull(notice.endsAt)
        assertTrue(notice.image is PopupImage.Preset)
    }

    @Test
    fun `API 3_7 규칙 목록을 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[{"id":"force-1","mode":"FORCE","platform":"ANDROID",
                "build":{"op":"RANGE","value":null,"min":5,"max":9,"values":null},
                "osVersion":{"min":30,"max":null},"title":null,"body":null,
                "image":"https://example.com/a.png","imageAlt":"업데이트 안내"}]}""",
            ),
        )

        val rule = remote.getRules().value().single()

        assertEquals("/api/v1/app-update-rules", server.takeRequest().path)
        assertEquals(UpdateMode.FORCE, rule.mode)
        assertEquals(AppPlatform.ANDROID, rule.platform)
        assertEquals(BuildCondition.Range(5, 9), rule.build)
        assertEquals(OsCondition(min = 30, max = null), rule.os)
        assertNull(rule.title)
        assertEquals(PopupImage.Web("https://example.com/a.png"), rule.image)
        assertEquals("업데이트 안내", rule.imageAlt)
    }

    @Test
    fun `API 3_7 모르는 필드는 무시한다`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[{"id":"a","closable":true,"title":"안내","body":null,"future":{"x":1},"newField":[1,2]}],"nextCursor":null}""",
            ),
        )

        assertEquals("a", remote.getNotices().value().single().id)
    }

    @Test
    fun `API 3_7 서버 오류는 실패로 돌려준다`() = runTest {
        server.enqueue(jsonResponse(500, """{"code":"INTERNAL_ERROR","message":"x","details":null,"traceId":null}"""))

        val result = remote.getNotices()

        assertTrue((result as DataResult.Failure).error is DataError.ApiError)
    }
}
