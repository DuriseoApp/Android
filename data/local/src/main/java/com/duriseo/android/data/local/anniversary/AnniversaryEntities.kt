package com.duriseo.android.data.local.anniversary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** [AnniversaryOccurrenceEntity.listType]의 값: 캘린더용 범위 발생일 */
const val LIST_RANGE = "RANGE"

/** [AnniversaryOccurrenceEntity.listType]의 값: SCR-ANNIV-01 "다가오는 기념일" */
const val LIST_UPCOMING = "UPCOMING"

/** [AnniversaryPageStateEntity.key]의 값 */
const val STATE_UPCOMING = "UPCOMING"
const val STATE_PAST = "PAST"

/**
 * 기념일 발생일 캐시 (API.md 3.3 `AnniversaryOccurrence`). 캘린더용 범위(`RANGE`)와 다가오는 목록(`UPCOMING`)이
 * 같은 모양이라 한 테이블에 [listType]으로 나눠 담는다.
 *
 * [position]은 응답 배열의 위치다 (여러 페이지면 페이지를 이어 붙인 순서). 앱은 이름 등으로 다시 정렬하지 않고
 * 이 위치 그대로 보인다 (API.md "리드 보충 (순서 사용)", FR-ANNIV-04 AC5). 캘린더 범위는 `date`로 묶고 같은 날 안에서 [position] 순이다.
 * 현재 활성 공간의 값만 담는다. 공간이 바뀌면 비운다. 스키마 버전 4에서 더했다.
 */
@Entity(
    tableName = "anniversary_occurrence",
    indices = [Index("list_type", "date"), Index("list_type", "position"), Index("anniversary_id")],
)
data class AnniversaryOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "list_type") val listType: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "anniversary_id") val anniversaryId: String?,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "days_left") val daysLeft: Int,
    @ColumnInfo(name = "repeats_yearly") val repeatsYearly: Boolean,
    @ColumnInfo(name = "author_id") val authorId: String?,
)

/** "지난 기념일" 캐시 (`GET /anniversaries/past`의 항목). [position]은 응답 위치 (최근 순). 스키마 버전 4 */
@Entity(
    tableName = "anniversary_past",
    indices = [Index("anniversary_id")],
)
data class PastAnniversaryEntity(
    @PrimaryKey @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "anniversary_id") val anniversaryId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "author_id") val authorId: String,
)

/**
 * 커서 페이지 목록의 이어 받기 상태. [key]는 [STATE_UPCOMING]·[STATE_PAST]. [nextCursor]는 서버가 준 불투명 문자열 그대로이고
 * null이면 마지막 페이지다. [totalCount]는 past만 값이 있다 (전체 개수, 0이면 "지난 기념일" 행을 숨긴다). 스키마 버전 4.
 */
@Entity(tableName = "anniversary_page_state")
data class AnniversaryPageStateEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "next_cursor") val nextCursor: String?,
    @ColumnInfo(name = "total_count") val totalCount: Int?,
)
