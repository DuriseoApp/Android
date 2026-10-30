package com.duriseo.android.domain.me

import java.time.Instant
import java.time.LocalDate

/** `users.account_type` (API.md 3.1 `Me.accountType`) */
enum class AccountType { GUEST, MEMBER }

/** 로그인 수단 (API.md 3.1 `Me.providers`) */
enum class SocialProvider { GOOGLE, KAKAO }

/** 공간 종류 (API.md 3.1 `MeSpace.type`) */
enum class SpaceType { PERSONAL, COUPLE }

/** 커플 공간의 상대 (API.md 3.1 `MePartner`). [color]는 사람 색 팔레트 키 */
data class MePartner(
    val id: String,
    val nickname: String,
    val color: String,
    val hasProfileImage: Boolean,
)

/** 현재 활성 공간 요약 (API.md 3.1 `MeSpace`). 개인 공간이면 [partner]·[togetherColor]·[startDate]가 null */
data class MeSpace(
    val id: String,
    val type: SpaceType,
    val myColor: String,
    val partner: MePartner?,
    val togetherColor: String?,
    val startDate: LocalDate?,
    val importPending: Boolean,
)

/** 내 정보 + 현재 공간 요약 (API.md 3.1 `Me`). [createdAt]은 가입일 */
data class Me(
    val id: String,
    val accountType: AccountType,
    val nickname: String,
    val timeZone: String,
    val providers: List<SocialProvider>,
    val hasProfileImage: Boolean,
    val createdAt: Instant,
    val space: MeSpace,
)
