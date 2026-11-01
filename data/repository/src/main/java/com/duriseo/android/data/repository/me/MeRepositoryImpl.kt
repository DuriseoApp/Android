package com.duriseo.android.data.repository.me

import com.duriseo.android.data.local.me.AcknowledgedSpaceLocalDataSource
import com.duriseo.android.data.local.me.MeLocalDataSource
import com.duriseo.android.data.remote.me.MeRemoteDataSource
import com.duriseo.android.data.repository.auth.SessionManager
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceChange
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 내 정보: 화면은 Room만 구독하고, refresh가 `GET /me`를 Room에 넣는다 (ARCHITECTURE.md 2장).
 * 공간이 바뀐 것을 아는 규칙(해제 감지, 확인한 공간 ID)이 여기에 있다 (API.md 4.4의 8단계, 4.5의 6단계).
 */
@Singleton
class MeRepositoryImpl @Inject constructor(
    private val remote: MeRemoteDataSource,
    private val local: MeLocalDataSource,
    private val networkMonitor: NetworkMonitor,
    private val acknowledged: AcknowledgedSpaceLocalDataSource,
    private val session: SessionManager,
) : MeRepository {

    private val changeChannel = Channel<SpaceChange>(Channel.BUFFERED)

    /** 구독자가 없을 때 난 것은 버퍼에 남았다가 첫 구독자에게 간다 (`SessionManager.events`와 같다) */
    override val spaceChanges: Flow<SpaceChange> = changeChannel.receiveAsFlow()

    override fun observeMe(): Flow<Me?> = local.observe().map { it?.toDomainOrNull() }

    override suspend fun refreshMe(): DataResult<Unit> {
        // 오프라인이면 조용히 실패한다. 화면은 마지막으로 저장한 내 정보를 그대로 보여 준다 (NFR-OFF-01)
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return save(remote.getMe())
    }

    override fun observeLinkedSheetNeeded(): Flow<Boolean> =
        combine(observeMe(), acknowledged.observe()) { me, acknowledgedId ->
            val space = me?.space
            space != null && space.type == SpaceType.COUPLE &&
                (space.importPending || (acknowledgedId != null && acknowledgedId != space.id))
        }.distinctUntilChanged()

    override suspend fun acknowledgeSpace() {
        val spaceId = local.get()?.spaceId ?: return
        acknowledged.set(spaceId)
    }

    override suspend fun resetAfterUnlink() {
        session.clearUserData()
        refreshMe() // 실패(오프라인)해도 지운 채로 끝낸다. 다음 실행 때 새 개인 공간을 받는다
        changeChannel.trySend(SpaceChange.UNLINKED)
    }

    override suspend fun syncTimeZone(zoneId: String): DataResult<Unit> {
        // 저장된 내 정보가 없으면 비교할 수 없다 (로그인 직후에는 refreshMe가 먼저 채운다)
        val cached = local.get() ?: return DataResult.Success(Unit)
        if (cached.timeZone == zoneId) return DataResult.Success(Unit)
        return save(networkMonitor.runOnlineWrite { remote.patchTimeZone(zoneId) })
    }

    private suspend fun save(result: DataResult<Me>): DataResult<Unit> = when (result) {
        is DataResult.Success -> {
            saveFresh(result.value)
            DataResult.Success(Unit)
        }

        is DataResult.Failure -> result
    }

    /**
     * 받은 내 정보를 저장한다. **해제 감지가 저장보다 먼저**다 (API.md 4.5의 6단계):
     * 받은 공간이 개인 공간인데 ① 확인한 공간 ID와 다르거나 ② Room의 공간이 커플이었으면 해제다.
     * ②가 필요한 이유: 연결 완료 시트를 닫기 전에 상대가 해제하면 같은 개인 공간이 되살아나 ID가 같아진다.
     */
    private suspend fun saveFresh(fresh: Me) {
        val previous = local.get()?.toDomainOrNull()
        val acknowledgedId = acknowledged.get()
        val isPersonal = fresh.space.type == SpaceType.PERSONAL
        val unlinked = isPersonal &&
            (previous?.space?.type == SpaceType.COUPLE || (acknowledgedId != null && acknowledgedId != fresh.space.id))

        if (unlinked) session.clearUserData()
        local.replace(fresh.toEntity())
        // 저장 시점 1: 개인 공간을 받을 때마다 (처음 설치·로그인, 초기화 포함. 3번 "초기화 때"도 이것으로 채워진다)
        if (isPersonal) acknowledged.set(fresh.space.id)
        if (unlinked) changeChannel.trySend(SpaceChange.UNLINKED)
    }
}
