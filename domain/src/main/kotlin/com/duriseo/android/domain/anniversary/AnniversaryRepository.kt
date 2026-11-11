package com.duriseo.android.domain.anniversary

import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 기념일 (API.md 3.3 "기념일"). 화면은 Room의 `observe…`만 구독한다 (NFR-OFF-01, ARCHITECTURE.md 2장).
 *
 * - 응답 배열의 **순서를 그대로** 쓴다. Room에 위치를 함께 저장하고 앱이 다시 정렬하지 않는다 (API.md "리드 보충 (순서 사용)").
 * - `refresh…`·`loadMore…`는 읽기라서 오프라인이면 요청 없이 `DataError.Offline`이다. 부른 쪽이 조용히 넘긴다.
 * - 쓰기([create]·[update]·[delete])는 온라인에서만 한다. 오프라인이면 요청 없이 `Offline` (NFR-OFF-02).
 *   성공하면 upcoming·past 첫 페이지와 [setVisibleRange]로 알린 캘린더 범위를 다시 불러온다 (자동 기념일 계산은 서버가 한다).
 * - 404 `NOT_FOUND`면 Room에서도 그 기념일을 지운다. 403 `NOT_AUTHOR`면 [refreshAll]과 같이 다시 불러온다 (D-121).
 */
interface AnniversaryRepository {
    /** [from]~[to](포함)의 발생일. 캘린더용이고 위치 순서 그대로다 */
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<AnniversaryOccurrence>>

    /**
     * 캘린더가 지금 보이는 범위를 알린다. 쓰기 뒤·[refreshAll]이 이 범위를 다시 불러온다 (달이 바뀔 때마다 부른다).
     * 캐시를 지우면(해제·로그아웃·공간 변경) 이 값도 비워지므로 캘린더가 다시 알려야 한다.
     */
    fun setVisibleRange(from: LocalDate, to: LocalDate)

    /** SCR-ANNIV-01 "다가오는 기념일" */
    fun observeUpcoming(): Flow<UpcomingAnniversaries>

    /** SCR-ANNIV-01 "지난 기념일" (개수 포함) */
    fun observePast(): Flow<PastAnniversaries>

    /**
     * `GET /anniversaries?from=&to=`. 범위를 1950-01-01 ~ 2050-12-31 안으로 잘라 요청하고(밖이면 요청 없이 성공),
     * 응답을 받으면 그 범위의 발생일을 Room에서 지우고 새로 넣는다. 요청 뒤 캐시가 지워졌거나 겹치는 더 새 응답이 이미 저장됐으면 저장하지 않는다.
     * 월 조회의 앞뒤 하루 여유(Q-19 ②)는 부르는 쪽이 정한다 ([calendarMonthRange]).
     */
    suspend fun refreshRange(from: LocalDate, to: LocalDate): DataResult<Unit>

    /** `GET /anniversaries/upcoming` 첫 페이지: 목록을 교체한다 (화면에 들어갈 때) */
    suspend fun refreshUpcoming(): DataResult<Unit>

    /** `GET /anniversaries/upcoming?cursor=` 다음 페이지를 이어 붙인다. 다음 페이지가 없으면 요청 없이 성공 */
    suspend fun loadMoreUpcoming(): DataResult<Unit>

    /** `GET /anniversaries/past` 첫 페이지: 목록과 `totalCount`를 교체한다 */
    suspend fun refreshPast(): DataResult<Unit>

    /** `GET /anniversaries/past?cursor=` 다음 페이지를 이어 붙인다. 다음 페이지가 없으면 요청 없이 성공 */
    suspend fun loadMorePast(): DataResult<Unit>

    /** upcoming·past 첫 페이지와 보이는 캘린더 범위를 다시 불러온다 (처음 만난 날 변경 뒤, 403 뒤). 하나라도 실패하면 첫 실패를 돌려준다 */
    suspend fun refreshAll(): DataResult<Unit>

    /** `POST /anniversaries` (201). 성공하면 목록과 달을 다시 불러온다 */
    suspend fun create(anniversary: NewAnniversary): DataResult<Anniversary>

    /** `PATCH /anniversaries/{id}`. 바뀐 것이 없으면([AnniversaryPatch.isEmpty]) 요청 없이 성공으로 끝낸다 */
    suspend fun update(id: String, patch: AnniversaryPatch): DataResult<Unit>

    /** `DELETE /anniversaries/{id}` (204) */
    suspend fun delete(id: String): DataResult<Unit>
}

/**
 * 캘린더 월 조회 범위: 그 달 1일 하루 전 ~ 말일 하루 뒤 (Q-19 ②). `daysLeft`가 서버에 저장된 시간대 기준이라
 * 기기 시간대와 어긋나도 보이는 달의 가장자리 날짜가 비지 않게 한다. 3개월 상한 안이다 (API.md 조회 규칙).
 */
fun calendarMonthRange(month: java.time.YearMonth): Pair<LocalDate, LocalDate> =
    month.atDay(1).minusDays(1) to month.atEndOfMonth().plusDays(1)
