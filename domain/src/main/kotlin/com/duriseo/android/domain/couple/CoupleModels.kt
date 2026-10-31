package com.duriseo.android.domain.couple

import com.duriseo.android.domain.me.SpaceType
import java.time.Instant
import java.time.LocalDate

/**
 * 초대코드 (API.md 3.2 `InviteCode`). [code]는 6자리 대문자.
 * 남은 시간과 재요청 가능 시각은 서버 시각이 아니라 기기 시계와 비교해 계산한다 (진행 기록 결정 4).
 */
data class InviteCode(
    val code: String,
    /** 만료 시각 (생성 + 10분) */
    val expiresAt: Instant,
    /** 이 시각부터 재요청할 수 있음 (생성 + 60초) */
    val reissueAvailableAt: Instant,
)

/** `POST /couple/join` 200 (API.md 3.2 `CoupleJoinResult`) */
data class CoupleJoinResult(
    val spaceId: String,
    /** 유예 기간 안의 이전 커플 공간을 복구했는지 (FR-COUPLE-06) */
    val restored: Boolean,
    /** 요청자에게 가져오기 대기 중인 개인 공간이 있는지 */
    val importPending: Boolean,
)

/**
 * 사람 색 변경 요청 (`PATCH /space/colors`, API.md 3.2). 모든 필드는 선택이고 없는 필드는 바꾸지 않는다.
 * [members]·[together]가 모두 비면 서버가 400 `VALIDATION_FAILED`로 답하므로 보내기 전에 [isEmpty]로 거른다.
 */
data class ColorChange(
    val members: List<MemberColor> = emptyList(),
    val together: String? = null,
) {
    val isEmpty: Boolean get() = members.isEmpty() && together == null
}

/** 한 사람의 색 ([color]는 `pick-*` 팔레트 키) */
data class MemberColor(
    val userId: String,
    val color: String,
)

/** 공간 상세 (API.md 3.2 `Space`). `PATCH /space`·`PATCH /space/colors`의 응답 */
data class Space(
    val id: String,
    val type: SpaceType,
    val togetherColor: String?,
    val startDate: LocalDate?,
    val members: List<SpaceMember>,
)

/** 공간의 활성 구성원 (API.md 3.2 `SpaceMember`). [color]는 팔레트 키 */
data class SpaceMember(
    val userId: String,
    val nickname: String,
    val color: String,
    val hasProfileImage: Boolean,
)
