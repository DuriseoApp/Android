package com.duriseo.android.domain.auth

import kotlinx.coroutines.flow.StateFlow

/** 토큰 저장소의 현재 상태 */
sealed interface TokenState {
    /** 아직 저장소에서 읽지 않았다 (앱 시작 직후). 로그인 여부를 모른다. */
    data object Loading : TokenState

    /** 읽었더니 토큰이 없다, 또는 복호화할 수 없어 비웠다 (FR-AUTH-01 AC3) */
    data object Empty : TokenState

    /** 토큰이 있다 = 로그인 상태 (결정 2) */
    data class Present(val tokens: AuthTokens) : TokenState
}

/**
 * 암호화해 저장하는 토큰 저장소 (NFR-SEC-01). 구현은 `data:local`의 `EncryptedTokenStorage`.
 * `data:remote`(Authenticator)와 `data:repository`가 함께 쓰려고 `domain`에 인터페이스를 둔다.
 *
 * 읽기는 메모리 값이라 OkHttp 스레드가 매 요청 복호화하지 않는다.
 */
interface TokenStorage {
    val state: StateFlow<TokenState>

    /** 지금 토큰. 없거나 아직 읽는 중이면 null */
    fun current(): AuthTokens? = (state.value as? TokenState.Present)?.tokens

    /** 저장소에서 한 번 읽어 [state]를 정한다. 두 번째부터는 아무것도 하지 않는다. 복호화할 수 없으면 저장소를 비우고 [TokenState.Empty]. */
    suspend fun restore()

    /** 메모리 값을 바꾼 뒤 암호화해 저장한다. */
    suspend fun save(tokens: AuthTokens)

    /** 메모리와 저장소를 모두 비운다. */
    suspend fun clear()
}
