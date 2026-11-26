package com.duriseo.android.feature.appops

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import com.duriseo.android.core.common.image.PopupImageLoader
import com.duriseo.android.core.common.update.AppForegroundEvents
import com.duriseo.android.core.common.update.AppUpdateSource
import com.duriseo.android.core.common.update.ImmediateUpdateResult
import com.duriseo.android.domain.appops.AppPopup
import com.duriseo.android.domain.appops.AppPopupCoordinator
import com.duriseo.android.domain.appops.PopupKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * 앱 전체 공지·업데이트 팝업 (SCR-APP-01·02, FR-APP-01·02, D-178·D-179). 판단은 [AppPopupCoordinator]가 하고 이 ViewModel은
 * 확인 시점과 버튼 동작을 이어 준다.
 *
 * 확인 시점 (D-178): 만들어질 때(Activity 생성 = 앱 실행, 화면 회전에는 ViewModel이 남아 다시 하지 않는다), 앱으로 돌아올 때
 * ([AppForegroundEvents], 회전은 돌아옴이 아니다), Remote Config 값이 바뀔 때, 서버가 426을 줄 때.
 * 새로 고른 결과가 지금 팝업과 다르면 바꾸고(앞 순서가 생기면 바뀜), 없으면 닫는다(떠 있던 공지가 내려감).
 */
@HiltViewModel
class AppPopupViewModel @Inject constructor(
    private val coordinator: AppPopupCoordinator,
    private val updateSource: AppUpdateSource,
    foreground: AppForegroundEvents,
    @PopupImageLoader val imageLoader: ImageLoader,
) : ViewModel() {

    private val mutableState = MutableStateFlow(AppPopupUiState())
    val uiState: StateFlow<AppPopupUiState> = mutableState.asStateFlow()

    private val effects = Channel<AppPopupSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<AppPopupSideEffect> = effects.receiveAsFlow()

    /** 지금 도는 실행·돌아올 때 확인. 도는 중에 돌아옴 신호가 또 오면 그 확인이 대신한다 */
    private var checkJob: Job? = null

    /**
     * 팝업의 링크 버튼으로 앱을 나갔다 돌아오는 한 번은 "앱으로 돌아옴" 확인으로 세지 않는다: 팝업이 그대로 보여야 한다
     * (SCREENS 560행, unit-9 결정 17). 돌아오면 바로 꺼진다.
     */
    private var skipNextReturn = false

    /** [업데이트]를 눌러 Play 화면이 떠 있는 동안 */
    private var updating = false

    /**
     * 결과 순번 (메인 스레드에서만 만지므로 잠금이 필요 없다). 실행·돌아올 때 확인은 **시작할 때**, Remote Config 변경·426·다음 팝업 고르기는
     * 결과가 **도착할 때** 번호를 받는다. 이미 나중 번호의 결과를 보인 뒤에 도착한 이전 번호의 결과는 버린다:
     * 이미지를 받는 동안 늦게 끝난 확인이 그 사이에 생긴 앞 순서 팝업(특히 426 강제)을 덮지 않는다.
     */
    private var generation = 0L
    private var appliedGeneration = 0L

    init {
        check(isLaunch = true)
        viewModelScope.launch { coordinator.changes.collect { apply(++generation, it) } }
        viewModelScope.launch { coordinator.forceUpdates.collect { apply(++generation, it) } }
        viewModelScope.launch {
            foreground.returns.collect {
                if (skipNextReturn) {
                    skipNextReturn = false
                } else if (checkJob?.isActive != true) {
                    check(isLaunch = false)
                }
            }
        }
    }

    fun onAction(action: AppPopupAction) {
        val popup = mutableState.value.popup ?: return
        when (action) {
            AppPopupAction.Dismiss -> dismiss(popup)
            AppPopupAction.Back -> if (popup.kind.canHideToday) dismiss(popup)
            AppPopupAction.HideToday -> hideToday(popup)
            AppPopupAction.Exit -> effects.trySend(AppPopupSideEffect.FinishApp)
            AppPopupAction.OpenLink -> popup.link?.let {
                skipNextReturn = true
                effects.trySend(AppPopupSideEffect.OpenUrl(it.url))
            }
            is AppPopupAction.Update -> startUpdate(action.activity)
        }
    }

    /** 링크를 열 앱이 없어 나가지 못했다: 돌아옴이 없으니 건너뛰기를 거둔다 */
    fun onLinkOpenFailed() {
        skipNextReturn = false
    }

    private fun check(isLaunch: Boolean) {
        val gen = ++generation
        // 지금 떠 있는 팝업을 알려 준다: 서버를 못 받은 확인이 서버에만 있던 떠 있는 팝업을 바꾸지 않게 (결정 18)
        val current = mutableState.value.popup
        checkJob = viewModelScope.launch { apply(gen, coordinator.check(isLaunch, current)) }
    }

    /** 먼저 지금 팝업을 내리고(다음 팝업 이미지를 받는 동안 기다리지 않는다, 버튼 중복 누름도 막힌다) 다음 것은 받은 뒤 보인다 */
    private fun dismiss(popup: AppPopup) {
        coordinator.dismissForNow(popup)
        closeThenShowNext()
    }

    private fun hideToday(popup: AppPopup) {
        if (!popup.kind.canHideToday) return
        mutableState.value = AppPopupUiState()
        viewModelScope.launch {
            coordinator.hideToday(popup)
            apply(++generation, coordinator.next())
        }
    }

    private fun closeThenShowNext() {
        mutableState.value = AppPopupUiState()
        viewModelScope.launch { apply(++generation, coordinator.next()) }
    }

    private fun startUpdate(activity: Activity) {
        if (updating) return
        updating = true
        // 오래 떠 있는 코루틴이 Activity를 붙들지 않게 약한 참조로 넘긴다
        val ref = WeakReference(activity)
        viewModelScope.launch {
            try {
                val target = ref.get()
                val result = if (target == null) ImmediateUpdateResult.CANCELED else updateSource.startImmediateUpdate(target)
                // 취소는 팝업을 그대로 둔다(다시 보인다). 쓸 수 없으면 스토어 페이지를 연다
                if (result == ImmediateUpdateResult.UNAVAILABLE) {
                    effects.trySend(AppPopupSideEffect.OpenStore(PlayStoreLinks.MARKET_URI, PlayStoreLinks.WEB_URL))
                }
            } finally {
                updating = false
            }
        }
    }

    /**
     * 결과를 보인다. 이미 나중 번호의 결과를 보였으면 버리고, 426 강제 팝업이 떠 있고 신호가 남아 있으면 강제가 아닌 결과로 바꾸지 않는다.
     */
    private fun apply(gen: Long, popup: AppPopup?) {
        if (gen < appliedGeneration) return
        val current = mutableState.value.popup
        if (current?.kind == PopupKind.FORCE_UPDATE && coordinator.hasForceSignal && popup?.kind != PopupKind.FORCE_UPDATE) return
        appliedGeneration = gen
        mutableState.value = AppPopupUiState(popup)
    }
}
