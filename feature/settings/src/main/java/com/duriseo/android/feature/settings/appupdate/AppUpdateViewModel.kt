package com.duriseo.android.feature.settings.appupdate

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.common.update.AppUpdateAvailability
import com.duriseo.android.core.common.update.AppUpdatePhase
import com.duriseo.android.core.common.update.AppUpdateSource
import com.duriseo.android.core.common.update.AppVersion
import com.duriseo.android.domain.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SCR-SET-01 앱 정보 카드 (FR-SET-02 AC4, D-151). 전체 메뉴에 들어오면 새 버전이 있는지 확인한다.
 * 오프라인이면 Play에 묻지 않고 "확인할 수 없음"이다 (확인하지 못했는데 최신이라고 안내하지 않는다).
 * [업데이트]는 앱을 쓰는 중에 받는 방식(FLEXIBLE)이고 받는 동안은 "확인 중"과 같은 모양이다 (단위 8 확인 필요 5).
 */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val source: AppUpdateSource,
    private val networkMonitor: NetworkMonitor,
    private val version: AppVersion,
) : ViewModel() {

    /** null이면 확인 중 */
    private val availability = MutableStateFlow<AppUpdateAvailability?>(null)

    /** [업데이트]를 눌러 Play의 확인 창이 떠 있는 동안 */
    private val starting = MutableStateFlow(false)

    val uiState: StateFlow<AppUpdateUiState> = combine(availability, starting, source.phase) { available, isStarting, phase ->
        val status = when {
            isStarting || phase != AppUpdatePhase.IDLE -> AppUpdateStatus.DOWNLOADING
            available == null -> AppUpdateStatus.CHECKING
            available == AppUpdateAvailability.UP_TO_DATE -> AppUpdateStatus.UP_TO_DATE
            available == AppUpdateAvailability.AVAILABLE -> AppUpdateStatus.UPDATE_AVAILABLE
            else -> AppUpdateStatus.UNKNOWN
        }
        AppUpdateUiState(versionName = version.name, versionCode = version.code, status = status)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        AppUpdateUiState(versionName = version.name, versionCode = version.code),
    )

    init {
        viewModelScope.launch {
            availability.value = if (networkMonitor.isOnlineNow()) source.check() else AppUpdateAvailability.UNKNOWN
        }
    }

    /** [업데이트]. Play가 받기 확인 창을 띄운다. 취소하거나 실패하면 [업데이트]가 다시 보인다 */
    fun onUpdateClicked(activity: Activity) {
        if (availability.value != AppUpdateAvailability.AVAILABLE || starting.value) return
        starting.value = true
        viewModelScope.launch {
            try {
                source.startUpdate(activity)
            } finally {
                starting.value = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
