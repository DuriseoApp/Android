package com.duriseo.android.data.remote.image

import coil3.ImageLoader
import com.duriseo.android.domain.profile.ProfileImageCacheCleaner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Provider

/**
 * Coil의 메모리 캐시와 디스크 캐시를 비운다 (NFR-PRIV-01 취지: 이전 사용자의 사진이 기기에 남지 않게).
 * 디스크 캐시 비우기는 파일 입출력이라 IO 디스패처에서 한다.
 */
class CoilProfileImageCacheCleaner(
    private val loader: Provider<ImageLoader>,
    private val io: CoroutineDispatcher,
) : ProfileImageCacheCleaner {
    override suspend fun clear() {
        val imageLoader = loader.get()
        imageLoader.memoryCache?.clear()
        withContext(io) {
            imageLoader.diskCache?.clear()
        }
    }
}
