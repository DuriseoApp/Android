package com.duriseo.android.feature.memo.component

import com.duriseo.android.core.common.Logger
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.result.DataResult
import kotlin.coroutines.cancellation.CancellationException

/** 예외를 [DataResult.Failure]로 바꿀 때 쓰는 오류 코드. 서버 코드가 아니라 기기에서 만든 값이고 화면은 공통 오류로 보인다 */
private const val CLIENT_ERROR_CODE = "CLIENT_ERROR"

/**
 * 쓰기 요청 하나를 부르되, 저장소가 예외를 던져도 [DataResult.Failure](공통 오류)로 돌려준다.
 * 호출한 쪽이 결과를 받은 뒤 로딩 덮개를 걷고 안내할 수 있게 하려는 것이다 (D-115). 취소는 그대로 던진다.
 * 로그에는 예외 클래스 이름만 남긴다 (개인 정보·토큰 금지).
 */
internal suspend fun <T> guardedWrite(request: suspend () -> DataResult<T>): DataResult<T> =
    try {
        request()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w("memo", "메모 쓰기 중 예외가 났어요: ${e.javaClass.simpleName}")
        DataResult.Failure(DataError.ApiError(code = CLIENT_ERROR_CODE, details = null, traceId = null))
    }
