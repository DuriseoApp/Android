package com.duriseo.android.data.remote.network

import com.duriseo.android.data.remote.appops.parseForceUpdateDetails
import com.duriseo.android.data.remote.call.ApiErrorBody
import com.duriseo.android.domain.appops.ForceUpdateSignal
import com.duriseo.android.domain.appops.ForceUpdateSignals
import com.duriseo.android.domain.error.ErrorCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** 426 신호를 앱 전체에 알리는 곳. `feature:appops`가 [ForceUpdateSignals]로 듣는다 */
@Singleton
class ForceUpdateSignalHub @Inject constructor() : ForceUpdateSignals {
    private val state = MutableStateFlow<ForceUpdateSignal?>(null)
    override val current: StateFlow<ForceUpdateSignal?> = state

    fun publish(signal: ForceUpdateSignal) {
        state.value = signal
    }

    override fun clear() {
        state.value = null
    }
}

/**
 * 어느 요청이든 426 `APP_VERSION_UNSUPPORTED`를 받으면 `details`로 강제 업데이트 신호를 낸다 (API.md 4.9 7단계, R-20, FR-APP-02 AC6).
 * 응답 본문은 [okhttp3.Response.peekBody]로 읽어서 뒤의 `ApiCaller`가 다시 읽을 수 있다. 토큰·로컬 데이터는 건드리지 않는다.
 * `details`가 비었거나 모양이 틀리면 문구 없는 신호(앱 기본 문구)를 낸다. 본문을 읽을 수 없거나 `code`가 다르면 신호를 내지 않는다. 응답은 그대로 돌려준다.
 */
@Singleton
class ForceUpdateInterceptor @Inject constructor(
    private val hub: ForceUpdateSignalHub,
    private val json: Json,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == HTTP_UPGRADE_REQUIRED) publishFrom(response)
        return response
    }

    private fun publishFrom(response: Response) {
        val body = try {
            json.decodeFromString(ApiErrorBody.serializer(), response.peekBody(MAX_PEEK_BYTES).string())
        } catch (e: Exception) {
            // JSON이 아니거나 오류 객체 모양이 아님
            return
        }
        if (body.code != ErrorCode.APP_VERSION_UNSUPPORTED.code) return
        hub.publish(parseForceUpdateDetails(body.details))
    }

    private companion object {
        const val HTTP_UPGRADE_REQUIRED = 426
        const val MAX_PEEK_BYTES = 64L * 1024
    }
}
