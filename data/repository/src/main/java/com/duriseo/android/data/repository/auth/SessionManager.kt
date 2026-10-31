package com.duriseo.android.data.repository.auth

import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.data.local.cleanup.LocalDataCleaner
import com.duriseo.android.domain.auth.SessionEndNotifier
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.auth.SocialSessionCleaner
import com.duriseo.android.domain.auth.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 로컬 데이터 정리와 세션 끊김 알림 (API.md 1장, unit-2.md F절).
 *
 * - [end]: 서버가 세션을 끊었다(401 `SESSION_REPLACED`·`SESSION_INVALID`). 토큰·Room·설정·소셜 SDK 상태를 지우고
 *   [events]로 이유를 알린다. 이미 끝났거나 끝나는 중이면 무시한다 (한 번만).
 * - [clearAll]: 로그아웃·탈퇴 204 뒤에 같은 정리를 한다 (이벤트 없음).
 * - [clearUserData]: 앱 시작 때 토큰이 없으면 남은 사용자 데이터를 지운다 (실패는 무시).
 * - [clearUserDataOrFail]: 로그인·switch가 새 토큰을 저장하기 전에 이전 사용자 데이터를 지운다 (실패를 알린다).
 */
@Singleton
class SessionManager @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val localData: LocalDataCleaner,
    private val social: SocialSessionCleaner,
    @ApplicationScope private val scope: CoroutineScope,
) : SessionEndNotifier {

    private val eventChannel = Channel<SessionEndReason>(Channel.BUFFERED)

    /**
     * 정리가 끝난 뒤 이유를 한 번씩 내보낸다. 구독자가 없을 때 나온 것은 버퍼에 남았다가 첫 구독자에게 전달된다
     * (앱을 켜는 중에 세션이 끊겨도 시작 화면 안내가 사라지지 않게). 구독자는 하나(`MainViewModel`)라고 본다.
     */
    val events: Flow<SessionEndReason> = eventChannel.receiveAsFlow()

    private val ending = AtomicBoolean(false)

    override fun end(reason: SessionEndReason) {
        // 토큰이 없으면 이미 로그아웃(또는 끝난) 상태다
        if (tokenStorage.current() == null) return
        if (!ending.compareAndSet(false, true)) return
        scope.launch {
            try {
                clearAll()
                eventChannel.send(reason)
            } finally {
                ending.set(false)
            }
        }
    }

    /** 토큰, Room, 설정, 소셜 SDK 상태를 모두 지운다. 한 단계가 실패해도 나머지는 계속한다. */
    suspend fun clearAll() {
        attempt("토큰") { tokenStorage.clear() }
        clearUserData()
        attempt("소셜 SDK 상태") { social.clear() }
    }

    /**
     * 새 로그인(게스트 시작, 소셜 로그인, switch)이 새 토큰을 저장하기 **전에** 이전 사용자의 Room·설정을 비운다.
     * 실패하면 false다: 이전 사용자 데이터가 남은 채 새 계정으로 들어가면 안 되므로 호출한 쪽이 로그인을 실패로 처리한다.
     */
    suspend fun clearUserDataOrFail(): Boolean = try {
        localData.clearUserData()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w(TAG, "로컬 데이터 정리에 실패했어요: ${e.javaClass.simpleName}")
        false
    }

    /** Room과 설정만 지운다. 토큰과 소셜 SDK 상태는 그대로 둔다. */
    suspend fun clearUserData() {
        attempt("로컬 데이터") { localData.clearUserData() }
    }

    private suspend fun attempt(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "$what 정리에 실패했어요: ${e.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "session"
    }
}
