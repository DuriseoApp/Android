package com.duriseo.android.feature.memo.pinned

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.memo.component.guardedWrite
import com.duriseo.android.feature.memo.component.toItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * SCR-MEMO-02 고정 메모 전체. 화면은 Room의 고정 목록만 구독하고, 화면이 보일 때 `GET /memos/pinned`를 다시 부른다
 * (실패·오프라인은 저장된 목록 그대로, 조용히 넘김). 핀을 누르면 고정을 푼다: 온라인에서만 하고 요청 중에는 로딩 덮개를 그린다
 * (FR-MEMO-05 AC1, D-115, NFR-OFF-02). 풀린 메모는 저장소가 고정 목록을 다시 불러오면서 목록에서 사라진다.
 */
@HiltViewModel
class PinnedMemosViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val writing = MutableStateFlow(false)

    val uiState: StateFlow<PinnedMemosUiState> = combine(
        meRepository.observeMe(),
        memoRepository.observePinned(),
        writing,
    ) { me, pinned, isWriting ->
        if (me == null) {
            PinnedMemosUiState(writing = isWriting)
        } else {
            PinnedMemosUiState(loaded = true, items = pinned.map { it.toItemUi(me, clock) }, writing = isWriting)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PinnedMemosUiState())

    private val effectChannel = Channel<PinnedMemosSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onAction(action: PinnedMemosAction) {
        when (action) {
            PinnedMemosAction.Shown -> viewModelScope.launch { refreshPinned() }
            PinnedMemosAction.BackClicked -> effectChannel.trySend(PinnedMemosSideEffect.Back)
            is PinnedMemosAction.ItemClicked -> effectChannel.trySend(PinnedMemosSideEffect.OpenDetail(action.item.id))
            is PinnedMemosAction.UnpinClicked -> unpin(action.item.id)
        }
    }

    private suspend fun refreshPinned() {
        memoRepository.refreshPinned()
    }

    private fun unpin(id: String) {
        if (writing.value) return
        writing.update { true }
        viewModelScope.launch {
            // 저장소가 예외를 던져도 덮개는 걷힌다 (예외는 공통 오류로, D-115)
            val result = try {
                guardedWrite { memoRepository.setPinned(id, pinned = false) }
            } finally {
                writing.update { false }
            }
            if (result is DataResult.Failure) {
                effectChannel.trySend(PinnedMemosSideEffect.ShowMessage(result.error.toMessageRes()))
            }
        }
    }
}
