package com.duriseo.android.data.remote.couple

import com.duriseo.android.data.remote.FakeTokenStorage
import com.duriseo.android.data.remote.RecordingSessionEnd
import com.duriseo.android.data.remote.authedClient
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.errorJson
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.data.remote.jsonResponse
import com.duriseo.android.data.remote.retrofitFor
import com.duriseo.android.data.remote.testTokens
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.MemberColor
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.ErrorDetails
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
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

/** 공간·커플 API (API.md 3.2, 4.4, 4.5) */
class CoupleRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: CoupleRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val client = authedClient(server, FakeTokenStorage(testTokens(access = "my-access")), RecordingSessionEnd())
        val api = retrofitFor(server, client).create(CoupleApi::class.java)
        remote = CoupleRemoteDataSource(api, ApiCaller(createApiJson()))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun <T> DataResult<T>.value(): T = (this as DataResult.Success).value

    private fun <T> DataResult<T>.apiError(): DataError.ApiError =
        (this as DataResult.Failure).error as DataError.ApiError

    private fun bodyOf(request: okhttp3.mockwebserver.RecordedRequest): JsonObject =
        Json.parseToJsonElement(request.body.readUtf8()).jsonObject

    private val inviteJson = """{"code":"K7Q29X","expiresAt":"2026-10-31T03:10:00Z",
        "reissueAvailableAt":"2026-10-31T03:01:00Z"}"""

    private val spaceJson = """{"id":"22222222-2222-2222-2222-222222222222","type":"COUPLE",
        "togetherColor":"pick-purple","startDate":"2025-05-20",
        "members":[{"userId":"u1","nickname":"하나","color":"pick-orange","hasProfileImage":true},
                   {"userId":"u2","nickname":"둘","color":"pick-blue","hasProfileImage":false}]}"""

    @Test
    fun `API POST invite-codes는 본문 없이 보내고 InviteCode 세 필드를 읽는다`() = runTest {
        server.enqueue(jsonResponse(201, inviteJson))

        val invite = remote.issueInviteCode().value()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/invite-codes", request.path)
        assertEquals("Bearer my-access", request.getHeader("Authorization"))
        assertEquals(0L, request.bodySize)
        assertEquals("K7Q29X", invite.code)
        assertEquals(Instant.parse("2026-10-31T03:10:00Z"), invite.expiresAt)
        assertEquals(Instant.parse("2026-10-31T03:01:00Z"), invite.reissueAvailableAt)
    }

    @Test
    fun `API POST invite-codes의 429는 retryAfterSeconds를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                429,
                """{"code":"INVITE_REISSUE_TOO_SOON","message":"m","details":{"retryAfterSeconds":23},"traceId":"t"}""",
            ),
        )

        val error = remote.issueInviteCode().apiError()

        assertEquals(ErrorCode.INVITE_REISSUE_TOO_SOON, error.knownCode)
        assertEquals(ErrorDetails.RetryAfter(23), error.details)
    }

    @Test
    fun `API GET invite-codes current는 InviteCode를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, inviteJson))

        val invite = remote.currentInviteCode().value()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/invite-codes/current", request.path)
        assertEquals("K7Q29X", invite?.code)
    }

    @Test
    fun `API GET invite-codes current의 404는 코드 없음으로 읽는다`() = runTest {
        server.enqueue(jsonResponse(404, errorJson("NOT_FOUND")))

        val result = remote.currentInviteCode()

        assertNull(result.value())
    }

    @Test
    fun `API GET invite-codes current의 다른 오류는 오류로 남긴다`() = runTest {
        server.enqueue(jsonResponse(409, errorJson("ALREADY_COUPLED")))

        assertEquals(ErrorCode.ALREADY_COUPLED, remote.currentInviteCode().apiError().knownCode)
    }

    @Test
    fun `API POST couple join은 code를 보내고 CoupleJoinResult 세 필드를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(200, """{"spaceId":"22222222-2222-2222-2222-222222222222","restored":false,"importPending":true}"""),
        )

        val result = remote.join("K7Q29X").value()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/couple/join", request.path)
        assertEquals("K7Q29X", bodyOf(request)["code"]?.jsonPrimitive?.content)
        assertEquals("22222222-2222-2222-2222-222222222222", result.spaceId)
        assertFalse(result.restored)
        assertTrue(result.importPending)
    }

    @Test
    fun `FR-COUPLE-06 AC1 API POST couple join은 restored true를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, """{"spaceId":"s1","restored":true,"importPending":false}"""))

        val result = remote.join("K7Q29X").value()

        assertTrue(result.restored)
        assertFalse(result.importPending)
    }

    @Test
    fun `API couple join의 오류 코드 일곱 가지를 ApiError로 바꾼다`() = runTest {
        val cases = listOf(
            404 to "INVITE_NOT_FOUND",
            409 to "INVITE_SELF",
            409 to "INVITE_USED",
            409 to "INVITE_UNAVAILABLE",
            409 to "INVITE_EXPIRED",
            409 to "ALREADY_COUPLED",
            403 to "GUEST_NOT_ALLOWED",
        )
        for ((status, code) in cases) {
            server.enqueue(jsonResponse(status, errorJson(code)))

            val error = remote.join("K7Q29X").apiError()

            assertEquals(code, error.code)
            assertEquals(ErrorCode.fromCode(code), error.knownCode)
        }
    }

    @Test
    fun `API POST couple join의 429는 retryAfterSeconds를 읽는다`() = runTest {
        server.enqueue(
            jsonResponse(
                429,
                """{"code":"INVITE_RATE_LIMITED","message":"m","details":{"retryAfterSeconds":1800},"traceId":"t"}""",
            ),
        )

        val error = remote.join("K7Q29X").apiError()

        assertEquals(ErrorCode.INVITE_RATE_LIMITED, error.knownCode)
        assertEquals(ErrorDetails.RetryAfter(1800), error.details)
    }

    @Test
    fun `API POST couple import는 import true와 false를 보내고 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        remote.decideImport(true).value()
        remote.decideImport(false).value()

        val first = server.takeRequest()
        assertEquals("POST", first.method)
        assertEquals("/api/v1/couple/import", first.path)
        assertTrue(bodyOf(first)["import"]!!.jsonPrimitive.boolean)
        assertFalse(bodyOf(server.takeRequest())["import"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `API couple import의 409 NO_IMPORT_PENDING은 ApiError다`() = runTest {
        server.enqueue(jsonResponse(409, errorJson("NO_IMPORT_PENDING")))

        assertEquals(ErrorCode.NO_IMPORT_PENDING, remote.decideImport(true).apiError().knownCode)
    }

    @Test
    fun `API PATCH space는 startDate만 보내고 Space와 members를 읽는다`() = runTest {
        server.enqueue(jsonResponse(200, spaceJson))

        val space = remote.updateStartDate(LocalDate.of(2025, 5, 20)).value()

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/space", request.path)
        val body = bodyOf(request)
        assertEquals(setOf("startDate"), body.keys)
        assertEquals("2025-05-20", body["startDate"]?.jsonPrimitive?.content)
        assertEquals(SpaceType.COUPLE, space.type)
        assertEquals("pick-purple", space.togetherColor)
        assertEquals(LocalDate.of(2025, 5, 20), space.startDate)
        assertEquals(listOf("u1", "u2"), space.members.map { it.userId })
        assertEquals("pick-blue", space.members[1].color)
        assertTrue(space.members[0].hasProfileImage)
    }

    @Test
    fun `API PATCH space colors는 바뀐 필드만 보내고 null을 보내지 않는다`() = runTest {
        server.enqueue(jsonResponse(200, spaceJson))

        remote.updateColors(ColorChange(together = "pick-green")).value()

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/space/colors", request.path)
        val body = bodyOf(request)
        assertEquals(setOf("together"), body.keys)
        assertEquals("pick-green", body["together"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API PATCH space colors는 members와 together를 함께 보낼 수 있다`() = runTest {
        server.enqueue(jsonResponse(200, spaceJson))

        remote.updateColors(
            ColorChange(
                members = listOf(MemberColor("u1", "pick-rose"), MemberColor("u2", "pick-teal")),
                together = "pick-ink",
            ),
        ).value()

        val body = bodyOf(server.takeRequest())
        val members = body["members"]!!.toString()
        assertEquals(setOf("members", "together"), body.keys)
        assertEquals("""[{"userId":"u1","color":"pick-rose"},{"userId":"u2","color":"pick-teal"}]""", members)
        assertEquals("pick-ink", body["together"]?.jsonPrimitive?.content)
    }

    @Test
    fun `API DELETE couple은 204를 성공으로 본다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        remote.unlink().value()

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/couple", request.path)
    }

    @Test
    fun `API DELETE couple의 409 NOT_COUPLE은 ApiError다`() = runTest {
        server.enqueue(jsonResponse(409, errorJson("NOT_COUPLE")))

        assertEquals(ErrorCode.NOT_COUPLE, remote.unlink().apiError().knownCode)
    }
}
