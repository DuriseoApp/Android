package com.duriseo.android.domain.appops

import java.time.Instant

/** 공지가 지금 이 앱에 유효한가: 기간과 플랫폼 (FR-APP-01 AC1, API.md 3.7 "지금 유효함") */
fun AppNotice.isActive(now: Instant, platform: AppPlatform): Boolean {
    val started = startsAt == null || !startsAt.isAfter(now)
    val notEnded = endsAt == null || now.isBefore(endsAt)
    val platformOk = this.platform == null || this.platform == platform
    return started && notEnded && platformOk
}

fun List<AppNotice>.filterActive(now: Instant, platform: AppPlatform): List<AppNotice> =
    filter { it.isActive(now, platform) }

/** 규칙이 이 앱에 맞는가: 플랫폼, 빌드, OS 버전을 모두 만족 (API.md 3.7 "업데이트 규칙 맞춤 판단", 서버 R-20과 같은 식) */
fun AppUpdateRule.matches(env: AppEnvironment): Boolean =
    platform == env.platform && build.matches(env.build) && (os?.matches(env.osVersion) ?: true)

fun BuildCondition.matches(build: Long): Boolean = when (this) {
    is BuildCondition.Eq -> build == value
    is BuildCondition.Lte -> build <= value
    is BuildCondition.Gte -> build >= value
    is BuildCondition.Range -> build in min..max
    is BuildCondition.In -> build in values
}

fun OsCondition.matches(osVersion: Int): Boolean =
    (min == null || osVersion >= min) && (max == null || osVersion <= max)
