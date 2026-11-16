package com.duriseo.android.domain.profile

/**
 * 기기에 받아 둔 프로필 사진(이미지 로더의 메모리·디스크 캐시)을 모두 지운다 (NFR-PRIV-01 취지).
 * 로그아웃·탈퇴·세션 끊김·계정 전환 때 부른다. 이전 사용자의 사진이 기기에 남지 않게 하려는 것이다.
 * 구현은 Coil을 아는 `data:remote`에 있다.
 */
interface ProfileImageCacheCleaner {
    suspend fun clear()
}
