package com.duriseo.android.feature.memo.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.message.toMessageRes
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.memo.Memo
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.memo.MemoRules
import com.duriseo.android.domain.memo.NewMemo
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.memo.component.guardedWrite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.duriseo.android.core.designsystem.R as DesignR

/**
 * SCR-MEMO-04 메모 작성·수정의 입력 상태와 저장. 입력은 화면 안에서만 들고 있고, [저장]은 온라인에서만 한다 (NFR-OFF-02).
 * 서버가 성공해야 Room이 바뀐다 (낙관적 갱신 없음).
 *
 * 단독 화면이다 (탭 없음, 메모 날짜 없음, D-174). 메모 id는 `SavedStateHandle`의 [ARG_MEMO_ID] 키에서 읽는다:
 * `MemoRouteModel.Edit(memoId)`로 들어오면 수정이고, 값이 없으면(메모 탭의 FAB) **새 메모**다.
 *
 * - 저장 조건 (D-132): 제목·본문을 정리(제목 앞뒤 공백 제거, 본문은 공백·줄바꿈뿐이면 없음)한 뒤 하나라도 있어야 한다.
 * - 입력 제한 (D-133): 제목 50자·본문 5,000자에서 더 입력되지 않고 오류 문구는 없다.
 * - 알림 스위치: 커플 공간의 새 메모만, 기본 켜짐. 개인 공간·수정은 `notifyPartner`를 보내지 않는다 (FR-NOTI-04 AC3).
 * - 수정은 **바꾼 필드만** `PATCH`하고, 바뀐 것이 없으면 요청 없이 닫는다 (FR-MEMO-03 AC4, D-123).
 * - 서버 오류 (D-121): 수정이 404면 공통 오류를 보이고 수정과 상세를 함께 닫는다(저장소가 기기에서 지움). 403이면 공통 오류를 보이고
 *   수정 화면만 닫는다(저장소가 상세를 다시 불러옴). 그 밖의 오류는 문구만 보이고 입력은 그대로다.
 */
@HiltViewModel
class MemoEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    private val meRepository: MeRepository,
) : ViewModel() {

    private val memoId: String? = savedStateHandle.get<String>(ARG_MEMO_ID)

    private val _uiState = MutableStateFlow(MemoEditUiState(isEdit = memoId != null))
    val uiState: StateFlow<MemoEditUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<MemoEditSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    /** 수정 화면이 열릴 때의 메모. 바뀐 필드만 보내려고 들고 있다 */
    private var original: Memo? = null

    init {
        viewModelScope.launch { initialize() }
    }

    private suspend fun initialize() {
        var current = meRepository.observeMe().first()
        if (current == null) {
            if (memoId != null) {
                // 단독 수정 화면: 내 정보가 없으면 닫는다
                effectChannel.trySend(MemoEditSideEffect.Close)
                return
            }
            // 새 메모: 작성 화면을 닫지 않고 저장만 막는다. 내 정보를 받으면 풀린다
            _uiState.update { it.copy(loaded = true, meMissing = true) }
            current = meRepository.observeMe().filterNotNull().first()
        }
        val me = checkNotNull(current)
        val isCouple = me.space.type == SpaceType.COUPLE && me.space.partner != null
        if (memoId == null) {
            // 새 메모: 커플이면 알림 스위치가 켜져 있다
            _uiState.update { it.copy(loaded = true, isCouple = isCouple, meMissing = false) }
            return
        }
        val memo = memoRepository.observeMemo(memoId).first()
        if (memo == null || memo.authorId != me.id) {
            // 사라졌거나 내가 쓴 메모가 아니다 (직접 열 수 없는 경로)
            effectChannel.trySend(MemoEditSideEffect.ShowMessage(DesignR.string.error_common))
            effectChannel.trySend(MemoEditSideEffect.Close)
            return
        }
        original = memo
        _uiState.update {
            it.copy(
                loaded = true,
                isCouple = isCouple,
                title = memo.title.orEmpty(),
                body = memo.body.orEmpty(),
            )
        }
    }

    fun onAction(action: MemoEditAction) {
        val s = _uiState.value
        if (!s.loaded || s.saving) {
            if (action == MemoEditAction.CloseClicked && !s.saving) effectChannel.trySend(MemoEditSideEffect.Close)
            return
        }
        when (action) {
            is MemoEditAction.TitleChanged -> _uiState.update { it.copy(title = MemoRules.limitTitle(action.text)) }
            is MemoEditAction.BodyChanged -> _uiState.update { it.copy(body = MemoRules.limitBody(action.text)) }
            is MemoEditAction.NotifyChanged -> if (s.showNotify) _uiState.update { it.copy(notifyPartner = action.on) }

            MemoEditAction.SaveClicked -> save()
            MemoEditAction.CloseClicked -> effectChannel.trySend(MemoEditSideEffect.Close)
        }
    }

    // --- 저장 ---

    private fun save() {
        val s = _uiState.value
        if (!s.saveEnabled) return
        val before = original
        if (memoId == null || before == null) {
            _uiState.update { it.copy(saving = true, loading = true) }
            viewModelScope.launch {
                // 입력 그대로 넘긴다. 정리(D-132)는 저장소가 보내기 직전에 한다. 개인 공간은 notifyPartner를 보내지 않는다
                // 저장소가 예외를 던져도 덮개는 걷힌다 (예외는 공통 오류로, D-115)
                val result = try {
                    guardedWrite {
                        memoRepository.create(
                            NewMemo(
                                title = s.title,
                                body = s.body,
                                notifyPartner = if (s.showNotify) s.notifyPartner else null,
                            ),
                        )
                    }
                } finally {
                    _uiState.update { it.copy(loading = false) }
                }
                when (result) {
                    is DataResult.Success -> closeAfterRequest()
                    is DataResult.Failure -> fail(result.error)
                }
            }
            return
        }
        // 바꾼 필드만 보낸다. 같은 값이면 변경으로 보지 않고 요청 없이 닫는다 (FR-MEMO-03 AC4, D-123)
        val patch = MemoRules.patchOf(before, s.title, s.body)
        if (patch.isEmpty) {
            effectChannel.trySend(MemoEditSideEffect.Close)
            return
        }
        _uiState.update { it.copy(saving = true, loading = true) }
        viewModelScope.launch {
            val result = try {
                guardedWrite { memoRepository.update(memoId, patch) }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
            when (result) {
                is DataResult.Success -> closeAfterRequest()
                is DataResult.Failure -> fail(result.error)
            }
        }
    }

    /** 요청이 성공했다: 로딩 덮개를 먼저 걷고 닫는다 (D-115). `saving`은 닫힐 때까지 켜 둬 입력을 막는다 */
    private fun closeAfterRequest() {
        _uiState.update { it.copy(loading = false) }
        effectChannel.trySend(MemoEditSideEffect.Close)
    }

    /**
     * 요청 실패 (D-121): 덮개를 걷은 뒤 안내한다. 수정이 404(`NOT_FOUND`)면 수정과 상세를 함께 닫고, 403(`NOT_AUTHOR`)이면 수정만 닫는다.
     * 그 밖의 오류와 오프라인은 문구만 보이고 입력은 그대로 둔다. 문구는 모두 공통 오류·오프라인 안내다 (새 문구 없음).
     */
    private fun fail(error: DataError) {
        _uiState.update { it.copy(saving = false, loading = false) }
        effectChannel.trySend(MemoEditSideEffect.ShowMessage(error.toMessageRes()))
        if (memoId != null) {
            when {
                error.hasCode(ErrorCode.NOT_FOUND) -> effectChannel.trySend(MemoEditSideEffect.CloseWithDetail)
                error.hasCode(ErrorCode.NOT_AUTHOR) -> effectChannel.trySend(MemoEditSideEffect.Close)
            }
        }
    }

    companion object {
        /** `MemoRouteModel.Edit::memoId`의 이름. 항목에 이 키가 없으면 새 메모다 */
        const val ARG_MEMO_ID = "memoId"
    }
}
