package com.duriseo.android.domain.appops

import java.time.Instant

/** 오늘 숨긴 `id` (기기 시간대의 오늘 날짜로 기억한 것, D-179) */
data class HiddenTodayIds(
    val notices: Set<String> = emptySet(),
    val rules: Set<String> = emptySet(),
) {
    fun contains(space: PopupIdSpace, id: String?): Boolean = id != null && when (space) {
        PopupIdSpace.NOTICE -> id in notices
        PopupIdSpace.UPDATE_RULE -> id in rules
    }

    companion object {
        val None = HiddenTodayIds()
    }
}

/**
 * 어느 팝업을 띄울지 정하는 순수 함수 모음 (FR-APP-01 AC5, FR-APP-02 AC5, D-178, D-179, API.md 4.9 3~5단계).
 */
object AppPopupResolver {

    /**
     * 두 곳의 목록을 합친다. 같은 `id`는 하나로 보고 [remote](Remote Config) 값을 쓴다. 순서는 Remote Config 목록 순서,
     * 그다음 서버에만 있는 것의 서버 목록 순서다. 한 목록 안에서 `id`가 겹치면 앞의 것만 쓴다.
     */
    fun <T> merge(remote: List<T>, server: List<T>, idOf: (T) -> String): List<T> {
        val seen = HashSet<String>()
        val result = ArrayList<T>(remote.size + server.size)
        (remote.asSequence() + server.asSequence()).forEach { item ->
            if (seen.add(idOf(item))) result += item
        }
        return result
    }

    /**
     * 지금 띄울 수 있는 팝업을 표시 순서대로 돌려준다. 첫 번째가 보일 것이다.
     *
     * - 공지는 기간·플랫폼으로, 규칙은 [env]에 맞는 것만 남긴다. 합친 뒤에 거른다(같은 `id`는 Remote Config 값이 이긴다).
     * - [hidden]에 있는 `id`는 건너뛴다. 단 강제 업데이트와 앱을 막는 공지는 숨기지 않는다 (D-179, FR-APP-02 AC4).
     * - 종류별 순서는 [PopupKind] 선언 순서, 같은 종류끼리는 Remote Config 목록 → 서버 목록 순서(안정 정렬).
     * - [forced]는 서버가 426으로 알려 준 강제 업데이트다. 같은 규칙이 목록에도 있으면 하나로 본다.
     */
    fun resolveAll(
        remote: AppOpsData,
        server: AppOpsData?,
        env: AppEnvironment,
        now: Instant,
        hidden: HiddenTodayIds = HiddenTodayIds.None,
        forced: ForceUpdateSignal? = null,
    ): List<AppPopup> {
        val serverData = server ?: AppOpsData.Empty
        val notices = merge(remote.notices, serverData.notices) { it.id }.filterActive(now, env.platform)
        val rules = merge(remote.rules, serverData.rules) { it.id }.filter { it.matches(env) }

        val candidates = ArrayList<AppPopup>()
        if (forced != null) candidates += forced.toPopup()
        rules.forEach { candidates += it.toPopup() }
        notices.forEach { candidates += it.toPopup() }

        val visible = candidates.filterIndexed { index, popup ->
            val duplicateOfForced = forced != null && index > 0 && popup.kind == PopupKind.FORCE_UPDATE &&
                forced.ruleId != null && popup.id == forced.ruleId
            !duplicateOfForced && !(popup.kind.canHideToday && hidden.contains(popup.kind.idSpace, popup.id))
        }
        // sortedBy는 안정 정렬이라 같은 종류는 위에서 쌓은 순서(426 → Remote Config → 서버)를 지킨다
        return visible.sortedBy { it.kind.ordinal }
    }

    /** [resolveAll]의 첫 번째. 없으면 null */
    fun resolve(
        remote: AppOpsData,
        server: AppOpsData?,
        env: AppEnvironment,
        now: Instant,
        hidden: HiddenTodayIds = HiddenTodayIds.None,
        forced: ForceUpdateSignal? = null,
    ): AppPopup? = resolveAll(remote, server, env, now, hidden, forced).firstOrNull()
}
