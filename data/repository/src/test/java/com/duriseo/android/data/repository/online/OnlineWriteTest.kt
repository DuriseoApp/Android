package com.duriseo.android.data.repository.online

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.DELETE

class OnlineWriteTest {

    private class FakeNetworkMonitor(private val online: Boolean) : NetworkMonitor {
        override val isOnline: Flow<Boolean> = flowOf(online)
        override fun isOnlineNow(): Boolean = online
    }

    /** 테스트용 쓰기 API (삭제는 204) */
    interface WriteApi {
        @DELETE("memos/1")
        suspend fun delete(): Response<Unit>
    }

    private lateinit var server: MockWebServer
    private lateinit var api: WriteApi
    private val apiCaller = ApiCaller(createApiJson())

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .client(OkHttpClient())
            .build()
            .create(WriteApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 쓰기 요청을 보내지 않고 Offline을 돌려준다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = FakeNetworkMonitor(online = false).runOnlineWrite {
            apiCaller.callUnit { api.delete() }
        }

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `NFR-OFF-02 온라인이면 쓰기 요청을 보낸다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = FakeNetworkMonitor(online = true).runOnlineWrite {
            apiCaller.callUnit { api.delete() }
        }

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(1, server.requestCount)
        assertEquals("DELETE", server.takeRequest().method)
    }
}
