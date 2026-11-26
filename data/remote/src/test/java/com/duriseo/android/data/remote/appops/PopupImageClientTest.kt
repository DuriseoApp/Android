package com.duriseo.android.data.remote.appops

import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** unit-9 결정 3: 팝업 이미지 요청에는 토큰·공통 헤더(`X-*`)가 붙지 않는다 (운영자가 정한 주소가 우리 서버가 아닐 수 있다) */
class PopupImageClientTest {

    @Test
    fun `결정 3 팝업 이미지 요청에는 Authorization과 X 헤더가 없다`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("img"))
            server.start()
            val client = createPopupImageClient()

            client.newCall(Request.Builder().url(server.url("/banner.png")).build()).execute().use { response ->
                assertEquals(200, response.code)
            }

            val recorded = server.takeRequest()
            assertNull(recorded.getHeader("Authorization"))
            assertTrue(recorded.headers.names().none { it.startsWith("X-", ignoreCase = true) })
            assertTrue(client.interceptors.isEmpty())
            assertTrue(client.networkInterceptors.isEmpty())
        }
    }
}
