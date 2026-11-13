package com.duriseo.android.data.repository.memo

import com.duriseo.android.data.local.memo.MemoEntity
import com.duriseo.android.domain.memo.Memo
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** 도메인 메모 -> Room 행. [position]은 응답 배열의 위치 (다시 정렬하지 않는다) */
internal fun Memo.toEntity(listType: String, position: Int) = MemoEntity(
    listType = listType,
    position = position,
    memoId = id,
    title = title,
    body = body,
    memoDate = memoDate?.toString(),
    authorId = authorId,
    isPinned = isPinned,
    pinnedAt = pinnedAt?.toString(),
    isFavorite = isFavorite,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

/** Room 행 -> 도메인. 깨진 행(날짜·일시 형식)이면 null (화면에 내보내지 않는다) */
internal fun MemoEntity.toDomainOrNull(): Memo? = try {
    Memo(
        id = memoId,
        title = title,
        body = body,
        memoDate = memoDate?.let { LocalDate.parse(it) },
        authorId = authorId,
        isPinned = isPinned,
        pinnedAt = pinnedAt?.let { Instant.parse(it) },
        isFavorite = isFavorite,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )
} catch (e: DateTimeParseException) {
    null
}
