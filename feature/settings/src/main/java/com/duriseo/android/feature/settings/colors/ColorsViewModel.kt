package com.duriseo.android.feature.settings.colors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.MemberColor
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.me.SpaceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * SCR-SET-03 우리 색상 (FR-CAL-04). 화면은 Room의 내 정보만 구독하고, 사용자가 고른 색은 초안으로 따로 들고 있다.
 *
 * 저장 (D-100): 색을 누르면 미리보기만 바뀌고, [완료]에서 **바뀐 색만** 한 번에 `PATCH /space/colors`로 보낸다.
 * 성공하면 화면을 닫고, 바뀐 것이 없으면 요청 없이 닫는다. 실패·오프라인이면 안내만 하고 화면을 그대로 둔다.
 * 뒤로 가기는 저장하지 않고 초안을 버린다(묻지 않음). 서버가 `NOT_COUPLE`이라 하면 상대가 먼저 해제한 것이라
 * 앱 초기화(D-103)가 이동을 맡으므로 이 화면은 아무것도 보여 주지 않는다.
 */
@HiltViewModel
class ColorsViewModel @Inject constructor(
    private val meRepository: MeRepository,
    private val coupleRepository: CoupleRepository,
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
    private val saving = MutableStateFlow(false)

    private val effectChannel = Channel<ColorsSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val uiState: StateFlow<ColorsUiState> = combine(meRepository.observeMe(), draft, saving) { me, draft, saving ->
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
                saving = saving,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ColorsUiState())

    fun onAction(action: ColorsAction) {
        when (action) {
            is ColorsAction.TargetSelected -> {
                if (saving.value) return
                // 개인 공간은 [나]뿐이다 (FR-CAL-04 AC5)
                if (action.target != ColorTarget.Me && !uiState.value.isCouple) return
                draft.update { it.copy(target = action.target) }
            }
            is ColorsAction.ColorPicked -> {
                // 12색 밖의 키는 받지 않는다 (D-41)
                if (DuriseoPickPalette.colorOf(action.key) == null || saving.value) return
                draft.update { current ->
                    when (current.target) {
                        ColorTarget.Me -> current.copy(me = action.key)
                        ColorTarget.Partner -> current.copy(partner = action.key)
                        ColorTarget.Together -> current.copy(together = action.key)
                    }
                }
            }
            // 화면에서 보는 사람 기준으로 나 주황, 상대 파랑, 함께 보라 (D-101). 저장은 [완료]에서 한다
            ColorsAction.ResetClicked -> if (!saving.value) {
                draft.update {
                    it.copy(me = ColorDefaults.ME, partner = ColorDefaults.PARTNER, together = ColorDefaults.TOGETHER)
                }
            }
            ColorsAction.DoneClicked -> done()
        }
    }

    private fun done() {
        if (saving.value) return
        saving.value = true
        viewModelScope.launch {
            try {
                val me = meRepository.observeMe().first()
                val change = me?.let { changeOf(it, draft.value) } ?: ColorChange()
                if (change.isEmpty) {
                    // 바뀐 것이 없으면 요청하지 않고 닫는다 (D-100)
                    effectChannel.trySend(ColorsSideEffect.Close)
                    return@launch
                }
                when (val result = coupleRepository.updateColors(change)) {
                    is DataResult.Success -> effectChannel.trySend(ColorsSideEffect.Close)
                    is DataResult.Failure -> {
                        // 상대가 먼저 해제했으면(NOT_COUPLE) 앱 초기화가 이동을 맡는다 (D-103). 그 밖은 안내만 하고 화면을 그대로 둔다
                        if (!result.error.hasCode(ErrorCode.NOT_COUPLE)) {
                            effectChannel.trySend(ColorsSideEffect.ShowMessage(result.error.toMessageRes()))
                        }
                    }
                }
            } finally {
                saving.value = false
            }
        }
    }

    /** Room의 색과 다른 대상만 담는다. 개인 공간은 내 색만 (R-08: `together`나 자기 밖 `userId`는 거부된다) */
    private fun changeOf(me: Me, draft: Draft): ColorChange {
        val space = me.space
        val members = mutableListOf<MemberColor>()
        draft.me?.takeIf { it != space.myColor }?.let { members += MemberColor(me.id, it) }
        if (space.type != SpaceType.COUPLE) return ColorChange(members = members)
        val partner = space.partner
        if (partner != null) {
            draft.partner?.takeIf { it != partner.color }?.let { members += MemberColor(partner.id, it) }
        }
        val together = draft.together?.takeIf { it != space.togetherColor }
        return ColorChange(members = members, together = together)
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
