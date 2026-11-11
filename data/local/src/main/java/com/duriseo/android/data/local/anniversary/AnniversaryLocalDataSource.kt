package com.duriseo.android.data.local.anniversary

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 기념일 캐시(Room)를 `data:repository`에 보여 주는 얇은 창구. Room 타입은 이 모듈 밖으로 나가지 않는다 */
@Singleton
class AnniversaryLocalDataSource @Inject constructor(
    private val dao: AnniversaryDao,
) {
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<AnniversaryOccurrenceEntity>> =
        dao.observeRange(from.toString(), to.toString())

    suspend fun replaceRange(from: LocalDate, to: LocalDate, entities: List<AnniversaryOccurrenceEntity>) =
        dao.replaceRange(from.toString(), to.toString(), entities)

    fun observeUpcoming(): Flow<List<AnniversaryOccurrenceEntity>> = dao.observeUpcoming()

    fun observeUpcomingState(): Flow<AnniversaryPageStateEntity?> = dao.observeState(STATE_UPCOMING)

    suspend fun upcomingNextCursor(): String? = dao.getState(STATE_UPCOMING)?.nextCursor

    suspend fun replaceUpcoming(items: List<AnniversaryOccurrenceEntity>, nextCursor: String?) =
        dao.replaceUpcoming(items, nextCursor)

    suspend fun appendUpcoming(items: List<AnniversaryOccurrenceEntity>, nextCursor: String?) =
        dao.appendUpcoming(items, nextCursor)

    fun observePast(): Flow<List<PastAnniversaryEntity>> = dao.observePast()

    fun observePastState(): Flow<AnniversaryPageStateEntity?> = dao.observeState(STATE_PAST)

    suspend fun pastNextCursor(): String? = dao.getState(STATE_PAST)?.nextCursor

    suspend fun replacePast(items: List<PastAnniversaryEntity>, nextCursor: String?, totalCount: Int) =
        dao.replacePast(items, nextCursor, totalCount)

    suspend fun appendPast(items: List<PastAnniversaryEntity>, nextCursor: String?, totalCount: Int?) =
        dao.appendPast(items, nextCursor, totalCount)

    /** 404: 그 기념일의 발생일·지난 항목을 지운다 */
    suspend fun delete(anniversaryId: String) = dao.deleteAnniversary(anniversaryId)

    suspend fun clear() = dao.clear()
}
