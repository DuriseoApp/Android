package com.duriseo.android.data.remote.anniversary

import com.duriseo.android.domain.anniversary.Anniversary
import com.duriseo.android.domain.anniversary.AnniversaryKind
import com.duriseo.android.domain.anniversary.AnniversaryOccurrence
import com.duriseo.android.domain.anniversary.AnniversaryPatch
import com.duriseo.android.domain.anniversary.NewAnniversary
import com.duriseo.android.domain.anniversary.PastAnniversary
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant
import java.time.LocalDate

// API.md 3.3 `Anniversary`, `AnniversaryOccurrence`, "지난 기념일" 항목과 요청 본문.

@Serializable
data class AnniversaryDto(
    val id: String,
    val name: String,
    @Contextual val date: LocalDate,
    val repeatsYearly: Boolean,
    val authorId: String,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
)

/** 발생일 한 건. `kind`는 문자열로 받아 모르는 값이면 그 항목을 건너뛴다 */
@Serializable
data class AnniversaryOccurrenceDto(
    val anniversaryId: String? = null,
    val kind: String,
    val name: String,
    @Contextual val date: LocalDate,
    val daysLeft: Int,
    val repeatsYearly: Boolean,
    val authorId: String? = null,
)

/** `GET /anniversaries/past`의 항목: `{anniversaryId, name, date, authorId}` */
@Serializable
data class PastAnniversaryDto(
    val anniversaryId: String,
    val name: String,
    @Contextual val date: LocalDate,
    val authorId: String,
)

/** `POST /anniversaries` 요청. `repeatsYearly`가 null이면 보내지 않는다 (서버 기본: 켜짐) */
@Serializable
data class CreateAnniversaryRequest(
    val name: String,
    @Contextual val date: LocalDate,
    val repeatsYearly: Boolean? = null,
)

internal fun NewAnniversary.toRequest() = CreateAnniversaryRequest(name = name, date = date, repeatsYearly = repeatsYearly)

/** 바뀐 필드만 담은 본문 (unit-5.md 확인 필요 1) */
internal fun AnniversaryPatch.toRequestBody(): JsonObject = buildJsonObject {
    name?.let { put("name", JsonPrimitive(it)) }
    date?.let { put("date", JsonPrimitive(it.toString())) }
    repeatsYearly?.let { put("repeatsYearly", JsonPrimitive(it)) }
}

internal fun AnniversaryDto.toDomain() = Anniversary(
    id = id,
    name = name,
    date = date,
    repeatsYearly = repeatsYearly,
    authorId = authorId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/** 모르는 `kind`면 null */
internal fun AnniversaryOccurrenceDto.toDomainOrNull(): AnniversaryOccurrence? {
    val k = AnniversaryKind.fromCode(kind) ?: return null
    return AnniversaryOccurrence(
        anniversaryId = anniversaryId,
        kind = k,
        name = name,
        date = date,
        daysLeft = daysLeft,
        repeatsYearly = repeatsYearly,
        authorId = authorId,
    )
}

internal fun PastAnniversaryDto.toDomain() = PastAnniversary(anniversaryId, name, date, authorId)
