package com.duriseo.android.data.repository.auth

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.authTokensJson
import com.duriseo.android.data.repository.errorJson
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.meJson
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.SocialProvider
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 최근 로그인 수단을 기억하고 지우는 때 (FR-AUTH-10 AC1, Q-25) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AuthRepositoryImplTest {
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

    private fun harness(signedIn: Boolean = false): RepoHarness =
        RepoHarness(
            server = server,
            tempDir = tempFolder.root,
            initialTokens = if (signedIn) testTokens() else null,
        ).also { created = it }

    private suspend fun RepoHarness.remembered(): SocialProvider? = authRepository.lastProvider.first()

    @Test
    fun `FR-AUTH-10 AC1 소셜 로그인 성공이면 수단을 기억한다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, authTokensJson("a", "r", isNewUser = false)))
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER")))

        h.authRepository.signIn(SocialProvider.KAKAO, "kakao-token")

        assertEquals(SocialProvider.KAKAO, h.remembered())
    }

    @Test
    fun `FR-AUTH-10 AC1 연동 성공이면 수단을 기억한다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload(fixtureMe(accountType = AccountType.GUEST))
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER")))

        h.authRepository.link(SocialProvider.GOOGLE, "google-id-token")

        assertEquals(SocialProvider.GOOGLE, h.remembered())
    }

    @Test
    fun `FR-AUTH-10 AC1 switch 성공이면 수단을 기억한다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload(fixtureMe(id = "guest-user", accountType = AccountType.GUEST))
        h.lastSignIn.save(SocialProvider.GOOGLE)
        server.enqueue(jsonResponse(200, authTokensJson("member-access", "member-refresh", isNewUser = false)))
        server.enqueue(jsonResponse(200, meJson(id = "member-user", accountType = "MEMBER")))

        h.authRepository.switchAccount(SocialProvider.KAKAO, "kakao-token")

        // 다른 수단으로 성공하면 그 수단으로 바뀐다
        assertEquals(SocialProvider.KAKAO, h.remembered())
    }

    @Test
    fun `FR-AUTH-10 AC1 실패하면 바꾸지 않는다`() = runTestWithTimeout {
        val h = harness()
        h.lastSignIn.save(SocialProvider.GOOGLE)
        server.enqueue(jsonResponse(401, errorJson("SOCIAL_TOKEN_INVALID")))

        h.authRepository.signIn(SocialProvider.KAKAO, "bad-token")

        assertEquals(SocialProvider.GOOGLE, h.remembered())
    }

    @Test
    fun `FR-AUTH-10 AC1 탈퇴하면 지운다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload()
        h.lastSignIn.save(SocialProvider.KAKAO)
        server.enqueue(MockResponse().setResponseCode(204))

        h.authRepository.withdraw()

        assertNull(h.remembered())
        assertNull(h.tokens.current())
    }

    @Test
    fun `FR-AUTH-10 AC1 로그아웃하면 지우지 않는다`() = runTestWithTimeout {
        val h = harness(signedIn = true)
        h.preload()
        h.lastSignIn.save(SocialProvider.KAKAO)
        server.enqueue(MockResponse().setResponseCode(204))

        h.authRepository.logout()

        assertEquals(SocialProvider.KAKAO, h.remembered())
    }
}
