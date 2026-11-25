package com.duriseo.android.data.remote.appops

import com.duriseo.android.core.common.coroutines.IoDispatcher
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.domain.appops.AppNotice
import com.duriseo.android.domain.appops.AppUpdateRule
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** 서버의 공지·업데이트 규칙 공개 조회 (API.md 3.7). 응답은 [AppOpsJsonParser]로 읽는다 (Remote Config와 같은 코드) */
@Singleton
class AppOpsRemoteDataSource @Inject constructor(
    private val api: AppOpsApi,
    private val apiCaller: ApiCaller,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /** `GET /app-notices`. 읽을 수 없는 항목은 건너뛴다 */
    suspend fun getNotices(): DataResult<List<AppNotice>> =
        read({ api.getAppNotices() }, AppOpsJsonParser::parseNotices)

    /** `GET /app-update-rules`. 읽을 수 없는 항목은 건너뛴다 */
    suspend fun getRules(): DataResult<List<AppUpdateRule>> =
        read({ api.getAppUpdateRules() }, AppOpsJsonParser::parseRules)

    private suspend fun <T> read(
        request: suspend () -> Response<ResponseBody>,
        parse: (String?) -> List<T>,
    ): DataResult<List<T>> = when (val result = apiCaller.call(request)) {
        is DataResult.Failure -> result
        // 본문 읽기는 막히는 일이라 IO 디스패처에서 한다
        is DataResult.Success -> withContext(io) { DataResult.Success(parse(result.value.readText())) }
    }

    private fun ResponseBody.readText(): String = try {
        use { string() }
    } catch (e: IOException) {
        ""
    }
}
