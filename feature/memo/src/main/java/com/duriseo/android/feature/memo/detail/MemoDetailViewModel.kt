package com.duriseo.android.feature.memo.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duriseo.android.core.common.navigation.MemoRouteModel
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.format.DateTexts
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.memo.Memo
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.memo.component.MemoAuthor
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
import java.time.LocalDate
import javax.inject.Inject
import com.duriseo.android.core.designsystem.R as DesignR

/**
 * SCR-MEMO-03 메모 상세. 화면은 Room의 메모와 내 정보만 구독한다 (NFR-OFF-01). 수정·삭제는 내 메모만, 고정·즐겨찾기는 누구 메모든 한다 (FR-MEMO-03, FR-MEMO-05 AC1, FR-MEMO-06).
 * 쓰기는 온라인에서만 하고 요청 중에는 로딩 덮개를 그린다 (D-115, NFR-OFF-02). 서버가 성공해야 Room이 바뀐다 (낙관적 갱신 없음).
 *
 * 서버 오류 (D-121, FR-MEMO-03 AC3): 메모가 없다고 하면(404 `NOT_FOUND`) 저장소가 기기에서 지우고, 이 화면은 공통 오류를 보이고 닫는다.
 * 권한이 없다고 하면(403 `NOT_AUTHOR`) 저장소가 목록과 이 메모를 다시 불러오므로 이 화면은 공통 오류만 보이고 남는다. 새 문구는 없다.
 */
@HiltViewModel
class MemoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    meRepository: MeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val memoId: String = savedStateHandle.toRoute<MemoRouteModel.Detail>().memoId

    private data class Local(
        val deleteDialogVisible: Boolean = false,
        val deleting: Boolean = false,
        val deleteLoading: Boolean = false,
        val writing: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    /** 한 번이라도 메모를 읽었는가. 그 뒤에 사라지면(상대가 지움 등) 화면을 닫는다 */
    private var everLoaded = false
    private var vanished = false

    val uiState: StateFlow<MemoDetailUiState> = combine(
        memoRepository.observeMemo(memoId),
        meRepository.observeMe(),
        local,
    ) { memo, me, local ->
        val base = MemoDetailUiState(
            deleteDialogVisible = local.deleteDialogVisible,
            deleting = local.deleting,
            deleteLoading = local.deleteLoading,
            writing = local.writing,
        )
        if (memo == null || me == null) {
            if (memo == null && everLoaded && !local.deleting) onVanished()
            base
        } else {
            everLoaded = true
            base.copy(memo = memo.toDetail(me))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MemoDetailUiState())

    private val effectChannel = Channel<MemoDetailSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onAction(action: MemoDetailAction) {
        when (action) {
            MemoDetailAction.Shown -> viewModelScope.launch {
                val result = memoRepository.refreshMemo(memoId)
                // 404면 저장소가 Room에서 지웠다. 이미 읽은 적이 있으면 위 combine이 닫지만, 없어도 닫히도록 여기서도 처리한다 (한 번만)
                if (result is DataResult.Failure && result.error.hasCode(ErrorCode.NOT_FOUND)) onVanished()
            }
            MemoDetailAction.BackClicked -> effectChannel.trySend(MemoDetailSideEffect.Close)
            MemoDetailAction.EditClicked ->
                if (uiState.value.memo?.canEdit == true) effectChannel.trySend(MemoDetailSideEffect.OpenEdit(memoId))
            MemoDetailAction.PinClicked -> uiState.value.memo?.let { memo -> write { memoRepository.setPinned(memoId, !memo.isPinned) } }
            MemoDetailAction.FavoriteClicked ->
                uiState.value.memo?.let { memo -> write { memoRepository.setFavorite(memoId, !memo.isFavorite) } }
            MemoDetailAction.DeleteClicked ->
                if (uiState.value.memo?.canEdit == true) local.update { it.copy(deleteDialogVisible = true) }
            MemoDetailAction.DeleteCancelled -> local.update { if (it.deleting) it else it.copy(deleteDialogVisible = false) }
            MemoDetailAction.DeleteConfirmed -> delete()
        }
    }

    private fun onVanished() {
        // 한 번만 알린다 (같은 메모가 사라진 채 내 정보가 다시 오는 경우)
        if (vanished) return
        vanished = true
        effectChannel.trySend(MemoDetailSideEffect.ShowMessage(DesignR.string.error_common))
        effectChannel.trySend(MemoDetailSideEffect.Close)
    }

    /**
     * 쓰기 실패 안내. 메모가 없다고 하면(404) [onVanished]가 닫으면서 공통 오류를 보이므로 여기서는 또 보이지 않는다.
     * 그 밖(권한 없음 403 포함, 오프라인)은 문구만 보이고 화면은 그대로다.
     */
    private fun showFailure(result: DataResult.Failure) {
        if (result.error.hasCode(ErrorCode.NOT_FOUND)) {
            onVanished()
        } else {
            effectChannel.trySend(MemoDetailSideEffect.ShowMessage(result.error.toMessageRes()))
        }
    }

    private fun write(request: suspend () -> DataResult<Unit>) {
        if (local.value.writing) return
        local.update { it.copy(writing = true) }
        viewModelScope.launch {
            val result = request()
            local.update { it.copy(writing = false) }
            if (result is DataResult.Failure) showFailure(result)
        }
    }

    private fun delete() {
        if (local.value.deleting || uiState.value.memo?.canEdit != true) return
        local.update { it.copy(deleting = true, deleteLoading = true) }
        viewModelScope.launch {
            when (val result = memoRepository.delete(memoId)) {
                is DataResult.Success -> {
                    local.update { it.copy(deleteDialogVisible = false, deleteLoading = false) }
                    effectChannel.trySend(MemoDetailSideEffect.Close)
                }
                is DataResult.Failure -> {
                    local.update { it.copy(deleting = false, deleteLoading = false, deleteDialogVisible = false) }
                    showFailure(result)
                }
            }
        }
    }

    private fun Memo.toDetail(me: Me): MemoDetailUi {
        val mine = authorId == me.id
        return MemoDetailUi(
            id = id,
            title = title,
            body = body,
            author = if (mine) MemoAuthor.ME else MemoAuthor.PARTNER,
            nickname = if (mine) me.nickname else me.space.partner?.nickname.orEmpty(),
            writtenAt = DateTexts.writtenAt(createdAt, clock),
            memoDate = memoDate?.let { DateTexts.memoDateChip(it, LocalDate.now(clock)) },
            isPinned = isPinned,
            isFavorite = isFavorite,
            canEdit = mine,
        )
    }
}
