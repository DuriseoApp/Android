package com.duriseo.android.data.local.event

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** 일정 캐시(Room)를 `data:repository`에 보여 주는 얇은 창구. Room 타입은 이 모듈 밖으로 나가지 않는다 */
@Singleton
class EventLocalDataSource @Inject constructor(
    private val dao: EventDao,
) {
    fun observeRange(from: LocalDate, to: LocalDate, zone: ZoneId): Flow<List<EventEntity>> {
        val b = bounds(from, to, zone)
        return dao.observeRange(b.fromDate, b.toDate, b.fromMillis, b.toExclusiveMillis)
    }

    fun observe(id: String): Flow<EventEntity?> = dao.observe(id)

    suspend fun get(id: String): EventEntity? = dao.get(id)

    suspend fun upsert(entity: EventEntity) = dao.upsert(entity)

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun clear() = dao.clear()

    suspend fun replaceRange(from: LocalDate, to: LocalDate, zone: ZoneId, entities: List<EventEntity>) {
        val b = bounds(from, to, zone)
        dao.replaceRange(b.fromDate, b.toDate, b.fromMillis, b.toExclusiveMillis, entities)
    }

    private class Bounds(val fromDate: String, val toDate: String, val fromMillis: Long, val toExclusiveMillis: Long)

    /** 시간 일정은 [zone]의 `from` 00:00 ~ `to` 다음 날 00:00 (서버와 같은 규칙) */
    private fun bounds(from: LocalDate, to: LocalDate, zone: ZoneId) = Bounds(
        fromDate = from.toString(),
        toDate = to.toString(),
        fromMillis = from.atStartOfDay(zone).toInstant().toEpochMilli(),
        toExclusiveMillis = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
    )
}
