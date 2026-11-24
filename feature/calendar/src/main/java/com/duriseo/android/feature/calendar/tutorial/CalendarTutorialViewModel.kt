package com.duriseo.android.feature.calendar.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.common.Logger
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.tutorial.CalendarTutorialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * SCR-CAL-01 캘린더 튜토리얼 (FR-CAL-06, D-168, FR-SET-02 AC5, D-169). 앱 전체에서 하나(Activity 범위)라서 하단 탭까지 덮을 수 있다.
 *
 * 언제 띄우나:
 * - 이 기기에서 처음이면 캘린더 탭이 보이고 구멍 대상 위치를 모두 쟀고 다른 시트·다이얼로그가 없을 때 1단계부터 띄운다.
 *   **처음 띄울 때 본 것으로 저장한다** ([CalendarTutorialRepository.markShownIfFirst], 로그아웃·탈퇴·앱 초기화로 지우지 않는다).
 * - 전체 메뉴의 "튜토리얼 다시 보기" 요청이 있으면 같은 조건에서 저장 여부와 상관없이 1단계부터 띄운다.
 * - 캘린더 탭을 벗어나면 닫는다. 다른 시트가 뜨면 단계는 두고 가리기만 한다 ([CalendarTutorialUiState.visible]).
 *
 * 서버를 쓰지 않으므로 오프라인에서도 같다 (AC5).
 */
@HiltViewModel
class CalendarTutorialViewModel @Inject constructor(
    private val repository: CalendarTutorialRepository,
    meRepository: MeRepository,
) : ViewModel() {

    private data class Environment(
        val onCalendar: Boolean = false,
        val targetsReady: Boolean = false,
        val blocked: Boolean = false,
    )

    private val environment = MutableStateFlow(Environment())
    private val step = MutableStateFlow<CalendarTutorialStep?>(null)

    /** 이 프로세스에서 "처음인가"를 이미 물어봤다. 한 번 물으면 결과가 무엇이든 다시 묻지 않는다 */
    private var firstChecked = false

    val uiState: StateFlow<CalendarTutorialUiState> = combine(
        step,
        environment,
        meRepository.observeMe(),
    ) { step, environment, me ->
        CalendarTutorialUiState(
            step = step,
            blocked = environment.blocked,
            solo = me?.space?.type == SpaceType.PERSONAL,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CalendarTutorialUiState())

    init {
        viewModelScope.launch {
            combine(environment, repository.replayRequested) { environment, replay -> environment to replay }
                .collect { (environment, replay) -> evaluate(environment, replay) }
        }
    }

    fun onAction(action: CalendarTutorialAction) {
        when (action) {
            is CalendarTutorialAction.EnvironmentChanged -> environment.value = Environment(
                onCalendar = action.onCalendar,
                targetsReady = action.targetsReady,
                blocked = action.blocked,
            )
            CalendarTutorialAction.NextClicked -> step.update { it?.next() }
            CalendarTutorialAction.SkipClicked -> step.value = null
        }
    }

    private suspend fun evaluate(environment: Environment, replay: Boolean) {
        if (!environment.onCalendar) {
            // 캘린더를 벗어났다(푸시 진입 등). 요청이 남아 있으면 캘린더로 돌아올 때 띄운다
            step.value = null
            return
        }
        // 이미 뜬 단계는 위치를 잠시 잃어도(회전, 탭 전환) 그대로 두고, 레이어가 다시 잴 때까지 그리지 않는다.
        // 튜토리얼이 떠 있는 동안에는 전체 메뉴를 열 수 없어 다시 보기 요청이 새로 남지 않는다고 본다. 혹시 남아 있으면
        // 닫은 뒤 조건이 다시 바뀔 때(탭 전환·시트 닫힘 등) 1단계부터 뜬다
        if (step.value != null || !environment.targetsReady || environment.blocked) return
        if (replay) {
            repository.consumeReplay()
            step.value = CalendarTutorialStep.first
            return
        }
        if (firstChecked) return
        firstChecked = true
        // 저장에 실패(IOException)해도 튜토리얼은 보여 준다. 다음 실행에 다시 뜰 수 있을 뿐이다
        val isFirst = try {
            repository.markShownIfFirst()
        } catch (e: IOException) {
            Logger.w(TAG, "튜토리얼 본 기록을 저장하지 못했어요", e)
            true
        }
        if (isFirst) step.value = CalendarTutorialStep.first
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TAG = "CalendarTutorial"
    }
}
