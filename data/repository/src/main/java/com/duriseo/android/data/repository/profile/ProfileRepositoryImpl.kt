package com.duriseo.android.data.repository.profile

import com.duriseo.android.data.remote.me.MeRemoteDataSource
import com.duriseo.android.data.repository.cache.LocalCacheGeneration
import com.duriseo.android.data.repository.me.MeRepositoryImpl
import com.duriseo.android.data.repository.online.runOnlineWrite
import com.duriseo.android.domain.network.NetworkMonitor
import com.duriseo.android.domain.profile.ProfileRepository
import com.duriseo.android.domain.result.DataResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 내 프로필 쓰기 (FR-AUTH-07 AC6, 단위 8). 모두 [runOnlineWrite]로 감싸 오프라인이면 요청하지 않는다 (NFR-OFF-02).
 * 낙관적 갱신은 없다: 서버가 성공한 뒤에만 Room이 바뀌고 화면은 Room Flow로 새 값을 본다.
 *
 * - 이름: 200 응답 `Me`를 [MeRepositoryImpl.saveWrittenMe]로 저장한다 (`MeWriteLock`·캐시 세대 규칙은 `syncTimeZone`과 같다).
 * - 사진: 204에는 본문이 없어서 성공하면 `GET /me`로 새 사진 버전을 읽어 Room에 저장한다. 그 읽기가 실패해도(오프라인 등)
 *   올리기·지우기는 성공이다 (`CoupleRepositoryImpl`의 `thenRefreshMe`와 같다). 413·415는 `ApiError`로 돌려준다.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val remote: MeRemoteDataSource,
    private val meRepository: MeRepositoryImpl,
    private val networkMonitor: NetworkMonitor,
    private val cache: LocalCacheGeneration,
) : ProfileRepository {

    override suspend fun updateNickname(nickname: String): DataResult<Unit> {
        // 요청 전에 캐시 세대를 적어 둔다 (응답이 오기 전에 초기화·로그아웃으로 캐시가 지워졌으면 저장하지 않는다)
        val generation = cache.current()
        val result = networkMonitor.runOnlineWrite { remote.patchNickname(nickname.trim()) }
        return meRepository.saveWrittenMe(result, generation)
    }

    override suspend fun uploadPhoto(bytes: ByteArray, mimeType: String): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.putProfileImage(bytes, mimeType) }.thenRefreshMe()

    override suspend fun deletePhoto(): DataResult<Unit> =
        networkMonitor.runOnlineWrite { remote.deleteProfileImage() }.thenRefreshMe()

    /** 쓰기가 성공했으면 `GET /me`로 Room을 채운다. 이 갱신이 실패해도 쓰기 결과는 바꾸지 않는다 */
    private suspend fun <T> DataResult<T>.thenRefreshMe(): DataResult<T> {
        if (this is DataResult.Success) meRepository.refreshMe()
        return this
    }
}
