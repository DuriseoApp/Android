package com.duriseo.android.domain.appops

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.LocalDate

/**
 * 두 곳의 값 → 지금 띄울 팝업 하나 → (필요하면) 이미지 미리 받기까지 묶는다 (D-178, D-179, Q-23, FR-APP-01·02).
 * 화면(`feature:appops`)이 확인 시점(실행·복귀·Remote Config 변경·426)마다 부른다.
 *
 * 이미지 기다리는 시간 (Q-23): 앱 실행 때는 확인을 시작한 때부터 합쳐 [LAUNCH_BUDGET_MILLIS], 그 밖은 이미지만 [IMAGE_BUDGET_MILLIS].
 * 못 받으면 이미지를 [PopupImage.None]으로 바꿔 돌려준다(이미지만 넣은 팝업은 화면이 `imageAlt`를 제목 자리에 쓴다).
 *
 * @param uptimeMillis 단조 증가 시계(밀리초). 테스트는 가상 시간을 넣는다
 */
class AppPopupCoordinator(
    private val repository: AppOpsRepository,
    private val hiddenStore: PopupHiddenTodayStore,
    private val forceSignals: ForceUpdateSignals,
    private val prefetcher: PopupImagePrefetcher,
    private val environment: AppEnvironment,
    private val clock: Clock,
    private val uptimeMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    /**
     * 확인 한 번. 띄울 팝업이 없으면 null.
     *
     * @param isLaunch 앱 실행 때(Activity 생성)인지. 돌아올 때는 false
     */
    suspend fun check(isLaunch: Boolean): AppPopup? {
        val start = uptimeMillis()
        val snapshot = repository.check()
        val budget = if (isLaunch) LAUNCH_BUDGET_MILLIS - (uptimeMillis() - start) else IMAGE_BUDGET_MILLIS
        return present(snapshot, budget)
    }

    /** Remote Config 값이 바뀔 때마다 다시 판단한 결과 (이미지는 [IMAGE_BUDGET_MILLIS]까지) */
    val changes: Flow<AppPopup?>
        get() = repository.changes.map { present(it, IMAGE_BUDGET_MILLIS) }

    /** 서버가 426을 줄 때마다 그 강제 업데이트 팝업. 프로세스가 사는 동안 마지막 신호가 유지돼 늦게 구독해도 받는다 */
    val forceUpdates: Flow<AppPopup>
        get() = forceSignals.current.filterNotNull().map { signal ->
            withImageReady(signal.toPopup(), IMAGE_BUDGET_MILLIS)
        }

    /** [오늘 하루 안 보기]. 숨길 수 없는 종류나 `id`가 없는 팝업이면 아무것도 하지 않는다 */
    suspend fun hideToday(popup: AppPopup) {
        val id = popup.id ?: return
        if (!popup.kind.canHideToday) return
        hiddenStore.hideToday(popup.kind.idSpace, id, today())
    }

    private suspend fun present(snapshot: AppOpsSnapshot, imageBudgetMillis: Long): AppPopup? {
        val now = clock.instant()
        val hidden = hiddenStore.hiddenIds(today())
        var forced = forceSignals.current.value
        if (forced != null && shouldCloseForced(forced, snapshot, now, hidden)) {
            forceSignals.clear()
            forced = null
        }
        val popup = AppPopupResolver.resolve(snapshot.remote, snapshot.server, environment, now, hidden, forced)
            ?: return null
        return withImageReady(popup, imageBudgetMillis)
    }

    /**
     * 426으로 띄운 강제 팝업은 서버 목록을 받았는데 그 `ruleId` 규칙이 없고 다른 강제 규칙도 없을 때만 닫는다 (unit-9 결정 2).
     * 서버 값을 못 받았으면(실패·오프라인) 닫지 않는다.
     */
    private fun shouldCloseForced(
        forced: ForceUpdateSignal,
        snapshot: AppOpsSnapshot,
        now: java.time.Instant,
        hidden: HiddenTodayIds,
    ): Boolean {
        val server = snapshot.server ?: return false
        if (forced.ruleId != null && server.rules.any { it.id == forced.ruleId }) return false
        val others = AppPopupResolver.resolveAll(snapshot.remote, server, environment, now, hidden, forced = null)
        return others.none { it.kind == PopupKind.FORCE_UPDATE }
    }

    /** `https://` 이미지를 [budgetMillis] 안에 받으면 그대로, 못 받으면 이미지 없는 팝업으로 돌려준다 */
    private suspend fun withImageReady(popup: AppPopup, budgetMillis: Long): AppPopup {
        val image = popup.image as? PopupImage.Web ?: return popup
        val received = budgetMillis > 0 &&
            withTimeoutOrNull(budgetMillis) { prefetcher.prefetch(image.url) } == true
        return if (received) popup else popup.copy(image = PopupImage.None)
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    companion object {
        /** 서버·Remote Config를 기다리는 시간(3초) (API.md 4.9 2단계, unit-9 결정 1) */
        const val SERVER_WAIT_MILLIS = 3_000L

        /** 앱 실행 때 확인을 시작한 때부터 이미지까지 합쳐 기다리는 시간 (Q-23) */
        const val LAUNCH_BUDGET_MILLIS = 3_000L

        /** 그 밖의 때 이미지만 기다리는 시간 (Q-23) */
        const val IMAGE_BUDGET_MILLIS = 3_000L
    }
}
