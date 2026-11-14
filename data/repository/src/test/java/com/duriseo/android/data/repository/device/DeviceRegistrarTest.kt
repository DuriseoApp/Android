package com.duriseo.android.data.repository.device

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.errorJson
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.result.DataResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
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
import java.util.concurrent.TimeUnit

/** 기기 등록 `PUT /me/devices` (API.md 3.1, NFR-SYNC-01) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceRegistrarTest {
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

    private suspend fun signedIn(online: Boolean = true): RepoHarness =
        RepoHarness(server, tempFolder.root, online = online, initialTokens = testTokens()).also { created = it }

    private suspend fun signedOut(): RepoHarness =
        RepoHarness(server, tempFolder.root, initialTokens = null).also {
            created = it
            it.tokens.restore() // 토큰 저장소를 읽었더니 비어 있다 (로그아웃 상태)
        }

    @Test
    fun `로그인 상태에서 토큰이 바뀌면 PUT me devices를 부른다`() = runTestWithTimeout {
        val h = signedIn()
        server.enqueue(MockResponse().setResponseCode(204))

        val result = h.deviceRegistrar.registerToken("fcm-token-2")

        assertEquals(DataResult.Success(Unit), result)
        val request = server.takeRequest()
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/me/devices", request.path)
        assertEquals("fcm-token-2", body["fcmToken"]?.jsonPrimitive?.content)
        assertEquals("ANDROID", body["platform"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API PUT me devices 앱 시작 때는 지금 FCM 토큰을 얻어 등록한다`() = runTestWithTimeout {
        val h = signedIn()
        h.pushTokens.token = "fcm-current"
        server.enqueue(MockResponse().setResponseCode(204))

        h.deviceRegistrar.registerCurrentToken()

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("fcm-current", body["fcmToken"]?.jsonPrimitive?.content)
    }

    @Test
    fun `로그아웃 상태면 등록하지 않는다`() = runTestWithTimeout {
        val h = signedOut()
        h.pushTokens.token = "fcm-current"

        assertEquals(DataResult.Success(Unit), h.deviceRegistrar.registerToken("fcm-token"))
        assertEquals(DataResult.Success(Unit), h.deviceRegistrar.registerCurrentToken())

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `API PUT me devices FCM이 꺼져 토큰이 없으면 요청 없이 성공이다`() = runTestWithTimeout {
        val h = signedIn()
        h.pushTokens.token = null

        assertEquals(DataResult.Success(Unit), h.deviceRegistrar.registerCurrentToken())

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `API PUT me devices 오프라인이면 요청 없이 Offline이고 서버 오류는 실패로 돌려준다 다음 시작 때 다시`() = runTestWithTimeout {
        val offline = signedIn(online = false)
        assertEquals(DataResult.Failure(DataError.Offline), offline.deviceRegistrar.registerToken("fcm-token"))
        assertEquals(0, server.requestCount)

        offline.monitor.online = true
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))
        assertTrue(offline.deviceRegistrar.registerToken("fcm-token") is DataResult.Failure)
    }

    @Test
    fun `API PUT me devices 로그인이 끝나면 뒤에서 현재 토큰을 등록한다`() = runTestWithTimeout {
        val h = signedOut()
        h.pushTokens.token = "fcm-after-login"
        server.enqueue(jsonResponse(201, com.duriseo.android.data.repository.authTokensJson("a", "r", isNewUser = true)))
        server.enqueue(jsonResponse(200, com.duriseo.android.data.repository.meJson()))
        server.enqueue(MockResponse().setResponseCode(204))

        h.authRepository.startAsGuest()

        server.takeRequest() // POST /auth/guest
        server.takeRequest() // GET /me
        val device = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/api/v1/me/devices", device?.path)
    }
}
