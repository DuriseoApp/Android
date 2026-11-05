package com.duriseo.android.data.remote.event

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.event.Event
import com.duriseo.android.domain.event.EventPatch
import com.duriseo.android.domain.event.NewEvent
import com.duriseo.android.domain.event.ReminderOption
import com.duriseo.android.domain.result.DataResult
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

/** 일정 API 호출을 [DataResult]와 도메인 모델로 돌려준다 (API.md 3.3). 오류 코드는 그대로 `ApiError`로 올린다 */
@Singleton
class EventRemoteDataSource @Inject constructor(
    private val api: EventApi,
    private val apiCaller: ApiCaller,
) {
    /** `GET /events?from=&to=` */
    suspend fun list(from: LocalDate, to: LocalDate): DataResult<List<Event>> =
        when (val r = apiCaller.call { api.getEvents(from.toString(), to.toString()) }) {
            is DataResult.Success -> r.value.items.mapAll()
            is DataResult.Failure -> r
        }

    /** `POST /events` (201) */
    suspend fun create(event: NewEvent): DataResult<Event> =
        apiCaller.call { api.create(event.toRequest()) }.mapEvent()

    /** `GET /events/{id}` */
    suspend fun get(id: String): DataResult<Event> = apiCaller.call { api.get(id) }.mapEvent()

    /** `PATCH /events/{id}` */
    suspend fun update(id: String, patch: EventPatch): DataResult<Event> =
        apiCaller.call { api.update(id, patch.toRequestBody()) }.mapEvent()

    /** `DELETE /events/{id}` (204) */
    suspend fun delete(id: String): DataResult<Unit> = apiCaller.callUnit { api.delete(id) }

    /** `PUT /events/{id}/reminder` */
    suspend fun putReminder(id: String, option: ReminderOption): DataResult<Event> =
        apiCaller.call { api.putReminder(id, ReminderRequest(option.name)) }.mapEvent()

    /** `DELETE /events/{id}/reminder` (204, 멱등) */
    suspend fun deleteReminder(id: String): DataResult<Unit> = apiCaller.callUnit { api.deleteReminder(id) }

    private fun DataResult<EventDto>.mapEvent(): DataResult<Event> = when (this) {
        is DataResult.Success -> runCatchingParse { value.toDomain() }
        is DataResult.Failure -> this
    }

    private fun List<EventDto>.mapAll(): DataResult<List<Event>> = runCatchingParse { map { it.toDomain() } }

    /** 약속한 모양이 아닌 일시·날짜는 공통 오류(`INTERNAL_ERROR`)로 본다 (ARCHITECTURE.md 3장) */
    private inline fun <T> runCatchingParse(block: () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (e: DateTimeParseException) {
        DataResult.Failure(DataError.ApiError(ErrorCode.INTERNAL_ERROR.code, details = null, traceId = null))
    }
}
