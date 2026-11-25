package com.duriseo.android.data.remote.network

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.me.MeApi
import com.duriseo.android.data.remote.me.MeRemoteDataSource
import com.duriseo.android.data.remote.noAuthClient
import com.duriseo.android.data.remote.retrofitFor
import com.duriseo.android.domain.appops.ForceUpdateSignal
import com.duriseo.android.domain.appops.PopupImage
import com.duriseo.android.domain.appops.PopupPreset
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 426 `APP_VERSION_UNSUPPORTED` (API.md 1.1, 4.9 7단계, R-20, FR-APP-02 AC6) */
class ForceUpdateInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var hub: ForceUpdateSignalHub
    private lateinit var remote: MeRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        hub = ForceUpdateSignalHub()
        val client = noAuthClient(server, forceUpdate = ForceUpdateInterceptor(hub, createApiJson()))
        remote = MeRemoteDataSource(retrofitFor(server, client).create(MeApi::class.java), ApiCaller(createApiJson()))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun body(details: String) =
        """{"code":"APP_VERSION_UNSUPPORTED","message":"개발자용","details":$details,"traceId":"t-1"}"""

    @Test
    fun `FR-APP-02 AC6 426을 받으면 강제 업데이트 신호를 낸다`() = runTest {
        server.enqueue(
            jsonResponse(
                426,
                body("""{"ruleId":"force-1","title":"꼭 업데이트","body":"새 버전이 있어요","image":"preset:update","imageAlt":null,"extra":1}"""),
            ),
        )

        val result = remote.getMe()

        assertEquals(
            ForceUpdateSignal("force-1", "꼭 업데이트", "새 버전이 있어요", PopupImage.Preset(PopupPreset.UPDATE), null),
            hub.current.value,
        )
        // 응답 본문은 그대로 읽혀 ApiCaller가 같은 오류로 바꾼다 (토큰·로컬 데이터는 건드리지 않는다)
        val error = (result as DataResult.Failure).error as DataError.ApiError
        assertEquals(ErrorCode.APP_VERSION_UNSUPPORTED, error.knownCode)
    }

    @Test
    fun `FR-APP-02 AC6 이미지만 넣은 규칙은 https 이미지와 imageAlt를 담는다`() = runTest {
        server.enqueue(
            jsonResponse(
                426,
                body("""{"ruleId":"banner","title":null,"body":null,"image":"https://cdn.example.com/a.png","imageAlt":"업데이트 안내"}"""),
            ),
        )

        remote.getMe()

        assertEquals(
            ForceUpdateSignal("banner", null, null, PopupImage.Web("https://cdn.example.com/a.png"), "업데이트 안내"),
            hub.current.value,
        )
    }

    @Test
    fun `FR-APP-02 AC6 details가 비면 기본 문구 표시용 null이다`() = runTest {
        server.enqueue(jsonResponse(426, body("null")))

        remote.getMe()

        assertEquals(ForceUpdateSignal(null, null, null, PopupImage.Preset(PopupPreset.UPDATE), null), hub.current.value)
    }

    @Test
    fun `FR-APP-02 AC6 426이 아니거나 다른 코드면 신호를 내지 않는다`() = runTest {
        server.enqueue(jsonResponse(500, """{"code":"INTERNAL_ERROR","message":"x","details":null,"traceId":null}"""))
        server.enqueue(jsonResponse(426, """{"code":"SOMETHING_ELSE","message":"x","details":null,"traceId":null}"""))
        server.enqueue(jsonResponse(426, "<html>프록시</html>"))

        repeat(3) { remote.getMe() }

        assertNull(hub.current.value)
        assertTrue(server.requestCount == 3)
    }
}
