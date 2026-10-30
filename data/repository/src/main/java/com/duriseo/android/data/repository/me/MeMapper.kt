package com.duriseo.android.data.repository.me

import com.duriseo.android.data.local.me.MeEntity
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MePartner
import com.duriseo.android.domain.me.MeSpace
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.me.SpaceType
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val PROVIDER_SEPARATOR = ","

internal fun Me.toEntity(): MeEntity = MeEntity(
    id = id,
    accountType = accountType.name,
    nickname = nickname,
    timeZone = timeZone,
    providers = providers.joinToString(PROVIDER_SEPARATOR) { it.name },
    hasProfileImage = hasProfileImage,
    createdAt = createdAt.toEpochMilli(),
    spaceId = space.id,
    spaceType = space.type.name,
    myColor = space.myColor,
    partnerId = space.partner?.id,
    partnerNickname = space.partner?.nickname,
    partnerColor = space.partner?.color,
    partnerHasProfileImage = space.partner?.hasProfileImage,
    togetherColor = space.togetherColor,
    startDate = space.startDate?.toString(),
    importPending = space.importPending,
)

/**
 * 저장된 행을 도메인 모델로 바꾼다. 모르는 값(`accountType`, `providers`, `space_type`)이나 깨진 날짜가 있으면
 * 기본값으로 때우지 않고 null이다 (문서에 없는 값을 지어내지 않는다). 다음 refresh가 새로 채운다.
 */
internal fun MeEntity.toDomainOrNull(): Me? {
    val account = AccountType.entries.firstOrNull { it.name == accountType } ?: return null
    val spaceKind = SpaceType.entries.firstOrNull { it.name == spaceType } ?: return null
    val providerList = if (providers.isEmpty()) {
        emptyList()
    } else {
        providers.split(PROVIDER_SEPARATOR).map { name ->
            SocialProvider.entries.firstOrNull { it.name == name } ?: return null
        }
    }
    // 다른 모듈의 프로퍼티는 스마트 캐스트가 안 되어 지역 값으로 받는다
    val partnerIdValue = partnerId
    val partner = if (partnerIdValue == null) {
        null
    } else {
        MePartner(
            id = partnerIdValue,
            nickname = partnerNickname ?: return null,
            color = partnerColor ?: return null,
            hasProfileImage = partnerHasProfileImage ?: return null,
        )
    }
    val start = try {
        startDate?.let { LocalDate.parse(it) }
    } catch (e: DateTimeParseException) {
        return null
    }
    return Me(
        id = id,
        accountType = account,
        nickname = nickname,
        timeZone = timeZone,
        providers = providerList,
        hasProfileImage = hasProfileImage,
        createdAt = Instant.ofEpochMilli(createdAt),
        space = MeSpace(
            id = spaceId,
            type = spaceKind,
            myColor = myColor,
            partner = partner,
            togetherColor = togetherColor,
            startDate = start,
            importPending = importPending,
        ),
    )
}
