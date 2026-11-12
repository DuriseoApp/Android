package com.duriseo.android.feature.memo.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.memo.component.guardedWrite
import com.duriseo.android.feature.memo.component.midnightTicks
import com.duriseo.android.feature.memo.component.toItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * SCR-MEMO-01 메모 목록. 화면은 Room의 고정 목록·보기 목록·개수·내 정보만 구독한다 (NFR-OFF-01).
 * 화면이 보일 때와 보기를 바꿀 때 첫 페이지를 다시 불러오고(실패·오프라인이면 저장된 목록 그대로, 조용히 넘김),
 * 스크롤 끝에서 다음 페이지를 이어 붙인다. 응답 순서는 저장소가 위치로 보존하므로 다시 정렬하지 않는다 (API.md 3.4).
 *
 * 핀·별 버튼은 온라인에서만 쓰고 결과를 기다리는 동안 로딩 덮개를 그린다 (D-115). 오프라인이면 저장소가 요청 없이
 * `Offline`을 돌려주므로 "인터넷 연결이 필요해요"를 보인다 (NFR-OFF-02). 누구든 아무 메모나 고정·해제할 수 있다 (FR-MEMO-05 AC1).
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MemoListViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    /** 화면 안에서만 들고 있는 값 */
    private data class Local(
        val view: MemoView = MemoView.NEWEST,
        val writing: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    /** 내 정보. 다음 자정마다 다시 내보내 작성일의 "오늘"·"어제"를 새로 계산하게 한다 (NFR-TIME-02) */
    private val meAtMidnight = combine(meRepository.observeMe(), midnightTicks(clock)) { me, _ -> me }

    val uiState: StateFlow<MemoListUiState> = combine(
        meAtMidnight,
        // 고정 목록과 "받은 적이 있는가". 받기 전의 빈 목록에는 "고정 없음" 점선 상자를 그리지 않는다
        combine(memoRepository.observePinned(), memoRepository.observePinnedLoaded()) { pinned, loaded -> pinned to loaded },
        local.map { it.view }.distinctUntilChanged().flatMapLatest { memoRepository.observeList(it) },
        memoRepository.observeFavoriteCount(),
        local,
    ) { me, (pinned, pinnedLoaded), list, favoriteCount, state ->
        if (me == null) {
            MemoListUiState(view = state.view, writing = state.writing)
        } else {
            MemoListUiState(
                loaded = true,
                pins = pinned.map { it.toItemUi(me, clock) },
                pinsLoaded = pinnedLoaded,
                view = state.view,
                items = list.items.map { it.toItemUi(me, clock) },
                totalCount = list.totalCount,
                favoriteCount = favoriteCount,
                hasMore = list.hasMore,
                listLoaded = list.isLoaded,
                writing = state.writing,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MemoListUiState())

    private val effectChannel = Channel<MemoListSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    /** "다음 페이지" 요청이 겹치지 않게 */
    private var loadingMore = false

    fun onAction(action: MemoListAction) {
        when (action) {
            MemoListAction.Shown -> refresh()
            is MemoListAction.ViewSelected -> selectView(action.view)
            is MemoListAction.ItemClicked -> effectChannel.trySend(MemoListSideEffect.OpenDetail(action.item.id))
            is MemoListAction.PinClicked -> write { memoRepository.setPinned(action.item.id, !action.item.isPinned) }
            is MemoListAction.FavoriteClicked -> write { memoRepository.setFavorite(action.item.id, !action.item.isFavorite) }
            MemoListAction.PinsMoreClicked -> effectChannel.trySend(MemoListSideEffect.OpenPinned)
            MemoListAction.AddClicked -> effectChannel.trySend(MemoListSideEffect.OpenCreate)
            MemoListAction.EndReached -> loadMore()
        }
    }

    /** 고정 목록과 지금 보기의 첫 페이지. 실패·오프라인은 저장된 목록을 그대로 보이고 안내하지 않는다 (D-112와 같은 방식) */
    private fun refresh() {
        val view = local.value.view
        viewModelScope.launch {
            memoRepository.refreshPinned()
            memoRepository.refreshList(view)
        }
    }

    private fun selectView(view: MemoView) {
        if (local.value.view == view) return
        local.update { it.copy(view = view) }
        viewModelScope.launch { memoRepository.refreshList(view) }
    }

    private fun loadMore() {
        val state = uiState.value
        if (loadingMore || !state.hasMore) return
        loadingMore = true
        viewModelScope.launch {
            try {
                // 실패·오프라인은 조용히 넘긴다. 다시 끝까지 스크롤하면 다시 시도한다
                memoRepository.loadMoreList(state.view)
            } finally {
                loadingMore = false
            }
        }
    }

    /** 핀·별 쓰기. 요청 중에는 로딩 덮개를 그리고 다른 쓰기를 받지 않는다. 결과 안내는 덮개를 걷은 뒤에 보낸다 (D-115) */
    private fun write(request: suspend () -> DataResult<Unit>) {
        if (local.value.writing) return
        local.update { it.copy(writing = true) }
        viewModelScope.launch {
            // 저장소가 예외를 던져도 덮개는 걷힌다 (예외는 공통 오류로, D-115)
            val result = try {
                guardedWrite(request)
            } finally {
                local.update { it.copy(writing = false) }
            }
            if (result is DataResult.Failure) {
                effectChannel.trySend(MemoListSideEffect.ShowMessage(result.error.toMessageRes()))
            }
        }
    }
}
