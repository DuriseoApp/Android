package com.duriseo.android.data.remote.memo

import com.duriseo.android.domain.memo.FieldChange
import com.duriseo.android.domain.memo.Memo
import com.duriseo.android.domain.memo.MemoPatch
import com.duriseo.android.domain.memo.MemoRules
import com.duriseo.android.domain.memo.NewMemo
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant
import java.time.LocalDate

// API.md 3.4 `Memo`와 요청 본문.

/** `Memo`: 목록·고정·날짜·상세·생성·수정 응답이 같은 모양이다. 값이 없는 필드는 `null`로 온다 */
@Serializable
data class MemoDto(
    val id: String,
    val title: String? = null,
    val body: String? = null,
    @Contextual val memoDate: LocalDate? = null,
    val authorId: String,
    val isPinned: Boolean,
    @Contextual val pinnedAt: Instant? = null,
    val isFavorite: Boolean,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
)

/** `GET /memos` 응답 `{items, nextCursor, totalCount, favoriteCount}`. 마지막 페이지면 `nextCursor`는 null */
@Serializable
data class MemoPageResponse(
    val items: List<MemoDto>,
    val nextCursor: String? = null,
    val totalCount: Int,
    val favoriteCount: Int,
)

/** `POST /memos` 요청. 값이 없는 필드(null)는 보내지 않는다 (`explicitNulls = false`) */
@Serializable
data class CreateMemoRequest(
    val title: String? = null,
    val body: String? = null,
    @Contextual val memoDate: LocalDate? = null,
    val notifyPartner: Boolean? = null,
)

/** 입력을 D-132대로 정리해 보낸다: 제목은 앞뒤 공백 제거, 본문은 공백·줄바꿈뿐이면 보내지 않는다 */
internal fun NewMemo.toRequest() = CreateMemoRequest(
    title = MemoRules.normalizeTitle(title),
    body = MemoRules.normalizeBody(body),
    memoDate = memoDate,
    notifyPartner = notifyPartner,
)

/** 바뀐 필드만 담은 본문. `FieldChange.Set(null)`은 JSON `null`로 보내 그 필드를 지운다 (API.md 3.4 "PATCH") */
internal fun MemoPatch.toRequestBody(): JsonObject {
    val titleChange = title
    val bodyChange = body
    val dateChange = memoDate
    return buildJsonObject {
        if (titleChange is FieldChange.Set<String>) put("title", titleChange.value.toJson())
        if (bodyChange is FieldChange.Set<String>) put("body", bodyChange.value.toJson())
        if (dateChange is FieldChange.Set<LocalDate>) put("memoDate", dateChange.value?.toString().toJson())
    }
}

private fun String?.toJson(): JsonElement = if (this == null) JsonNull else JsonPrimitive(this)

internal fun MemoDto.toDomain() = Memo(
    id = id,
    title = title,
    body = body,
    memoDate = memoDate,
    authorId = authorId,
    isPinned = isPinned,
    pinnedAt = pinnedAt,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
