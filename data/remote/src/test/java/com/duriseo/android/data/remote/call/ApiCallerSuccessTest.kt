package com.duriseo.android.data.remote.call

import com.duriseo.android.data.remote.SampleApi
import com.duriseo.android.data.remote.SampleDto
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.sampleApi
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** 응답 형식 1.1 성공 경로 (API.md 1.1): 봉투 없는 단건, 목록, 페이지, 204 */
class ApiCallerSuccessTest {
    private lateinit var server: MockWebServer
    private lateinit var api: SampleApi
    private val caller = ApiCaller(createApiJson())

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = sampleApi(server)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun item(id: String) =
        """{"id":"$id","note":null,"createdAt":"2026-10-17T06:30:00Z"}"""

    @Test
    fun `API 1_1 단건 성공 응답은 봉투 없이 리소스로 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, item("a1")))

        val result = caller.call { api.getSample() }

        val dto = (result as DataResult.Success).value
        assertEquals("a1", dto.id)
        assertEquals(Instant.parse("2026-10-17T06:30:00Z"), dto.createdAt)
        assertEquals("/api/v1/sample", server.takeRequest().path)
    }

    @Test
    fun `API 1_1 페이지 목록은 items와 nextCursor를 읽고 마지막 페이지는 nextCursor가 null이다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[${item("a")},${item("b")}],"nextCursor":"c-2"}"""))
        server.enqueue(jsonResponse(200, """{"items":[${item("c")}],"nextCursor":null}"""))

        val first = (caller.call { api.getPage() } as DataResult.Success).value
        val last = (caller.call { api.getPage() } as DataResult.Success).value

        assertEquals(listOf("a", "b"), first.items.map(SampleDto::id))
        assertEquals("c-2", first.nextCursor)
        assertEquals(listOf("c"), last.items.map(SampleDto::id))
        assertNull(last.nextCursor)
    }

    @Test
    fun `API 1_1 개수가 필요한 페이지 목록은 totalCount도 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[${item("a")}],"nextCursor":null,"totalCount":7}"""))

        val page = (caller.call { api.getCounted() } as DataResult.Success).value

        assertEquals(7, page.totalCount)
        assertEquals(1, page.items.size)
    }

    @Test
    fun `API 1_1 페이지가 없는 목록은 items만 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[${item("a")}]}"""))

        val list = (caller.call { api.getItems() } as DataResult.Success).value

        assertEquals(listOf("a"), list.items.map(SampleDto::id))
    }

    @Test
    fun `API 1_1 204 응답은 본문 없이 성공이다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = caller.callUnit { api.delete() }

        assertEquals(DataResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/sample", request.path)
    }

    @Test
    fun `API 1_1 성공 응답 본문이 약속한 모양이 아니면 공통 오류로 본다`() = runTest {
        server.enqueue(jsonResponse(200, """{"unexpected":true}"""))

        val result = caller.call { api.getSample() }

        assertTrue(result is DataResult.Failure)
    }

    @Test
    fun `API 1_1 성공 응답의 일시 형식이 틀리면 INTERNAL_ERROR로 처리한다`() = runTest {
        server.enqueue(jsonResponse(200, """{"id":"a","note":null,"createdAt":"어제 오후"}"""))

        val result = caller.call { api.getSample() }

        val error = (result as DataResult.Failure).error as DataError.ApiError
        assertEquals(ErrorCode.INTERNAL_ERROR, error.knownCode)
    }
}
