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

    /** [업데이트]를 눌러 Play 화면이 떠 있는 동안 */
    private var updating = false

    init {
        check(isLaunch = true)
        viewModelScope.launch { coordinator.changes.collect { show(it) } }
        viewModelScope.launch { coordinator.forceUpdates.collect { show(it) } }
        viewModelScope.launch {
            foreground.returns.collect {
                if (checkJob?.isActive != true) check(isLaunch = false)
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
            AppPopupAction.OpenLink -> popup.link?.let { effects.trySend(AppPopupSideEffect.OpenUrl(it.url)) }
            is AppPopupAction.Update -> startUpdate(action.activity)
        }
    }

    private fun check(isLaunch: Boolean) {
        checkJob = viewModelScope.launch { show(coordinator.check(isLaunch)) }
    }

    private fun dismiss(popup: AppPopup) {
        coordinator.dismissForNow(popup)
        viewModelScope.launch { show(coordinator.next()) }
    }

    private fun hideToday(popup: AppPopup) {
        if (!popup.kind.canHideToday) return
        viewModelScope.launch {
            coordinator.hideToday(popup)
            show(coordinator.next())
        }
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

    private fun show(popup: AppPopup?) {
        mutableState.value = AppPopupUiState(popup)
    }
}
