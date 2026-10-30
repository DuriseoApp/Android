package com.duriseo.android.data.remote.me

import com.duriseo.android.data.remote.auth.SocialProviderDto
import com.duriseo.android.data.remote.auth.toDomain
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MePartner
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SpaceType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

// API.md 3.1 `Me`, `MeSpace`, `MePartner`. 문서에 없는 enum 값은 역직렬화 오류가 되고,
// ApiCaller가 이를 INTERNAL_ERROR(공통 오류)로 바꾼다 (값을 지어내지 않는다).

@Serializable
enum class AccountTypeDto { GUEST, MEMBER }

@Serializable
enum class SpaceTypeDto { PERSONAL, COUPLE }

@Serializable
data class MePartnerDto(
    val id: String,
    val nickname: String,
    val color: String,
    val hasProfileImage: Boolean,
)

@Serializable
data class MeSpaceDto(
    val id: String,
    val type: SpaceTypeDto,
    val myColor: String,
    val partner: MePartnerDto? = null,
    val togetherColor: String? = null,
    @Contextual val startDate: LocalDate? = null,
    val importPending: Boolean,
)

@Serializable
data class MeDto(
    val id: String,
    val accountType: AccountTypeDto,
    val nickname: String,
    val timeZone: String,
    val providers: List<SocialProviderDto>,
    val hasProfileImage: Boolean,
    @Contextual val createdAt: Instant,
    val space: MeSpaceDto,
)

/**
 * `PATCH /me` 요청. 없는 필드는 바꾸지 않으므로 보내지 않을 필드는 `null`로 둔다.
 * API 본문용 `Json`이 `explicitNulls = false`라서 `null`은 필드째 생략된다 (`null`을 보내면 400, API 3.1).
 */
@Serializable
data class UpdateMeRequest(
    val nickname: String? = null,
    val timeZone: String? = null,
)

/** `PUT /me/devices` 요청 (API 3.1). 단위 7(FCM)이 쓴다. `platform` 기본값은 서버가 `ANDROID`로 본다 */
@Serializable
data class RegisterDeviceRequest(
    val fcmToken: String,
    val platform: String? = null,
) {
    override fun toString(): String = "RegisterDeviceRequest(platform=$platform, fcmToken=***)"
}

fun MeDto.toDomain(): Me = Me(
    id = id,
    accountType = when (accountType) {
        AccountTypeDto.GUEST -> AccountType.GUEST
        AccountTypeDto.MEMBER -> AccountType.MEMBER
    },
    nickname = nickname,
    timeZone = timeZone,
    providers = providers.map { it.toDomain() },
    hasProfileImage = hasProfileImage,
    createdAt = createdAt,
    space = space.toDomain(),
)

private fun MeSpaceDto.toDomain(): MeSpace = MeSpace(
    id = id,
    type = when (type) {
        SpaceTypeDto.PERSONAL -> SpaceType.PERSONAL
        SpaceTypeDto.COUPLE -> SpaceType.COUPLE
    },
    myColor = myColor,
    partner = partner?.let { MePartner(it.id, it.nickname, it.color, it.hasProfileImage) },
    togetherColor = togetherColor,
    startDate = startDate,
    importPending = importPending,
)
