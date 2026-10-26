package com.duriseo.android.data.remote.call

import com.duriseo.android.core.common.Logger
import com.duriseo.android.data.remote.BuildConfig
import com.duriseo.android.data.remote.network.ServerNotConfiguredException
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.result.DataResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Retrofit 호출을 [DataResult]로 바꾼다 (API.md 1.1, ARCHITECTURE.md 3장).
 *
 * Retrofit 서비스 메서드는 `suspend fun x(): Response<T>`로 선언한다 (204는 `Response<Unit>`).
 *
 * - 성공(2xx): 봉투 없이 리소스 그대로. 204는 [callUnit].
 * - 오류(4xx, 5xx): `{code, message, details, traceId}` -> [DataError.ApiError]. `message`는 담지 않는다.
 *   JSON이 아닌 오류 본문(프록시 HTML, 빈 본문)은 `INTERNAL_ERROR`로 본다 (unit-1.md 확인 필요 9).
 * - 네트워크 오류(연결 없음, 시간 초과, 그 밖의 IOException): [DataError.Offline] (NFR-OFF-02).
 */
@Singleton
class ApiCaller @Inject constructor(
    private val json: Json,
) {

    /** 본문이 있는 성공 응답(200/201)을 [DataResult]로 바꾼다. */
    suspend fun <T : Any> call(request: suspend () -> Response<T>): DataResult<T> =
        execute(request) { response ->
            val body = response.body()
            if (body != null) DataResult.Success(body) else DataResult.Failure(internalError(traceId = null))
        }

    /** 본문 없는 성공(204)을 [Unit] 성공으로 바꾼다. */
    suspend fun callUnit(request: suspend () -> Response<Unit>): DataResult<Unit> =
        execute(request) { DataResult.Success(Unit) }

    private suspend fun <T, R> execute(
        request: suspend () -> Response<T>,
        onSuccess: (Response<T>) -> DataResult<R>,
    ): DataResult<R> = try {
        val response = request()
        if (response.isSuccessful) {
            onSuccess(response)
        } else {
            DataResult.Failure(parseApiError(response))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: ServerNotConfiguredException) {
        // 서버 주소가 없는 빌드. 화면에는 공통 오류로 보인다.
        DataResult.Failure(internalError(traceId = null))
    } catch (e: IOException) {
        DataResult.Failure(DataError.Offline)
    } catch (e: SerializationException) {
        // 성공 응답 본문이 약속한 모양이 아님
        DataResult.Failure(internalError(traceId = null))
    }

    private fun parseApiError(response: Response<*>): DataError.ApiError {
        val headerTraceId = response.headers()[TRACE_ID_HEADER]
        val raw = try {
            response.errorBody()?.string()
        } catch (e: IOException) {
            null
        }
        val body = raw?.takeIf { it.isNotBlank() }?.let { text ->
            try {
                json.decodeFromString(ApiErrorBody.serializer(), text)
            } catch (e: IllegalArgumentException) {
                // SerializationException 포함: JSON이 아니거나 오류 객체 모양이 아님
                null
            }
        } ?: return internalError(traceId = headerTraceId)

        val traceId = body.traceId ?: headerTraceId
        if (BuildConfig.DEBUG) {
            // message와 traceId는 디버그 로그에만 남긴다 (ARCHITECTURE.md 3장, API.md 1.1)
            Logger.d(TAG, "API 오류 code=${body.code} traceId=$traceId message=${body.message}")
        }
        return DataError.ApiError(
            code = body.code,
            details = parseErrorDetails(body.code, body.details),
            traceId = traceId,
        )
    }

    private fun internalError(traceId: String?) = DataError.ApiError(
        code = ErrorCode.INTERNAL_ERROR.code,
        details = null,
        traceId = traceId,
    )

    private companion object {
        const val TAG = "api"

        /** 응답 헤더에도 같은 값이 들어 있다 (API.md 1.1) */
        const val TRACE_ID_HEADER = "X-Trace-Id"
    }
}
