package com.duriseo.android.data.remote.me

import com.duriseo.android.data.remote.FakeTokenStorage
import com.duriseo.android.data.remote.RecordingSessionEnd
import com.duriseo.android.data.remote.authedClient
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.errorJson
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.meJson
import com.duriseo.android.data.remote.retrofitFor
import com.duriseo.android.data.remote.testTokens
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
import java.time.LocalDate

/** 내 정보 API (API.md 3.1 `Me`, `MeSpace`, `MePartner`, `PATCH /me`, `DELETE /me`, `PUT /me/devices`) */
class MeRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var api: MeApi
    private lateinit var remote: MeRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val client = authedClient(server, FakeTokenStorage(testTokens(access = "my-access")), RecordingSessionEnd())
        api = retrofitFor(server, client).create(MeApi::class.java)
        remote = MeRemoteDataSource(api, ApiCaller(createApiJson()))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun <T> DataResult<T>.value(): T = (this as DataResult.Success).value

    private val coupleMeJson = """{
        "id":"11111111-1111-1111-1111-111111111111","accountType":"MEMBER","nickname":"하나",
        "timeZone":"Asia/Seoul","providers":["GOOGLE","KAKAO"],"hasProfileImage":true,
        "createdAt":"2026-10-12T03:00:00Z",
        "space":{"id":"22222222-2222-2222-2222-222222222222","type":"COUPLE","myColor":"pick-orange",
          "partner":{"id":"33333333-3333-3333-3333-333333333333","nickname":"둘","color":"pick-blue","hasProfileImage":false},
          "togetherColor":"pick-green","startDate":"2025-05-20","importPending":true}}"""

    @Test
    fun `API GET me는 Me와 MeSpace와 MePartner를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, coupleMeJson))

        val me: Me = remote.getMe().value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/me", request.path)
        assertEquals("Bearer my-access", request.getHeader("Authorization"))
        assertEquals("11111111-1111-1111-1111-111111111111", me.id)
        assertEquals(AccountType.MEMBER, me.accountType)
        assertEquals("하나", me.nickname)
        assertEquals("Asia/Seoul", me.timeZone)
        assertEquals(listOf(SocialProvider.GOOGLE, SocialProvider.KAKAO), me.providers)
        assertTrue(me.hasProfileImage)
        assertEquals(Instant.parse("2026-10-12T03:00:00Z"), me.createdAt)
        assertEquals(SpaceType.COUPLE, me.space.type)
        assertEquals("pick-orange", me.space.myColor)
        assertEquals("pick-green", me.space.togetherColor)
        assertEquals(LocalDate.of(2025, 5, 20), me.space.startDate)
        assertTrue(me.space.importPending)
        val partner = me.space.partner!!
        assertEquals("33333333-3333-3333-3333-333333333333", partner.id)
        assertEquals("둘", partner.nickname)
        assertEquals("pick-blue", partner.color)
        assertFalse(partner.hasProfileImage)
    }

    @Test
    fun `API GET me의 개인 공간은 partner와 togetherColor와 startDate가 null이다`() = runTest {
        server.enqueue(jsonResponse(200, meJson()))

        val me = remote.getMe().value()

        assertEquals(AccountType.GUEST, me.accountType)
        assertEquals(emptyList<SocialProvider>(), me.providers)
        assertEquals(SpaceType.PERSONAL, me.space.type)
        assertNull(me.space.partner)
        assertNull(me.space.togetherColor)
        assertNull(me.space.startDate)
        assertFalse(me.space.importPending)
    }

    @Test
    fun `API GET me의 모르는 accountType은 공통 오류로 본다`() = runTest {
        server.enqueue(jsonResponse(200, meJson(accountType = "ADMIN")))

        val error = (remote.getMe() as DataResult.Failure).error as DataError.ApiError

        assertEquals(ErrorCode.INTERNAL_ERROR, error.knownCode)
    }

    @Test
    fun `API PATCH me는 timeZone만 보내고 nickname 필드를 넣지 않는다`() = runTest {
        server.enqueue(jsonResponse(200, meJson(timeZone = "Asia/Tokyo")))

        val me = remote.patchTimeZone("Asia/Tokyo").value()

        val request = server.takeRequest()
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/me", request.path)
        assertEquals(setOf("timeZone"), body.keys)
        assertEquals("Asia/Tokyo", body["timeZone"]?.jsonPrimitive?.content)
        assertEquals("Asia/Tokyo", me.timeZone)
    }

    @Test
    fun `API PATCH me의 VALIDATION_FAILED는 ApiError로 돌려준다`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """{"code":"VALIDATION_FAILED","message":"m","traceId":"t",
                    "details":{"fields":[{"field":"timeZone","reason":"INVALID"}]}}""",
            ),
        )

        val error = (remote.patchTimeZone("Nowhere/City") as DataResult.Failure).error as DataError.ApiError

        assertEquals(ErrorCode.VALIDATION_FAILED, error.knownCode)
    }

    @Test
    fun `API DELETE me는 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = remote.deleteMe()

        val request = server.takeRequest()
        assertEquals(DataResult.Success(Unit), result)
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/me", request.path)
        assertEquals(0L, request.bodySize)
    }

    @Test
    fun `API DELETE me의 401 SESSION_INVALID는 ApiError로 돌려준다`() = runTest {
        server.enqueue(jsonResponse(401, errorJson("SESSION_INVALID")))

        val error = (remote.deleteMe() as DataResult.Failure).error as DataError.ApiError

        assertEquals(ErrorCode.SESSION_INVALID, error.knownCode)
    }

    @Test
    fun `API PUT me devices는 fcmToken과 platform을 보내고 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = ApiCaller(createApiJson()).callUnit {
            api.putDevice(RegisterDeviceRequest(fcmToken = "fcm-token-1", platform = "ANDROID"))
        }

        val request = server.takeRequest()
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(DataResult.Success(Unit), result)
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/me/devices", request.path)
        assertEquals("fcm-token-1", body["fcmToken"]?.jsonPrimitive?.content)
        assertEquals("ANDROID", body["platform"]?.jsonPrimitive?.content)
        assertTrue(!RegisterDeviceRequest("secret-fcm").toString().contains("secret-fcm"))
    }
}
