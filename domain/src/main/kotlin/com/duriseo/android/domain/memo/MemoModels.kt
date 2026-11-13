package com.duriseo.android.domain.memo

import java.time.Instant
import java.time.LocalDate

/**
 * 메모 (API.md 3.4 `Memo`). 목록·고정·날짜·상세·생성·수정 응답이 모두 같은 모양이다.
 *
 * @property title 앞뒤 공백을 지운 값. 없으면 null
 * @property body 본문 전체. 공백·줄바꿈뿐이었으면 null. 미리보기는 화면이 자른다
 * @property memoDate 메모 날짜. 없으면 null
 * @property authorId 작성자. 닉네임은 `Me`·공간의 구성원 정보에서 찾는다
 * @property pinnedAt 고정한 시각. 고정이 아니면 null
 * @property isFavorite **요청자 기준** 즐겨찾기 여부 (R-06)
 */
data class Memo(
    val id: String,
    val title: String?,
    val body: String?,
    val memoDate: LocalDate?,
    val authorId: String,
    val isPinned: Boolean,
    val pinnedAt: Instant?,
    val isFavorite: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** SCR-MEMO-01 보기 칩 (API.md 3.4 `view`). [code]는 `GET /memos?view=`의 값이다 */
enum class MemoView(val code: String) {
    NEWEST("newest"),
    OLDEST("oldest"),
    FAVORITES("favorites"),
}

/**
 * 한 보기의 이어 붙인 목록. 고정된 메모는 들어 있지 않다 (D-130).
 *
 * @property totalCount 그 보기의 전체 개수(고정 제외). "전체 메모 N개"에 쓴다. 아직 불러오지 않았으면 0
 * @property hasMore 다음 페이지가 있다 (`nextCursor`가 null이 아님)
 * @property isLoaded 이 보기를 한 번이라도 받아 저장했다. false면 빈 목록이 "메모 없음"이 아니라 "아직 안 받음"이다
 */
data class MemoList(
    val items: List<Memo>,
    val totalCount: Int,
    val hasMore: Boolean,
    val isLoaded: Boolean,
)

/**
 * `POST /memos` 입력. [title]·[body]는 **입력한 그대로**다 (정리는 보내기 직전에 [MemoRules]가 한다, D-132).
 * [notifyPartner]가 null이면 보내지 않는다 (서버 기본). 개인 공간에서는 null로 둔다 (FR-NOTI-04).
 */
data class NewMemo(
    val title: String?,
    val body: String?,
    val memoDate: LocalDate?,
    val notifyPartner: Boolean? = null,
)

/** `PATCH`에서 한 필드의 뜻: [Keep]이면 보내지 않고, [Set]이면 그 값(null이면 지움)을 보낸다 (API.md 3.4 "메모 쓰기 규칙") */
sealed interface FieldChange<out T> {
    data object Keep : FieldChange<Nothing>

    data class Set<T>(val value: T?) : FieldChange<T>
}

/** `PATCH /memos/{id}` 입력: 바뀐 필드만 담는다. 값은 이미 정리된(D-132) 값이다. 만드는 것은 [MemoRules.patchOf] */
data class MemoPatch(
    val title: FieldChange<String> = FieldChange.Keep,
    val body: FieldChange<String> = FieldChange.Keep,
    val memoDate: FieldChange<LocalDate> = FieldChange.Keep,
) {
    /** 바뀐 것이 없으면 요청 없이 닫는다 (FR-MEMO-03 AC4, D-123) */
    val isEmpty: Boolean
        get() = title is FieldChange.Keep && body is FieldChange.Keep && memoDate is FieldChange.Keep
}
