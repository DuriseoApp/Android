package com.duriseo.android.data.repository.auth

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.authTokensJson
import com.duriseo.android.data.repository.errorJson
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.meJson
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.LoginResult
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 로그인·연동·로그아웃·탈퇴 (FR-AUTH-01~06, NFR-OFF-02, D-86). 메모리 Room은 Robolectric이 필요하다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AuthRepositoryTest {
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

    private fun harness(
        online: Boolean = true,
        signedIn: Boolean = false,
        failClearing: Boolean = false,
    ): RepoHarness =
        RepoHarness(
            server = server,
            tempDir = tempFolder.root,
            online = online,
            initialTokens = if (signedIn) testTokens() else null,
            failClearing = failClearing,
        ).also { created = it }

    private fun DataResult<*>.apiCode(): String? = ((this as? DataResult.Failure)?.error as? DataError.ApiError)?.code

    // --- FR-AUTH-01 게스트 시작 ---

    @Test
    fun `FR-AUTH-01 AC1 게스트 시작 응답의 토큰을 저장하고 내 정보를 Room에 넣는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(201, authTokensJson("new-access", "new-refresh", isNewUser = true)))
        server.enqueue(jsonResponse(200, meJson()))

        val result = h.authRepository.startAsGuest()

        assertEquals(DataResult.Success(LoginResult(isNewUser = true)), result)
        assertEquals("new-access", h.tokens.current()?.accessToken)
        assertEquals("new-refresh", h.tokens.current()?.refreshToken)
        assertEquals(AccountType.GUEST, h.roomMe()?.accountType)
        assertEquals("POST", server.takeRequest().method)
        assertEquals("/api/v1/me", server.takeRequest().path)
        assertEquals(AuthState.SignedIn, h.authRepository.authState.first())
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 게스트 시작 요청을 보내지 않고 안내한다`() = runTestWithTimeout {
        val h = harness(online = false)

        val result = h.authRepository.startAsGuest()

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
        assertNull(h.tokens.current())
    }

    @Test
    fun `FR-AUTH-01 AC1 게스트 시작이 서버 오류면 토큰을 저장하지 않는다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))

        val result = h.authRepository.startAsGuest()

        assertEquals("INTERNAL_ERROR", result.apiCode())
        assertNull(h.tokens.current())
        assertNull(h.roomMe())
    }

    @Test
    fun `FR-AUTH-01 AC2 앱을 시작하면 저장소를 읽어 로그인 상태를 정한다`() = runTestWithTimeout {
        val h = harness(signedIn = false)
        assertEquals(AuthState.Unknown, h.authRepository.authState.first())

        h.authRepository.restoreSession()

        assertEquals(AuthState.SignedOut, h.authRepository.authState.first())
    }

    // --- FR-AUTH-02 소셜 로그인 ---

    @Test
    fun `FR-AUTH-02 AC1 소셜 로그인 응답의 isNewUser를 로그인 결과로 돌려준다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, authTokensJson("a", "r", isNewUser = true)))
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER")))

        val result = h.authRepository.signIn(SocialProvider.GOOGLE, "google-id-token")

        assertEquals(DataResult.Success(LoginResult(isNewUser = true)), result)
    }

    @Test
    fun `FR-AUTH-02 AC2 기존 회원은 로그인 뒤 내 정보를 다시 읽고 캘린더로 간다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, authTokensJson("member-access", "member-refresh", isNewUser = false)))
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER")))

        val result = h.authRepository.signIn(SocialProvider.KAKAO, "kakao-token")

        assertEquals(DataResult.Success(LoginResult(isNewUser = false)), result)
        assertEquals("/api/v1/auth/social", server.takeRequest().path)
        assertEquals("/api/v1/me", server.takeRequest().path)
        assertEquals(AccountType.MEMBER, h.roomMe()?.accountType)
        assertEquals("member-access", h.tokens.current()?.accessToken)
    }

    @Test
    fun `FR-AUTH-02 소셜 로그인의 SOCIAL_TOKEN_INVALID는 토큰을 저장하지 않고 오류로 돌려준다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(401, errorJson("SOCIAL_TOKEN_INVALID")))

        val result = h.authRepository.signIn(SocialProvider.GOOGLE, "bad")

        assertEquals("SOCIAL_TOKEN_INVALID", result.apiCode())
        assertNull(h.tokens.current())
    }

    // --- FR-AUTH-03 연동, 기존 계정 로그인 ---

    @Test
    fun `FR-AUTH-03 AC1 연동은 토큰을 바꾸지 않는다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload(fixtureMe(accountType = AccountType.GUEST))
        val before = h.tokens.current()
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER")))

        val result = h.authRepository.link(SocialProvider.GOOGLE, "google-id-token")

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(before, h.tokens.current())
        // 연동 뒤 내 정보를 다시 읽어 Room이 회원으로 바뀐다
        assertEquals("/api/v1/auth/link", server.takeRequest().path)
        assertEquals("/api/v1/me", server.takeRequest().path)
        assertEquals(AccountType.MEMBER, h.roomMe()?.accountType)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 연동 요청을 보내지 않고 안내한다`() = runTestWithTimeout {
        val h = harness(online = false, signedIn = true)

        val result = h.authRepository.link(SocialProvider.KAKAO, "kakao-token")

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `FR-AUTH-03 AC2 연동이 SOCIAL_ALREADY_USED면 내 정보를 바꾸지 않고 오류로 돌려준다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload()
        server.enqueue(jsonResponse(409, errorJson("SOCIAL_ALREADY_USED")))

        val result = h.authRepository.link(SocialProvider.KAKAO, "kakao-token")

        assertEquals("SOCIAL_ALREADY_USED", result.apiCode())
        assertEquals(1, server.requestCount)
        assertEquals(AccountType.GUEST, h.roomMe()?.accountType)
    }

    @Test
    fun `FR-AUTH-03 연동이 GUEST_ONLY면 내 정보를 다시 읽는다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload(fixtureMe(accountType = AccountType.GUEST))
        server.enqueue(jsonResponse(403, errorJson("GUEST_ONLY")))
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER")))

        val result = h.authRepository.link(SocialProvider.GOOGLE, "t")

        assertEquals("GUEST_ONLY", result.apiCode())
        assertEquals(AccountType.MEMBER, h.roomMe()?.accountType)
    }

    @Test
    fun `FR-AUTH-03 AC3 switch가 성공하면 게스트 데이터를 지우고 새 토큰을 저장한다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload(fixtureMe(id = "guest-user", accountType = AccountType.GUEST))
        server.enqueue(jsonResponse(200, authTokensJson("member-access", "member-refresh", isNewUser = false)))
        server.enqueue(jsonResponse(200, meJson(id = "member-user", accountType = "MEMBER")))

        val result = h.authRepository.switchAccount(SocialProvider.KAKAO, "kakao-token")

        assertEquals(DataResult.Success(LoginResult(isNewUser = false)), result)
        assertEquals("member-access", h.tokens.current()?.accessToken)
        assertEquals("member-refresh", h.tokens.current()?.refreshToken)
        // 게스트의 Room 행은 사라지고 그 계정의 내 정보가 들어 있다
        assertEquals("member-user", h.roomMe()?.id)
        assertEquals(AccountType.MEMBER, h.roomMe()?.accountType)
        // 게스트 설정도 지워졌다
        assertNull(h.settingValue())
        assertEquals("/api/v1/auth/switch", server.takeRequest().path)
        assertEquals("/api/v1/me", server.takeRequest().path)
    }

    @Test
    fun `FR-AUTH-03 AC3 switch가 SOCIAL_NOT_REGISTERED면 게스트 데이터를 그대로 둔다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload(fixtureMe(id = "guest-user"))
        val before = h.tokens.current()
        server.enqueue(jsonResponse(404, errorJson("SOCIAL_NOT_REGISTERED")))

        val result = h.authRepository.switchAccount(SocialProvider.GOOGLE, "t")

        assertEquals("SOCIAL_NOT_REGISTERED", result.apiCode())
        assertEquals(before, h.tokens.current())
        assertEquals("guest-user", h.roomMe()?.id)
        assertEquals("저장된 설정", h.settingValue())
    }

    // --- 이전 사용자 데이터가 새 계정에 남지 않게 (FR-AUTH-01 AC3, NFR-SEC-01) ---

    @Test
    fun `FR-AUTH-01 AC3 로그인하면 이전 사용자의 Room과 설정을 비우고 새 내 정보만 남긴다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(id = "previous-user"))
        server.enqueue(jsonResponse(201, authTokensJson("new-access", "new-refresh", isNewUser = true)))
        server.enqueue(jsonResponse(200, meJson(id = "new-user")))

        val result = h.authRepository.startAsGuest()

        assertTrue(result is DataResult.Success)
        assertEquals("new-user", h.roomMe()?.id)
        assertNull(h.settingValue())
    }

    @Test
    fun `FR-AUTH-01 AC3 이전 사용자 데이터를 비우지 못하면 게스트 시작을 실패로 처리하고 토큰을 저장하지 않는다`() = runTestWithTimeout {
        val h = harness(failClearing = true)
        server.enqueue(jsonResponse(201, authTokensJson("new-access", "new-refresh", isNewUser = true)))

        val result = h.authRepository.startAsGuest()

        assertEquals("INTERNAL_ERROR", result.apiCode())
        assertNull(h.tokens.current())
        // 내 정보(GET /me)도 받지 않는다
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `FR-AUTH-02 이전 사용자 데이터를 비우지 못하면 소셜 로그인을 실패로 처리하고 토큰을 저장하지 않는다`() = runTestWithTimeout {
        val h = harness(failClearing = true)
        server.enqueue(jsonResponse(200, authTokensJson("member-access", "member-refresh")))

        val result = h.authRepository.signIn(SocialProvider.GOOGLE, "google-id-token")

        assertEquals("INTERNAL_ERROR", result.apiCode())
        assertNull(h.tokens.current())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `FR-AUTH-03 AC3 게스트 데이터를 비우지 못하면 switch를 실패로 처리하고 새 토큰을 저장하지 않는다`() = runTestWithTimeout {
        val h = harness(signedIn = true, failClearing = true)
        val before = h.tokens.current()
        server.enqueue(jsonResponse(200, authTokensJson("member-access", "member-refresh")))

        val result = h.authRepository.switchAccount(SocialProvider.KAKAO, "kakao-token")

        assertEquals("INTERNAL_ERROR", result.apiCode())
        assertEquals(before, h.tokens.current())
    }

    // --- FR-AUTH-05 로그아웃, FR-AUTH-06 탈퇴 ---

    @Test
    fun `FR-AUTH-05 AC1 로그아웃 204 뒤 토큰과 Room을 지운다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(MockResponse().setResponseCode(204))

        val result = h.authRepository.logout()

        assertEquals(DataResult.Success(Unit), result)
        assertNull(h.tokens.current())
        assertNull(h.roomMe())
        assertNull(h.settingValue())
        assertEquals(1, h.social.clearCount.get())
        assertEquals("/api/v1/auth/logout", server.takeRequest().path)
        assertEquals(AuthState.SignedOut, h.authRepository.authState.first())
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 로그아웃 요청을 보내지 않고 안내한다`() = runTestWithTimeout {
        val h = harness(online = false, signedIn = true)
        h.preload()

        val result = h.authRepository.logout()

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
        // 로그인 상태 그대로
        assertTrue(h.tokens.current() != null)
        assertTrue(h.roomMe() != null)
        assertEquals(0, h.social.clearCount.get())
    }

    @Test
    fun `FR-AUTH-05 AC4 로그아웃이 서버 오류면 로컬을 지우지 않고 로그인 상태를 둔다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload()
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))

        val result = h.authRepository.logout()

        assertEquals("INTERNAL_ERROR", result.apiCode())
        assertTrue(h.tokens.current() != null)
        assertTrue(h.roomMe() != null)
        assertEquals("저장된 설정", h.settingValue())
        assertEquals(0, h.social.clearCount.get())
    }

    @Test
    fun `FR-AUTH-02 AC3 로그아웃 응답이 SESSION_REPLACED면 세션을 끝내고 로컬을 지운다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload()
        server.enqueue(jsonResponse(401, errorJson("SESSION_REPLACED")))
        val event = async(start = CoroutineStart.UNDISPATCHED) { h.session.events.first() }

        val result = h.authRepository.logout()

        assertEquals("SESSION_REPLACED", result.apiCode())
        assertEquals(SessionEndReason.REPLACED, withTimeoutOrNull(5_000) { event.await() })
        assertNull(h.tokens.current())
        assertNull(h.roomMe())
    }

    @Test
    fun `FR-AUTH-06 AC1 탈퇴 204 뒤 토큰과 Room과 설정을 지운다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload()
        server.enqueue(MockResponse().setResponseCode(204))

        val result = h.authRepository.withdraw()

        assertEquals(DataResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/me", request.path)
        assertNull(h.tokens.current())
        assertNull(h.roomMe())
        assertNull(h.settingValue())
        assertEquals(1, h.social.clearCount.get())
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 탈퇴 요청을 보내지 않고 안내한다`() = runTestWithTimeout {
        val h = harness(online = false, signedIn = true)

        val result = h.authRepository.withdraw()

        assertEquals(DataResult.Failure(DataError.Offline), result)
        assertEquals(0, server.requestCount)
        assertTrue(h.tokens.current() != null)
    }

    @Test
    fun `FR-AUTH-06 AC5 탈퇴가 서버 오류면 로컬을 지우지 않는다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload()
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))

        val result = h.authRepository.withdraw()

        assertEquals(ErrorCode.INTERNAL_ERROR.code, result.apiCode())
        assertTrue(h.tokens.current() != null)
        assertTrue(h.roomMe() != null)
    }
}
