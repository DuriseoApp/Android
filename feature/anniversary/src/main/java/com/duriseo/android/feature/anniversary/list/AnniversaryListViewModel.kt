package com.duriseo.android.feature.anniversary.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.anniversary.AnniversaryRepository
import com.duriseo.android.domain.anniversary.DayCount
import com.duriseo.android.domain.anniversary.PastAnniversaries
import com.duriseo.android.domain.anniversary.UpcomingAnniversaries
import com.duriseo.android.domain.couple.CoupleRepository
import com.duriseo.android.domain.couple.StartDateRules
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * SCR-ANNIV-01 기념일 목록. 화면은 Room만 구독한다 (NFR-OFF-01): 들어갈 때 첫 페이지부터 다시 불러오고(실패·오프라인이면
 * 저장된 목록 그대로, 조용히 넘김), 스크롤 끝에서 다음 페이지를 이어 붙인다. 같은 날 순서는 서버 응답 순서 그대로다 (Q-15).
 *
 * 처음 만난 날이 바뀌면(이 화면의 시트든 다른 곳이든) 자동 기념일이 달라지므로 목록·지난 기념일 개수·보이는 달을 다시 불러온다
 * (FR-ANNIV-02 AC3).
 */
@HiltViewModel
class AnniversaryListViewModel @Inject constructor(
    private val anniversaryRepository: AnniversaryRepository,
    private val meRepository: MeRepository,
    private val coupleRepository: CoupleRepository,
    private val clock: Clock,
) : ViewModel() {

    /** 화면 안에서만 들고 있는 값 */
    private data class Local(
        val pastOpen: Boolean = false,
        val startDateSheet: StartDateSheetState? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<AnniversaryListUiState> = combine(
        meRepository.observeMe(),
        anniversaryRepository.observeUpcoming(),
        anniversaryRepository.observePast(),
        local,
    ) { me, upcoming, past, state -> buildState(me, upcoming, past, state) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AnniversaryListUiState(today = LocalDate.now(clock)))

    private val effectChannel = Channel<AnniversaryListSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    /** 같은 목록의 "다음 페이지" 요청이 겹치지 않게 */
    private var loadingUpcoming = false
    private var loadingPast = false

    init {
        // 들어갈 때 첫 페이지부터 교체한다. 실패·오프라인은 저장된 목록을 그대로 보인다 (D-112와 같은 방식)
        viewModelScope.launch {
            anniversaryRepository.refreshUpcoming()
            anniversaryRepository.refreshPast()
        }
        // 처음 만난 날이 바뀌면 자동 기념일이 달라진다 (FR-ANNIV-02 AC3). 처음 값은 건너뛴다
        viewModelScope.launch {
            meRepository.observeMe()
                .map { it?.space?.startDate }
                .distinctUntilChanged()
                .drop(1)
                .collect { anniversaryRepository.refreshAll() }
        }
    }

    private fun buildState(
        me: Me?,
        upcoming: UpcomingAnniversaries,
        past: PastAnniversaries,
        state: Local,
    ): AnniversaryListUiState {
        val today = LocalDate.now(clock)
        if (me == null) return AnniversaryListUiState(today = today)
        val space = me.space
        val partner = space.partner
        val isCouple = space.type == SpaceType.COUPLE && partner != null
        val start = if (isCouple) space.startDate else null

        fun authorOf(authorId: String?): AnniversaryAuthor? = when (authorId) {
            null -> null
            me.id -> AnniversaryAuthor.ME
            else -> AnniversaryAuthor.PARTNER
        }

        return AnniversaryListUiState(
            loaded = true,
            isCouple = isCouple,
            together = if (start != null && partner != null) {
                TogetherCardUi(
                    dayNumber = DayCount.of(start, today),
                    startDate = start,
                    meColorKey = space.myColor,
                    partnerColorKey = partner.color,
                    togetherColorKey = space.togetherColor ?: "",
                )
            } else {
                null
            },
            items = upcoming.items.map { o ->
                val author = authorOf(o.authorId)
                AnniversaryItemUi(
                    key = "${o.date}-${o.anniversaryId ?: o.name}",
                    anniversaryId = o.anniversaryId,
                    name = o.name,
                    date = o.date,
                    daysLeft = o.daysLeft,
                    author = author,
                    repeatsYearly = o.repeatsYearly,
                    // 직접 기념일은 작성자만 수정·삭제한다. 자동과 상대 것은 누를 수 없다
                    editable = !o.isAuto && author == AnniversaryAuthor.ME,
                )
            }.withUniqueKeys({ it.key }) { item, key -> item.copy(key = key) },
            pastCount = past.totalCount,
            pastItems = past.items.map { p ->
                val author = authorOf(p.authorId) ?: AnniversaryAuthor.PARTNER
                PastItemUi(
                    key = p.anniversaryId,
                    anniversaryId = p.anniversaryId,
                    name = p.name,
                    date = p.date,
                    author = author,
                    editable = author == AnniversaryAuthor.ME,
                )
            }.withUniqueKeys({ it.key }) { item, key -> item.copy(key = key) },
            pastOpen = state.pastOpen,
            hasMoreUpcoming = upcoming.hasMore,
            hasMorePast = past.hasMore,
            startDateSheet = state.startDateSheet,
            today = today,
        )
    }

    fun onAction(action: AnniversaryListAction) {
        when (action) {
            AnniversaryListAction.BackClicked -> effectChannel.trySend(AnniversaryListSideEffect.Back)
            AnniversaryListAction.AddClicked -> effectChannel.trySend(AnniversaryListSideEffect.OpenCreate)
            is AnniversaryListAction.ItemClicked -> {
                val id = action.item.anniversaryId
                if (action.item.editable && id != null) {
                    effectChannel.trySend(AnniversaryListSideEffect.OpenEdit(id, action.item.date))
                }
            }
            is AnniversaryListAction.PastItemClicked ->
                if (action.item.editable) {
                    effectChannel.trySend(AnniversaryListSideEffect.OpenEdit(action.item.anniversaryId, action.item.date))
                }
            AnniversaryListAction.PastToggled -> local.update { it.copy(pastOpen = !it.pastOpen) }
            AnniversaryListAction.UpcomingEndReached -> loadMoreUpcoming()
            AnniversaryListAction.PastEndReached -> loadMorePast()

            AnniversaryListAction.StartDateInputClicked -> openStartDateSheet()
            AnniversaryListAction.StartDatePreviousMonth -> moveStartDateMonth(forward = false)
            AnniversaryListAction.StartDateNextMonth -> moveStartDateMonth(forward = true)
            is AnniversaryListAction.StartDateSelected -> selectStartDate(action.date)
            is AnniversaryListAction.StartDateMonthSelected -> selectStartMonth(action.month)
            AnniversaryListAction.StartDateSaved -> saveStartDate()
            AnniversaryListAction.StartDateSheetDismissed ->
                if (local.value.startDateSheet?.saving != true) local.update { it.copy(startDateSheet = null) }
        }
    }

    // --- 다음 페이지 ---

    private fun loadMoreUpcoming() {
        if (loadingUpcoming || !uiState.value.hasMoreUpcoming) return
        loadingUpcoming = true
        viewModelScope.launch {
            try {
                // 실패·오프라인은 조용히 넘긴다. 다시 끝까지 스크롤하면 다시 시도한다
                anniversaryRepository.loadMoreUpcoming()
            } finally {
                loadingUpcoming = false
            }
        }
    }

    private fun loadMorePast() {
        if (loadingPast || !uiState.value.hasMorePast) return
        loadingPast = true
        viewModelScope.launch {
            try {
                anniversaryRepository.loadMorePast()
            } finally {
                loadingPast = false
            }
        }
    }

    // --- 처음 만난 날 시트 (SCR-SET-01 소속 시트를 재사용, FR-ANNIV-04 AC4) ---

    private fun openStartDateSheet() {
        val state = uiState.value
        if (!state.isCouple) return
        val today = LocalDate.now(clock)
        local.update {
            it.copy(
                startDateSheet = StartDateSheetState(
                    visibleMonth = StartDateRules.initialMonth(state.together?.startDate, today),
                    selected = state.together?.startDate,
                ),
            )
        }
    }

    private fun moveStartDateMonth(forward: Boolean) {
        val today = LocalDate.now(clock)
        local.update { current ->
            val sheet = current.startDateSheet ?: return@update current
            val next = if (forward) {
                StartDateRules.nextMonth(sheet.visibleMonth, today)
            } else {
                StartDateRules.previousMonth(sheet.visibleMonth)
            }
            if (next == null) current else current.copy(startDateSheet = sheet.copy(visibleMonth = next))
        }
    }

    private fun selectStartMonth(month: YearMonth) {
        if (!StartDateRules.isMonthSelectable(month, LocalDate.now(clock))) return
        local.update { current ->
            val sheet = current.startDateSheet ?: return@update current
            if (sheet.saving) current else current.copy(startDateSheet = sheet.copy(visibleMonth = month))
        }
    }

    private fun selectStartDate(date: LocalDate) {
        if (!StartDateRules.isSelectable(date, LocalDate.now(clock))) return
        local.update { current ->
            val sheet = current.startDateSheet ?: return@update current
            if (sheet.saving) current else current.copy(startDateSheet = sheet.copy(selected = date))
        }
    }

    /** 시트의 [저장]: `PATCH /space`. 성공하면 시트를 닫는다. Room의 처음 만난 날이 바뀌면 목록을 다시 불러온다 (init의 구독) */
    private fun saveStartDate() {
        val sheet = local.value.startDateSheet ?: return
        val date = sheet.selected ?: return
        if (sheet.saving) return
        local.update { it.copy(startDateSheet = sheet.copy(saving = true)) }
        viewModelScope.launch {
            when (val result = coupleRepository.updateStartDate(date)) {
                is DataResult.Success -> local.update { it.copy(startDateSheet = null) }
                is DataResult.Failure -> {
                    local.update { it.copy(startDateSheet = sheet.copy(saving = false)) }
                    // 상대가 먼저 해제했으면(NOT_COUPLE) 앱 초기화가 이동을 맡는다 (D-103)
                    if (!result.error.hasCode(ErrorCode.NOT_COUPLE)) {
                        effectChannel.trySend(AnniversaryListSideEffect.ShowMessage(result.error.toMessageRes()))
                    }
                }
            }
        }
    }
}

/**
 * LazyColumn의 key는 겹치면 앱이 죽는다. 같은 날 같은 이름처럼 key가 겹치는 항목은 뒤에 `#순번`을 붙여 구분한다
 * (첫 항목의 key는 그대로라 목록이 바뀌어도 대부분 같은 key를 유지한다).
 */
private fun <T> List<T>.withUniqueKeys(keyOf: (T) -> String, withKey: (T, String) -> T): List<T> {
    val seen = mutableMapOf<String, Int>()
    return map { item ->
        val key = keyOf(item)
        val count = (seen[key] ?: 0) + 1
        seen[key] = count
        if (count == 1) item else withKey(item, "$key#$count")
    }
}
