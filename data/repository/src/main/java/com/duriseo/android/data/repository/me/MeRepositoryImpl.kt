package com.duriseo.android.data.repository.me

import com.duriseo.android.data.local.me.MeLocalDataSource
import com.duriseo.android.data.remote.me.MeRemoteDataSource
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 내 정보: 화면은 Room만 구독하고, refresh가 `GET /me`를 Room에 넣는다 (ARCHITECTURE.md 2장). */
@Singleton
class MeRepositoryImpl @Inject constructor(
    private val remote: MeRemoteDataSource,
    private val local: MeLocalDataSource,
    private val networkMonitor: NetworkMonitor,
) : MeRepository {

    override fun observeMe(): Flow<Me?> = local.observe().map { it?.toDomainOrNull() }

    override suspend fun refreshMe(): DataResult<Unit> {
        // 오프라인이면 조용히 실패한다. 화면은 마지막으로 저장한 내 정보를 그대로 보여 준다 (NFR-OFF-01)
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return saveToRoom(remote.getMe())
    }

    override suspend fun syncTimeZone(zoneId: String): DataResult<Unit> {
        // 저장된 내 정보가 없으면 비교할 수 없다 (로그인 직후에는 refreshMe가 먼저 채운다)
        val cached = local.get() ?: return DataResult.Success(Unit)
        if (cached.timeZone == zoneId) return DataResult.Success(Unit)
        return saveToRoom(networkMonitor.runOnlineWrite { remote.patchTimeZone(zoneId) })
    }

    private suspend fun saveToRoom(result: DataResult<Me>): DataResult<Unit> = when (result) {
        is DataResult.Success -> {
            local.replace(result.value.toEntity())
            DataResult.Success(Unit)
        }

        is DataResult.Failure -> result
    }
}
