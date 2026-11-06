package com.duriseo.android.domain.event

import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

/**
 * 일정 (API.md 3.3). 화면은 Room의 [observeEvents]·[observeEvent]만 구독한다 (NFR-OFF-01, ARCHITECTURE.md 2장).
 *
 * - [refreshRange]·[refreshEvent]는 읽기라서 오프라인이면 요청 없이 `DataError.Offline`이다. 부른 쪽이 조용히 넘긴다.
 * - 쓰기([create]·[update]·[delete]·[setReminder])는 온라인에서만 한다. 오프라인이면 요청 없이 `Offline` (NFR-OFF-02).
 *   성공 응답을 Room에 반영한 뒤 Flow가 화면을 갱신한다 (낙관적 갱신 없음).
 * - 404 `NOT_FOUND`면 Room에서도 그 일정을 지운다 (ARCHITECTURE.md 5장). 403 `NOT_AUTHOR`면 그 일정을 다시 읽는다.
 */
interface EventRepository {
    /** [from]~[to](포함)에 하루라도 걸친 일정. 시간 일정의 날짜는 [zone] 기준 */
    fun observeEvents(from: LocalDate, to: LocalDate, zone: ZoneId): Flow<List<Event>>

    fun observeEvent(id: String): Flow<Event?>

    /**
     * `GET /events`. [from]~[to]는 1950-01-01 ~ 2050-12-31 안으로 잘라 요청하고(밖이면 요청 없이 성공),
     * 응답을 받으면 그 범위에 걸친 일정을 Room에서 지우고 새로 넣는다.
     */
    suspend fun refreshRange(from: LocalDate, to: LocalDate, zone: ZoneId): DataResult<Unit>

    /** `GET /events/{id}`. 응답을 Room에 넣는다 */
    suspend fun refreshEvent(id: String): DataResult<Unit>

    /** `POST /events` (201). `reminder`는 항상 null이라 알림은 [setReminder]로 따로 정한다 (D-59) */
    suspend fun create(event: NewEvent): DataResult<Event>

    /** `PATCH /events/{id}`. 바뀐 것이 없으면([EventPatch.isEmpty]) 요청 없이 성공으로 끝낸다 */
    suspend fun update(id: String, patch: EventPatch): DataResult<Unit>

    /** `DELETE /events/{id}` (204) */
    suspend fun delete(id: String): DataResult<Unit>

    /** 내 알림. [option]이 null이면 `DELETE …/reminder`(없음, 멱등 204), 값이면 `PUT …/reminder` */
    suspend fun setReminder(id: String, option: ReminderOption?): DataResult<Unit>
}
