package com.duriseo.android.domain.profile

/**
 * 프로필 사진을 가리키는 값 (API.md `GET /users/{id}/profile-image?v=<version>`, 단위 8).
 * 화면(feature)은 서버 주소·URL을 모른다. 이 값을 그대로 `DuriseoAvatar`에 넘기면 `data:remote`가 등록한
 * 이미지 로더가 인증 헤더를 붙여 받아 온다. [version]이 캐시 키이므로 사진이 바뀌면 값이 달라져 새로 받는다.
 */
data class ProfileImage(
    val userId: String,
    val version: String,
)

/** 사진 버전이 없으면(사진 없음) null이다. null이면 이미지를 요청하지 않고 사람 색 + 글자 아바타를 그린다 (FR-AUTH-07 AC4) */
fun profileImageOf(userId: String?, version: String?): ProfileImage? =
    if (userId.isNullOrBlank() || version.isNullOrBlank()) null else ProfileImage(userId, version)
