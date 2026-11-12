package com.duriseo.android.data.local.memo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// [MemoEntity.listType]의 값. 보기 세 개는 `MemoView.name`과 같다 (`GET /memos?view=`의 보기별 목록)
const val MEMO_LIST_NEWEST = "NEWEST"
const val MEMO_LIST_OLDEST = "OLDEST"
const val MEMO_LIST_FAVORITES = "FAVORITES"

/** 고정 메모 전체 (`GET /memos/pinned`) */
const val MEMO_LIST_PINNED = "PINNED"

/** 캘린더용 날짜 메모 (`GET /memos/dated`) */
const val MEMO_LIST_DATED = "DATED"

/** 상세 (`GET /memos/{id}`, 쓰기 응답). 메모마다 한 행 */
const val MEMO_LIST_DETAIL = "DETAIL"

/** [MemoPageStateEntity.key]의 값: 그 밖에 보기별 키는 [MEMO_LIST_NEWEST] 등과 같다 */
const val MEMO_STATE_COUNTS = "COUNTS"

/** 보기별 목록의 `listType`들. 이어 받기 상태([MemoPageStateEntity])가 있는 목록이다 */
val MEMO_VIEW_LISTS = listOf(MEMO_LIST_NEWEST, MEMO_LIST_OLDEST, MEMO_LIST_FAVORITES)

/**
 * 메모 캐시 (API.md 3.4 `Memo`). 보기별 목록·고정 목록·날짜 목록·상세가 같은 모양이라 한 테이블에 [listType]으로 나눠 담는다.
 *
 * [position]은 응답 배열의 위치다 (여러 페이지면 페이지를 이어 붙인 순서). 앱은 다시 정렬하지 않고 이 위치 그대로 보인다
 * (API.md 3.4 "앱 동작"). 날짜 목록은 `memo_date`로 묶고 같은 날 안에서 [position] 순이다 (서버 정렬 그대로).
 * 같은 메모가 여러 목록에 있을 수 있고 행 [id]가 클수록 나중에 저장한 값이다 (상세는 가장 큰 [id]를 쓴다).
 * 날짜는 `YYYY-MM-DD`, 일시는 ISO 8601 UTC 문자열이다. 현재 활성 공간의 값만 담고 공간이 바뀌면 비운다. 스키마 버전 5에서 더했다.
 */
@Entity(
    tableName = "memo",
    indices = [Index("list_type", "position"), Index("list_type", "memo_date"), Index("memo_id")],
)
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "list_type") val listType: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "memo_id") val memoId: String,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "body") val body: String?,
    @ColumnInfo(name = "memo_date") val memoDate: String?,
    @ColumnInfo(name = "author_id") val authorId: String,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean,
    @ColumnInfo(name = "pinned_at") val pinnedAt: String?,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

/**
 * 메모 목록의 이어 받기 상태. 보기 키([MEMO_LIST_NEWEST] 등)는 [nextCursor](서버가 준 불투명 문자열 그대로, null이면 마지막 페이지)와
 * [totalCount](그 보기의 전체 개수, 고정 제외)를 담는다. 행이 있다는 것은 그 보기를 받은 적이 있다는 뜻이다.
 * [MEMO_STATE_COUNTS] 키는 [favoriteCount](내 즐겨찾기 중 고정되지 않은 것의 개수)만 담는다. 스키마 버전 5.
 */
@Entity(tableName = "memo_page_state")
data class MemoPageStateEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "next_cursor") val nextCursor: String?,
    @ColumnInfo(name = "total_count") val totalCount: Int?,
    @ColumnInfo(name = "favorite_count") val favoriteCount: Int?,
)
