package com.duriseo.android.data.repository.couple

import com.duriseo.android.data.remote.couple.CoupleRemoteDataSource
import com.duriseo.android.data.repository.auth.SessionManager
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.result.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 커플 연결·해제와 공간 설정. 쓰기는 [runOnlineWrite]로 감싸 오프라인이면 요청하지 않고,
 * 성공하면 `GET /me`로 Room을 새로 채운다 (낙관적 갱신 없음, ARCHITECTURE.md 2장).
 */
@Singleton
class CoupleRepositoryImpl @Inject constructor(
    private val remote: CoupleRemoteDataSource,
    private val meRepository: MeRepository,
    private val session: SessionManager,
    private val networkMonitor: NetworkMonitor,
) : CoupleRepository {

    override suspend fun currentInviteCode(): DataResult<InviteCode?> {
        // 읽기라서 쓰기 규칙(runOnlineWrite)은 아니지만, 오프라인이면 요청 없이 같은 오류로 돌려준다
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Offline)
        return remote.currentInviteCode()
    }

    override suspend fun issueInviteCode(): DataResult<InviteCode> =
        networkMonitor.runOnlineWrite { remote.issueInviteCode() }

    override suspend fun join(code: String): DataResult<CoupleJoinResult> =
        networkMonitor.runOnlineWrite { remote.join(code) }.thenRefreshMe()

    override suspend fun decideImport(import: Boolean): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.decideImport(import) }.thenRefreshMe()

    override suspend fun updateStartDate(date: LocalDate): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.updateStartDate(date).map { } }.thenRefreshMe()

    override suspend fun updateColors(change: ColorChange): DataResult<Unit> {
        if (change.isEmpty) return DataResult.Success(Unit)
        return networkMonitor.runOnlineWrite { remote.updateColors(change).map { } }.thenRefreshMe()
    }

    override suspend fun unlink(): DataResult<Unit> {
        val result = networkMonitor.runOnlineWrite { remote.unlink() }
        // 409 NOT_COUPLE는 이미 해제된 것이다 (API.md 4.5의 1단계, FR-COUPLE-05.AC3)
        val alreadyUnlinked = result is DataResult.Failure &&
            (result.error as? DataError.ApiError)?.knownCode == ErrorCode.NOT_COUPLE
        if (result !is DataResult.Success && !alreadyUnlinked) return result

        // 앱 초기화 (D-37): 토큰은 두고 Room·설정만 지운 뒤 새 개인 공간을 받아 온다
        session.clearUserData()
        meRepository.refreshMe()
        return DataResult.Success(Unit)
    }

    /** 쓰기가 성공했으면 `GET /me`로 Room을 채운다. 이 갱신이 실패해도 쓰기 결과는 바꾸지 않는다 */
    private suspend fun <T> DataResult<T>.thenRefreshMe(): DataResult<T> {
        if (this is DataResult.Success) meRepository.refreshMe()
        return this
    }
}
