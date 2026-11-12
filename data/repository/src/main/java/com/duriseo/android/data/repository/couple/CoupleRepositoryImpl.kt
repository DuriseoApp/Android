package com.duriseo.android.data.repository.couple

import com.duriseo.android.data.local.anniversary.AnniversaryLocalDataSource
import com.duriseo.android.data.local.event.EventLocalDataSource
import com.duriseo.android.data.local.me.MeLocalDataSource
import com.duriseo.android.data.local.memo.MemoLocalDataSource
import com.duriseo.android.data.remote.couple.CoupleRemoteDataSource
import com.duriseo.android.data.repository.cache.LocalCacheGeneration
import com.duriseo.android.data.repository.me.MeWriteLock
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.couple.Space
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 커플 연결·해제와 공간 설정. 쓰기는 [runOnlineWrite]로 감싸 오프라인이면 요청하지 않고,
 * 성공하면 Room을 새로 채운다 (낙관적 갱신 없음, ARCHITECTURE.md 2장). `PATCH /space`·`PATCH /space/colors`는
 * 응답 `Space`로 Room을 바로 갱신하고, 그 뒤 `GET /me`가 실패해도 화면은 새 값이다.
 */
@Singleton
class CoupleRepositoryImpl @Inject constructor(
    private val remote: CoupleRemoteDataSource,
    private val meRepository: MeRepository,
    private val meLocal: MeLocalDataSource,
    private val networkMonitor: NetworkMonitor,
    private val writeLock: MeWriteLock,
    private val events: EventLocalDataSource,
    private val anniversaries: AnniversaryLocalDataSource,
    private val memos: MemoLocalDataSource,
    private val cache: LocalCacheGeneration,
) : CoupleRepository {

    override suspend fun currentInviteCode(): DataResult<InviteCode?> {
        // 읽기라서 쓰기 규칙(runOnlineWrite)은 아니지만, 오프라인이면 요청 없이 같은 오류로 돌려준다
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return remote.currentInviteCode().thenRereadMeIfStale()
    }

    override suspend fun issueInviteCode(): DataResult<InviteCode> =
        networkMonitor.runOnlineWrite { remote.issueInviteCode() }.thenRereadMeIfStale()

    override suspend fun join(code: String): DataResult<CoupleJoinResult> =
        networkMonitor.runOnlineWrite { remote.join(code) }.thenRefreshMe().thenRereadMeIfStale()

    override suspend fun decideImport(import: Boolean): DataResult<Unit> =
        // 가져오기의 409는 `NO_IMPORT_PENDING`뿐이다 (API.md R-18, 3.2). `NOT_COUPLE`은 오지 않아 초기화하지 않는다
        networkMonitor.runOnlineWrite { remote.decideImport(import) }
            // 개인 공간의 일정·기념일이 커플 공간으로 옮겨지거나 지워졌다: 캐시를 비워 다시 받게 한다 (unit-4.md 확인 필요 12, 단위 5)
            .also {
                if (it is DataResult.Success) {
                    cache.clearing {
                        events.clear()
                        anniversaries.clear()
                        memos.clear()
                    }
                }
            }
            .thenRefreshMe()

    override suspend fun updateStartDate(date: LocalDate): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.updateStartDate(date) }.thenApplySpace().thenRefreshMe().thenResetIfNotCouple()

    override suspend fun updateColors(change: ColorChange): DataResult<Unit> {
        if (change.isEmpty) return DataResult.Success(Unit)
        return networkMonitor.runOnlineWrite { remote.updateColors(change) }
            .thenApplySpace()
            .thenRefreshMe()
            .thenResetIfNotCouple()
    }

    override suspend fun unlink(): DataResult<Unit> {
        val result = networkMonitor.runOnlineWrite { remote.unlink() }
        // 409 NOT_COUPLE는 이미 해제된 것이다 (API.md 4.5의 1단계, FR-COUPLE-05.AC3)
        val alreadyUnlinked = result is DataResult.Failure && result.error.hasCode(ErrorCode.NOT_COUPLE)
        if (result !is DataResult.Success && !alreadyUnlinked) return result

        // 앱 초기화 (D-37, D-103): 토큰은 두고 Room·설정을 지운 뒤 새 개인 공간을 받아 온다
        meRepository.resetAfterUnlink()
        return DataResult.Success(Unit)
    }

    /**
     * 응답 `Space`로 Room의 내 정보를 바로 갱신한다. 다른 공간의 응답이면 건드리지 않는다 (`GET /me`가 채운다).
     * 읽고 쓰는 사이에 `GET /me` 저장이나 앱 초기화가 끼어들지 못하게 [MeWriteLock] 아래에서 한다
     * (이 안에서 `refreshMe`를 부르지 않는다. 락은 재진입이 안 된다).
     */
    private suspend fun DataResult<Space>.thenApplySpace(): DataResult<Unit> {
        val failure = this as? DataResult.Failure
        if (failure != null) return failure
        val space = (this as DataResult.Success).value
        writeLock.withLock { applySpaceLocked(space) }
        return DataResult.Success(Unit)
    }

    private suspend fun applySpaceLocked(space: Space) {
        val entity = meLocal.get()
        if (entity != null && entity.spaceId == space.id) {
            val partnerId = entity.partnerId
            meLocal.replace(
                entity.copy(
                    myColor = space.members.firstOrNull { it.userId == entity.id }?.color ?: entity.myColor,
                    partnerColor = if (partnerId == null) null else {
                        space.members.firstOrNull { it.userId == partnerId }?.color ?: entity.partnerColor
                    },
                    togetherColor = space.togetherColor,
                    startDate = space.startDate?.toString(),
                    linkedAt = space.linkedAt?.toEpochMilli(),
                ),
            )
        }
    }

    /** 쓰기가 성공했으면 `GET /me`로 Room을 채운다. 이 갱신이 실패해도 쓰기 결과는 바꾸지 않는다 */
    private suspend fun <T> DataResult<T>.thenRefreshMe(): DataResult<T> {
        if (this is DataResult.Success) meRepository.refreshMe()
        return this
    }

    /**
     * 409 `NOT_COUPLE`은 상대가 먼저 해제한 것이다. 해제로 보고 앱을 초기화한다 (API.md 4.5의 1단계, D-103).
     * 결과는 실패 그대로 돌려준다: 이동은 `MeRepository.spaceChanges`를 받는 쪽이 하므로 화면은 오류를 보여 주지 않고
     * 아무것도 하지 않는다.
     */
    private suspend fun <T> DataResult<T>.thenResetIfNotCouple(): DataResult<T> {
        val failure = this as? DataResult.Failure
        if (failure != null && failure.error.hasCode(ErrorCode.NOT_COUPLE)) meRepository.resetAfterUnlink()
        return this
    }

    /**
     * 409 `ALREADY_COUPLED`·403 `GUEST_NOT_ALLOWED`는 기기의 내 정보가 낡은 경우다. 화면이 공통 오류를 보여 주고,
     * 여기서 `GET /me`를 다시 읽는다 (공간이 바뀌었으면 시트나 초기화로 이어진다, API.md 4.4의 12단계, D-95).
     */
    private suspend fun <T> DataResult<T>.thenRereadMeIfStale(): DataResult<T> {
        val error = (this as? DataResult.Failure)?.error
        if (error != null && (error.hasCode(ErrorCode.ALREADY_COUPLED) || error.hasCode(ErrorCode.GUEST_NOT_ALLOWED))) {
            meRepository.refreshMe()
        }
        return this
    }
}
