package com.duriseo.android.data.remote.response

import kotlinx.serialization.Serializable

// 응답 형식 1.1 (API.md): 성공은 봉투 없이 리소스 그대로(200/201), 204는 본문 없음.
// 최상위는 항상 객체이고, 값이 없는 필드는 null로 온다. 서버 `ApiResponses.kt`와 같은 모양이다.

/** 페이지가 없는 목록 (`GET /events`, `/anniversaries`, `/memos/dated`, `/memos/pinned`) */
@Serializable
data class ListResponse<T>(
    val items: List<T>,
)

/** 페이지가 있는 목록 (`cursor`, `size`를 받는 경로). 마지막 페이지면 [nextCursor]는 null. */
@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val nextCursor: String? = null,
)

/** 개수가 필요한 페이지 목록 (`GET /anniversaries/past`) */
@Serializable
data class CountedPageResponse<T>(
    val items: List<T>,
    val nextCursor: String? = null,
    val totalCount: Int,
)
