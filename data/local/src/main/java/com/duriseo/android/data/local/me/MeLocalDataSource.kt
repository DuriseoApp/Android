package com.duriseo.android.data.local.me

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 내 정보 캐시(Room)를 `data:repository`에 보여 주는 얇은 창구. Room 타입은 이 모듈 밖으로 나가지 않는다. */
@Singleton
class MeLocalDataSource @Inject constructor(
    private val dao: MeDao,
) {
    fun observe(): Flow<MeEntity?> = dao.observe()

    suspend fun get(): MeEntity? = dao.get()

    suspend fun replace(entity: MeEntity) = dao.replace(entity)
}
