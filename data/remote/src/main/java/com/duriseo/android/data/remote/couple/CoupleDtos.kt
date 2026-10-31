package com.duriseo.android.data.remote.couple

import com.duriseo.android.data.remote.me.SpaceTypeDto
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.CoupleJoinResult
import com.duriseo.android.domain.couple.InviteCode
import com.duriseo.android.domain.couple.Space
import com.duriseo.android.domain.couple.SpaceMember
import com.duriseo.android.domain.me.SpaceType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

// API.md 3.2 `InviteCode`, `CoupleJoinResult`, `Space`, `SpaceMember`과 요청 본문.
// 응답의 모르는 필드는 무시한다(`ignoreUnknownKeys`). 요청의 null 필드는 `explicitNulls = false`라서 생략된다.

@Serializable
data class InviteCodeDto(
    val code: String,
    @Contextual val expiresAt: Instant,
    @Contextual val reissueAvailableAt: Instant,
)

@Serializable
data class CoupleJoinResultDto(
    val spaceId: String,
    val restored: Boolean,
    val importPending: Boolean,
)

@Serializable
data class SpaceMemberDto(
    val userId: String,
    val nickname: String,
    val color: String,
    val hasProfileImage: Boolean,
)

@Serializable
data class SpaceDto(
    val id: String,
    val type: SpaceTypeDto,
    val togetherColor: String? = null,
    @Contextual val startDate: LocalDate? = null,
    val members: List<SpaceMemberDto>,
)

/** `POST /couple/join` 요청. 대문자 변환은 서버도 하지만 앱이 먼저 대문자 6자리로 만들어 보낸다 */
@Serializable
data class JoinCoupleRequest(val code: String)

/** `POST /couple/import` 요청. JSON 필드 이름은 `import`다 */
@Serializable
data class ImportRequest(
    @SerialName("import") val shouldImport: Boolean,
)

/** `PATCH /space` 요청. `startDate`는 `YYYY-MM-DD`이고 지우기(`null`)는 없다 (D-84) */
@Serializable
data class UpdateStartDateRequest(
    @Contextual val startDate: LocalDate,
)

@Serializable
data class MemberColorDto(
    val userId: String,
    val color: String,
)

/** `PATCH /space/colors` 요청. 바꾸지 않을 필드는 `null`이라 보내지 않는다 (`null`을 보내면 400) */
@Serializable
data class UpdateColorsRequest(
    val members: List<MemberColorDto>? = null,
    val together: String? = null,
)

fun InviteCodeDto.toDomain(): InviteCode = InviteCode(code, expiresAt, reissueAvailableAt)

fun CoupleJoinResultDto.toDomain(): CoupleJoinResult = CoupleJoinResult(spaceId, restored, importPending)

fun SpaceDto.toDomain(): Space = Space(
    id = id,
    type = when (type) {
        SpaceTypeDto.PERSONAL -> SpaceType.PERSONAL
        SpaceTypeDto.COUPLE -> SpaceType.COUPLE
    },
    togetherColor = togetherColor,
    startDate = startDate,
    members = members.map { SpaceMember(it.userId, it.nickname, it.color, it.hasProfileImage) },
)

/** 바뀐 필드만 담는다. `members`가 비었으면 필드를 보내지 않는다 */
fun ColorChange.toRequest(): UpdateColorsRequest = UpdateColorsRequest(
    members = members.takeIf { it.isNotEmpty() }?.map { MemberColorDto(it.userId, it.color) },
    together = together,
)
