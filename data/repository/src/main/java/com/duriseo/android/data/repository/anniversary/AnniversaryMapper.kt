package com.duriseo.android.data.repository.anniversary

import com.duriseo.android.data.local.anniversary.AnniversaryOccurrenceEntity
import com.duriseo.android.data.local.anniversary.PastAnniversaryEntity
import com.duriseo.android.domain.anniversary.AnniversaryKind
import com.duriseo.android.domain.anniversary.AnniversaryOccurrence
import com.duriseo.android.domain.anniversary.PastAnniversary
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** 도메인 발생일 -> Room 행. [position]은 응답 배열의 위치 (다시 정렬하지 않는다) */
internal fun AnniversaryOccurrence.toEntity(listType: String, position: Int) = AnniversaryOccurrenceEntity(
    listType = listType,
    position = position,
    anniversaryId = anniversaryId,
    kind = kind.name,
    name = name,
    date = date.toString(),
    daysLeft = daysLeft,
    repeatsYearly = repeatsYearly,
    authorId = authorId,
)

/** Room 행 -> 도메인. 깨진 행(모르는 종류, 날짜 형식)이면 null (화면에 내보내지 않는다) */
internal fun AnniversaryOccurrenceEntity.toDomainOrNull(): AnniversaryOccurrence? {
    val k = AnniversaryKind.fromCode(kind) ?: return null
    val d = try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        return null
    }
    return AnniversaryOccurrence(
        anniversaryId = anniversaryId,
        kind = k,
        name = name,
        date = d,
        daysLeft = daysLeft,
        repeatsYearly = repeatsYearly,
        authorId = authorId,
    )
}

internal fun PastAnniversary.toEntity(position: Int) = PastAnniversaryEntity(
    position = position,
    anniversaryId = anniversaryId,
    name = name,
    date = date.toString(),
    authorId = authorId,
)

internal fun PastAnniversaryEntity.toDomainOrNull(): PastAnniversary? {
    val d = try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        return null
    }
    return PastAnniversary(anniversaryId, name, d, authorId)
}
