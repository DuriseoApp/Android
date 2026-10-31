package com.duriseo.android.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AuthTokensTest {
    private val tokens = AuthTokens(
        accessToken = "access-secret-value",
        accessTokenExpiresAt = Instant.parse("2026-10-30T01:00:00Z"),
        refreshToken = "refresh-secret-value",
        refreshTokenExpiresAt = Instant.parse("2026-11-29T00:00:00Z"),
    )

    @Test
    fun `NFR-SEC-01 AuthTokens toString은 토큰 값을 찍지 않는다`() {
        val text = tokens.toString()

        assertFalse(text.contains("access-secret-value"))
        assertFalse(text.contains("refresh-secret-value"))
    }

    @Test
    fun `NFR-SEC-01 토큰이 든 TokenState toString도 토큰 값을 찍지 않는다`() {
        val text = TokenState.Present(tokens).toString()

        assertFalse(text.contains("access-secret-value"))
        assertFalse(text.contains("refresh-secret-value"))
    }

    @Test
    fun `저장소 상태에서 토큰이 있을 때만 current가 값을 돌려준다`() {
        class Fixed(override val state: kotlinx.coroutines.flow.StateFlow<TokenState>) : TokenStorage {
            override suspend fun restore() = Unit
            override suspend fun save(tokens: AuthTokens) = Unit
            override suspend fun clear() = Unit
        }

        val loading = Fixed(kotlinx.coroutines.flow.MutableStateFlow(TokenState.Loading))
        val empty = Fixed(kotlinx.coroutines.flow.MutableStateFlow(TokenState.Empty))
        val present = Fixed(kotlinx.coroutines.flow.MutableStateFlow(TokenState.Present(tokens)))

        assertNull(loading.current())
        assertNull(empty.current())
        assertEquals(tokens, present.current())
        assertTrue(present.current() != null)
    }
}
