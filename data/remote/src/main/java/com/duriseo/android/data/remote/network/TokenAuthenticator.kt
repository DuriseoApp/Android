package com.duriseo.android.data.remote.network

import com.duriseo.android.data.remote.auth.AuthApi
import com.duriseo.android.data.remote.auth.RefreshRequest
import com.duriseo.android.data.remote.auth.toTokens
import com.duriseo.android.data.remote.call.ApiErrorBody
import com.duriseo.android.domain.auth.AuthTokens
import com.duriseo.android.domain.auth.SessionEndNotifier
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.auth.TokenStorage
import com.duriseo.android.domain.error.ErrorCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 401 응답을 받으면 오류 `code`를 보고 (API.md 1장, ARCHITECTURE.md 4장):
 *
 * - `TOKEN_EXPIRED`만 `POST /auth/refresh`를 **한 번만** 부르고(동시 요청은 잠금에서 기다림) 원래 요청을 새 access로 다시 보낸다.
 * - `SESSION_REPLACED` / `SESSION_INVALID`는 refresh 없이 세션을 끝낸다 ([SessionEndNotifier]).
 * - 그 밖의 401(`SOCIAL_TOKEN_INVALID` 등)은 건드리지 않는다.
 *
 * refresh 요청은 "인증 없음" 클라이언트의 [AuthApi]로 동기 호출한다(이 클라이언트에는 Authenticator가 없다).
 * 네트워크 오류면 [java.io.IOException]을 그대로 던져 원래 호출이 `Offline`이 되게 하고 토큰은 지우지 않는다.
 * 토큰 값은 로그에 남기지 않는다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val authApi: AuthApi,
    private val sessionEnd: SessionEndNotifier,
    private val json: Json,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 같은 요청이 갱신 뒤에도 401이면 포기한다
        if (responseCount(response) >= MAX_ATTEMPTS) return null

        // 토큰 없이 보낸 요청(로그아웃 상태)의 401은 세션 종료로 알리지 않는다
        val requestAccess = response.request.bearerToken() ?: return null
        val code = ErrorCode.fromCode(errorCode(response) ?: return null) ?: return null

        return when (code) {
            ErrorCode.TOKEN_EXPIRED -> refreshAndRetry(response.request, requestAccess)
            ErrorCode.SESSION_REPLACED -> endIfCurrent(requestAccess, SessionEndReason.REPLACED)
            ErrorCode.SESSION_INVALID -> endIfCurrent(requestAccess, SessionEndReason.INVALID)
            else -> null
        }
    }

    private fun refreshAndRetry(request: Request, requestAccess: String): Request? =
        synchronized(lock) { refreshLocked(request, requestAccess) }

    /** [lock]을 잡은 채로 부른다: 한 번에 한 스레드만 refresh 한다. */
    private fun refreshLocked(request: Request, requestAccess: String): Request? {
        val stored = tokenStorage.current() ?: return null
        // 기다리는 사이 다른 스레드가 이미 갱신했다: 저장된 새 access로 다시 보낸다 (refresh를 또 부르지 않는다)
        if (stored.accessToken != requestAccess) return request.withBearer(stored.accessToken)

        return when (val outcome = refresh(stored)) {
            is RefreshOutcome.Refreshed -> {
                // 저장이 끝난 뒤에 다시 보낸다. 기다리던 다른 스레드는 이 값을 읽는다
                runBlocking { tokenStorage.save(outcome.tokens) }
                request.withBearer(outcome.tokens.accessToken)
            }

            is RefreshOutcome.SessionEnded -> {
                sessionEnd.end(outcome.reason)
                null
            }

            RefreshOutcome.Failed -> null
        }
    }

    /** 요청에 실린 access가 지금 저장된 것일 때만 끝낸다. 이미 다른 로그인으로 바뀌었으면 새 세션을 건드리지 않는다. */
    private fun endIfCurrent(requestAccess: String, reason: SessionEndReason): Request? {
        if (tokenStorage.current()?.accessToken == requestAccess) sessionEnd.end(reason)
        return null
    }

    /** @throws java.io.IOException 네트워크 오류 (원래 호출이 Offline이 된다) */
    private fun refresh(stored: AuthTokens): RefreshOutcome {
        val result = try {
            authApi.refresh(RefreshRequest(stored.refreshToken)).execute()
        } catch (e: IllegalArgumentException) {
            // 성공 응답 본문이 약속한 모양이 아님 (SerializationException 포함)
            return RefreshOutcome.Failed
        }

        if (result.isSuccessful) {
            val body = result.body() ?: return RefreshOutcome.Failed
            return RefreshOutcome.Refreshed(body.toTokens())
        }

        val code = result.errorBody()?.string()?.let { parseCode(it) }
        return when (code?.let { ErrorCode.fromCode(it) }) {
            ErrorCode.SESSION_REPLACED -> RefreshOutcome.SessionEnded(SessionEndReason.REPLACED)
            ErrorCode.SESSION_INVALID -> RefreshOutcome.SessionEnded(SessionEndReason.INVALID)
            // 5xx 등: 원래 요청의 401이 그대로 전달된다
            else -> RefreshOutcome.Failed
        }
    }

    /** 응답 본문을 소비하지 않고 오류 `code`만 읽는다 */
    private fun errorCode(response: Response): String? {
        val text = response.peekBody(PEEK_LIMIT_BYTES).string()
        return parseCode(text)
    }

    private fun parseCode(text: String): String? {
        if (text.isBlank()) return null
        return try {
            json.decodeFromString(ApiErrorBody.serializer(), text).code
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private sealed interface RefreshOutcome {
        data class Refreshed(val tokens: AuthTokens) : RefreshOutcome
        data class SessionEnded(val reason: SessionEndReason) : RefreshOutcome
        data object Failed : RefreshOutcome
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
        const val PEEK_LIMIT_BYTES = 64L * 1024
    }
}
