package com.duriseo.android.feature.settings.colors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * SCR-SET-03 우리 색상 (FR-CAL-04). 화면은 Room의 내 정보만 구독하고, 사용자가 고른 색은 초안으로 따로 들고 있다.
 * 고른 색을 서버로 보내는 시점은 v0.9 반영 대기다 (unit-3.md 확인 필요 10).
 */
@HiltViewModel
class ColorsViewModel @Inject constructor(
    meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    /** 사용자가 고른 색. 고르지 않은 대상은 null이고 Room 값을 그대로 쓴다 */
    private data class Draft(
        val target: ColorTarget = ColorTarget.Me,
        val me: String? = null,
        val partner: String? = null,
        val together: String? = null,
    )

    private val draft = MutableStateFlow(Draft())

    val uiState: StateFlow<ColorsUiState> = combine(meRepository.observeMe(), draft) { me, draft ->
        if (me == null) {
            ColorsUiState()
        } else {
            val space = me.space
            val isCouple = space.type == SpaceType.COUPLE
            val today = LocalDate.now(clock)
            ColorsUiState(
                loaded = true,
                isCouple = isCouple,
                myColor = draft.me ?: space.myColor,
                partnerColor = draft.partner ?: space.partner?.color ?: ColorDefaults.PARTNER,
                togetherColor = draft.together ?: space.togetherColor ?: ColorDefaults.TOGETHER,
                target = if (isCouple) draft.target else ColorTarget.Me,
                today = today,
                weekDays = weekOf(today),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ColorsUiState())

    fun onAction(action: ColorsAction) {
        when (action) {
            is ColorsAction.TargetSelected -> {
                // 개인 공간은 [나]뿐이다 (FR-CAL-04 AC5)
                if (action.target != ColorTarget.Me && !uiState.value.isCouple) return
                draft.update { it.copy(target = action.target) }
            }
            is ColorsAction.ColorPicked -> {
                // 12색 밖의 키는 받지 않는다 (D-41)
                if (DuriseoPickPalette.colorOf(action.key) == null) return
                draft.update { current ->
                    when (current.target) {
                        ColorTarget.Me -> current.copy(me = action.key)
                        ColorTarget.Partner -> current.copy(partner = action.key)
                        ColorTarget.Together -> current.copy(together = action.key)
                    }
                }
            }
            ColorsAction.ResetClicked -> draft.update {
                it.copy(me = ColorDefaults.ME, partner = ColorDefaults.PARTNER, together = ColorDefaults.TOGETHER)
            }
            // TODO(v0.9 반영 대기): [완료]가 바뀐 색만 `CoupleRepository.updateColors`로 보내고 닫는 동작 (확인 필요 10)
            ColorsAction.DoneClicked -> Unit
        }
    }

    /** 오늘이 든 한 주, 일요일부터 토요일까지 (보드 `ColorSettings.dc.html`의 미리보기 한 주) */
    private fun weekOf(today: LocalDate): List<LocalDate> {
        // dayOfWeek.value: 월 1 ... 일 7. 일요일이면 0일을 뺀다
        val sunday = today.minusDays((today.dayOfWeek.value % DAYS_PER_WEEK).toLong())
        return (0 until DAYS_PER_WEEK).map { sunday.plusDays(it.toLong()) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DAYS_PER_WEEK = 7
    }
}
