package com.duriseo.android.data.repository.me

import com.duriseo.android.data.local.event.EventLocalDataSource
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
    private val writeLock: MeWriteLock,
    private val events: EventLocalDataSource,
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

    override suspend fun resetAfterUnlink() = writeLock.withLock {
        session.clearUserData()
        // 실패(오프라인)해도 지운 채로 끝낸다. 다음 실행 때 새 개인 공간을 받는다.
        // 락 안이라 락을 잡는 refreshMe·saveFresh 대신 락 없는 버전을 쓴다
        if (networkMonitor.isOnlineNow()) {
            val fresh = remote.getMe()
            if (fresh is DataResult.Success) saveFreshLocked(fresh.value)
        }
        changeChannel.trySend(SpaceChange.UNLINKED)
        Unit
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

    /** `GET /me` 응답이 동시에 두 번 저장되면 감지·저장 순서가 섞이므로 한 번에 하나씩 처리한다 ([MeWriteLock]) */
    private suspend fun saveFresh(fresh: Me) = writeLock.withLock { saveFreshLocked(fresh) }

    /**
     * 받은 내 정보를 저장한다. **해제 감지가 저장보다 먼저**다 (API.md 4.5의 6단계, 리드 결정):
     * - 저장 전 Room의 공간이 커플이었고 받은 공간 ID가 다르면 해제다. 새 공간이 개인 공간이든 다른 커플 공간이든 같다.
     * - 받은 공간이 개인 공간이면, 확인한 공간 ID와 다를 때도 해제다 (①).
     *
     * 해제면 로그인 정보를 뺀 로컬 데이터를 지운다. 새 공간이 개인 공간이면 `UNLINKED`를 내보내 앱이 SCR-COUPLE-01로 간다.
     * 새 공간이 다른 커플 공간이면 이동하지 않고, 확인한 공간 ID를 이전 공간으로 남겨 연결 완료 시트가 뜨게 한다
     * (4.4의 8단계 2번). Docs 후속: API 4.5 6단계에 "커플 X → 커플 Y" 경우를 명시해야 한다.
     *
     * [MeWriteLock] 안에서만 부른다.
     */
    private suspend fun saveFreshLocked(fresh: Me) {
        val previous = local.get()?.toDomainOrNull()
        val previousCoupleId = previous?.space?.takeIf { it.type == SpaceType.COUPLE }?.id
        val acknowledgedId = acknowledged.get()
        val isPersonal = fresh.space.type == SpaceType.PERSONAL
        val leftCouple = previousCoupleId != null && previousCoupleId != fresh.space.id
        val unlinked = leftCouple || (isPersonal && acknowledgedId != null && acknowledgedId != fresh.space.id)

        if (unlinked) session.clearUserData()
        // 공간이 바뀌었으면(연결·복구·다른 커플 공간) 이전 공간의 일정 캐시를 비운다 (unit-4.md 확인 필요 12)
        if (!unlinked && previous != null && previous.space.id != fresh.space.id) events.clear()
        local.replace(fresh.toEntity())
        when {
            // 저장 시점 1: 개인 공간을 받을 때마다 (처음 설치·로그인, 초기화 포함. 3번 "초기화 때"도 이것으로 채워진다)
            isPersonal -> acknowledged.set(fresh.space.id)
            // 다른 커플 공간으로 바뀜: 지운 설정에서 이전 공간 ID를 되살려 새 연결의 시트가 뜨게 한다
            leftCouple && previousCoupleId != null -> acknowledged.set(previousCoupleId)
        }
        if (unlinked && isPersonal) changeChannel.trySend(SpaceChange.UNLINKED)
    }
}
