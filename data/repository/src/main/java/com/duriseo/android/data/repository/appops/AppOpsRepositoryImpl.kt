package com.duriseo.android.data.repository.appops

import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.core.common.coroutines.IoDispatcher
import com.duriseo.android.data.remote.appops.AppOpsJsonParser
import com.duriseo.android.data.remote.appops.AppOpsRemoteDataSource
import com.duriseo.android.data.remote.appops.RemoteConfigGateway
import com.duriseo.android.domain.appops.AppOpsData
import com.duriseo.android.domain.appops.AppOpsRepository
import com.duriseo.android.domain.appops.AppOpsSnapshot
import com.duriseo.android.domain.appops.AppPopupCoordinator
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 공지·업데이트 팝업의 원천 값 읽기 (FR-APP-01 AC6, D-176~D-178, API.md 4.9 2단계, unit-9 결정 1·5).
 *
 * - [check]: Remote Config 가져오기와 서버 두 조회를 **동시에** 시작한다. 서버는 3초까지, Remote Config도 3초까지만 기다린다.
 *   서버가 늦거나 실패하면 `server = null`(Remote Config 값만으로 판단), Remote Config가 늦거나 실패하면 마지막으로 활성화한 값을 쓴다.
 *   Remote Config 가져오기는 3초 뒤에도 앱 범위에서 계속하고, 늦게 끝나면 [changes]로 알린다.
 * - Firebase가 없는 빌드는 [RemoteConfigGateway]가 빈 값을 줘서 서버 값만으로 동작한다.
 */
@Singleton
class AppOpsRepositoryImpl @Inject constructor(
    private val remote: AppOpsRemoteDataSource,
    private val gateway: RemoteConfigGateway,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AppOpsRepository {

    /** [check]에서 3초 안에 못 받은 Remote Config 값이 나중에 도착했다는 신호 */
    private val lateRemote = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun check(): AppOpsSnapshot = coroutineScope {
        // Remote Config는 3초가 지나도 멈추지 않게 앱 범위에서 돌린다 (Firebase 기본 시간 초과 60초를 기다리지 않는다)
        val fetch = scope.async { fetchRemoteConfig() }
        val server = async { withTimeoutOrNull(AppPopupCoordinator.SERVER_WAIT_MILLIS) { fetchServer() } }

        val fetched = withTimeoutOrNull(AppPopupCoordinator.SERVER_WAIT_MILLIS) { fetch.await() }
        if (fetched == null) {
            scope.launch { if (fetch.await()) lateRemote.tryEmit(Unit) }
        }

        val serverData = server.await()
        AppOpsSnapshot(remote = readRemoteConfig(), server = serverData)
    }

    /**
     * Remote Config가 바뀐 때의 값. 서버 값은 담지 않는다(`server = null`): 마지막으로 받은 서버 값과의 합침은 [AppPopupCoordinator]가 하고,
     * 426 팝업을 닫는 일은 새로 받은 서버 목록으로 하는 [check]에서만 한다 (unit-9 결정 2).
     */
    override val changes: Flow<AppOpsSnapshot> =
        merge(gateway.updates(), lateRemote).map { AppOpsSnapshot(remote = readRemoteConfig(), server = null) }

    private suspend fun fetchRemoteConfig(): Boolean = try {
        gateway.fetchAndActivate()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    /** 공지와 규칙을 둘 다 받아야 서버 값으로 쓴다. 하나라도 실패하면 null (오류·오프라인이면 Remote Config 값만) */
    private suspend fun fetchServer(): AppOpsData? = coroutineScope {
        val notices = async { remote.getNotices() }
        val rules = async { remote.getRules() }
        val n = notices.await() as? DataResult.Success
        val r = rules.await() as? DataResult.Success
        if (n == null || r == null) null else AppOpsData(notices = n.value, rules = r.value)
    }

    /** 디스크에 남은 값을 읽고 JSON을 해석하는 일은 IO 스레드에서 한다 (메인 스레드를 막지 않는다) */
    private suspend fun readRemoteConfig(): AppOpsData = withContext(io) {
        val values = gateway.currentValues()
        AppOpsJsonParser.parse(values.notices, values.rules)
    }
}
