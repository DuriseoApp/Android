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

/**
 * 가져오기 대기 중인 개인 공간의 기록 개수 (API.md 3.1 `ImportCounts`, D-93).
 * 기념일은 직접 기념일만 센다. 0개인 종류도 화면에 보여 준다 (Q-13).
 */
data class ImportCounts(
    val events: Int,
    val anniversaries: Int,
    val memos: Int,
)

/**
 * 현재 활성 공간 요약 (API.md 3.1 `MeSpace`). 개인 공간이면 [partner]·[togetherColor]·[startDate]·[linkedAt]이 null
 *
 * @property linkedAt 커플 공간이 연결된 시각. 유예 기간 안에 복구했으면 다시 연결한 시각 (D-94)
 * @property importCounts 가져오기 대기 공간의 기록 개수. [importPending]이 false면 null (D-93)
 */
data class MeSpace(
    val id: String,
    val type: SpaceType,
    val myColor: String,
    val partner: MePartner?,
    val togetherColor: String?,
    val startDate: LocalDate?,
    val importPending: Boolean,
    val linkedAt: Instant? = null,
    val importCounts: ImportCounts? = null,
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
