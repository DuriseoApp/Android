package com.duriseo.android.domain.appops

import java.time.Instant

internal val ANDROID_34 = AppEnvironment(platform = AppPlatform.ANDROID, build = 10, osVersion = 34)
internal val NOW: Instant = Instant.parse("2026-11-25T03:00:00Z")

internal fun notice(
    id: String,
    closable: Boolean = true,
    title: String? = "제목 $id",
    body: String? = "본문",
    image: PopupImage = PopupImage.None,
    imageAlt: String? = null,
    platform: AppPlatform? = null,
    startsAt: Instant? = null,
    endsAt: Instant? = null,
) = AppNotice(id, closable, title, body, image, imageAlt, link = null, platform = platform, startsAt = startsAt, endsAt = endsAt)

internal fun rule(
    id: String,
    mode: UpdateMode = UpdateMode.RECOMMEND,
    platform: AppPlatform = AppPlatform.ANDROID,
    build: BuildCondition = BuildCondition.Lte(10),
    os: OsCondition? = null,
    title: String? = "업데이트 $id",
    image: PopupImage = PopupImage.None,
) = AppUpdateRule(id, mode, platform, build, os, title, body = null, image = image, imageAlt = null)
