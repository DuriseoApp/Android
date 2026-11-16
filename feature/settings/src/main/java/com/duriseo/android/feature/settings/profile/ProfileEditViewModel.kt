package com.duriseo.android.feature.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.message.toMessageResOrNull
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.profile.ProfileNameRules
import com.duriseo.android.domain.profile.ProfileRepository
import com.duriseo.android.domain.profile.photoErrorOrNull
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.settings.profile.photo.CameraAvailability
import com.duriseo.android.feature.settings.profile.photo.PickedPhotoReader
import com.duriseo.android.feature.settings.profile.photo.PickedPhotoResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import javax.inject.Inject

/**
 * SCR-SET-04 내 프로필 (FR-AUTH-07). 화면은 Room의 내 정보만 구독하고 쓰기는 온라인에서만 한다 (NFR-OFF-02).
 * 고른 사진은 [저장]을 누르기 전에는 기기에서 미리보기만 하고 서버에 올리지 않는다 (D-146).
 */
@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val meRepository: MeRepository,
    private val profileRepository: ProfileRepository,
    private val photoReader: PickedPhotoReader,
    cameraAvailability: CameraAvailability,
) : ViewModel() {

    private data class Local(
        /** 입력 중인 이름. 아직 건드리지 않았으면 null이라 Room의 이름을 보인다 */
        val name: String? = null,
        val selection: PhotoSelection = PhotoSelection.Keep,
        val sheetVisible: Boolean = false,
        val saving: Boolean = false,
    )

    private val local = MutableStateFlow(Local())
    private val cameraAvailable = cameraAvailability.isAvailable()
    private var pickJob: Job? = null

    val uiState: StateFlow<ProfileEditUiState> = combine(meRepository.observeMe(), local) { me, localState ->
        buildState(me, localState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProfileEditUiState(cameraAvailable = cameraAvailable))

    private val effectChannel = Channel<ProfileEditSideEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private fun buildState(me: Me?, localState: Local): ProfileEditUiState {
        if (me == null) {
            return ProfileEditUiState(
                sheetVisible = localState.sheetVisible,
                cameraAvailable = cameraAvailable,
                saving = localState.saving,
            )
        }
        return ProfileEditUiState(
            loaded = true,
            name = localState.name ?: me.nickname,
            savedName = me.nickname,
            colorKey = me.space.myColor,
            savedPhoto = me.savedPhoto(),
            selection = localState.selection,
            account = me.toAccountLine(),
            sheetVisible = localState.sheetVisible,
            cameraAvailable = cameraAvailable,
            saving = localState.saving,
        )
    }

    fun onAction(action: ProfileEditAction) {
        when (action) {
            is ProfileEditAction.NameChanged -> {
                if (local.value.saving) return
                local.update { it.copy(name = ProfileNameRules.limit(action.text)) }
            }
            ProfileEditAction.PhotoButtonClicked -> {
                if (local.value.saving) return
                local.update { it.copy(sheetVisible = true) }
            }
            ProfileEditAction.SheetDismissed -> local.update { it.copy(sheetVisible = false) }
            ProfileEditAction.AlbumClicked -> {
                local.update { it.copy(sheetVisible = false) }
                effectChannel.trySend(ProfileEditSideEffect.LaunchAlbum)
            }
            ProfileEditAction.CameraClicked -> {
                local.update { it.copy(sheetVisible = false) }
                // 카메라 앱이 없으면 시트가 이 행을 숨긴다. 그래도 들어오면 아무것도 하지 않는다
                if (cameraAvailable) effectChannel.trySend(ProfileEditSideEffect.LaunchCamera)
            }
            ProfileEditAction.DefaultImageClicked ->
                local.update { it.copy(sheetVisible = false, selection = PhotoSelection.DefaultImage) }
            is ProfileEditAction.PhotoPicked -> pickPhoto(action.uri, action.fromCamera)
            ProfileEditAction.SaveClicked -> save()
        }
    }

    /** 고른 사진을 읽어 검사하고 줄인다 (IO 스레드). 통과하면 기기 미리보기만 바꾸고 올리지는 않는다. 실패하면 아바타는 원래 모습 그대로다 */
    private fun pickPhoto(uri: String, fromCamera: Boolean) {
        pickJob?.cancel()
        pickJob = viewModelScope.launch {
            when (val result = photoReader.read(uri, fromCamera)) {
                is PickedPhotoResult.Ready ->
                    local.update { if (it.saving) it else it.copy(selection = PhotoSelection.Picked(result.photo)) }
                is PickedPhotoResult.Rejected ->
                    effectChannel.trySend(ProfileEditSideEffect.ShowMessage(result.error.toMessageRes()))
                PickedPhotoResult.Failed ->
                    effectChannel.trySend(ProfileEditSideEffect.ShowMessage(DesignR.string.error_common))
            }
        }
    }

    /**
     * [저장] (D-146): 이름이 바뀌었으면 이름 -> 사진이 바뀌었으면 사진(기본 이미지면 삭제). 요청이 도는 동안 덮개를 그리고(D-115)
     * 어떤 결과든 덮개를 푼다. 중간에 실패하면 거기서 멈추고 오류를 보인다. 이미 된 것은 Room이 바뀌어 원래 값이 되므로
     * 다시 누르면 남은 것만 저장된다. 모두 성공하면 화면을 닫는다. 오프라인이면 저장소가 요청 없이 `Offline`을 돌려준다 (NFR-OFF-02).
     */
    private fun save() {
        if (local.value.saving) return
        local.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val me = meRepository.observeMe().first()
                val state = me?.let { buildState(it, local.value) }
                if (state == null || !state.canSaveIgnoringFlag()) return@launch
                val failure = runSteps(state)
                if (failure == null) {
                    effectChannel.trySend(ProfileEditSideEffect.Close)
                } else {
                    val messageRes = failure.photoErrorOrNull()?.toMessageRes() ?: failure.toMessageResOrNull()
                    // 세션이 끊겼다는 응답이면 안내 없이 세션 종료 흐름이 시작 화면으로 보낸다
                    messageRes?.let { effectChannel.trySend(ProfileEditSideEffect.ShowMessage(it)) }
                }
            } finally {
                local.update { it.copy(saving = false) }
            }
        }
    }

    /** 이름 -> 사진 순서로 보낸다. 실패하면 그 오류를, 모두 성공하면 null을 돌려준다 */
    private suspend fun runSteps(state: ProfileEditUiState): DataError? {
        if (state.nameChanged) {
            val result = guardedWrite { profileRepository.updateNickname(ProfileNameRules.normalize(state.name)) }
            if (result is DataResult.Failure) return result.error
        }
        val result = when (val change = state.photoChange) {
            PhotoChange.None -> return null
            is PhotoChange.Upload -> guardedWrite { profileRepository.uploadPhoto(change.photo.bytes, change.photo.mimeType) }
            PhotoChange.Delete -> guardedWrite { profileRepository.deletePhoto() }
        }
        return when (result) {
            is DataResult.Success -> {
                // 사진까지 됐다. 고른 사진은 서버에 올라갔으니 들고 있지 않는다
                local.update { it.copy(selection = PhotoSelection.Keep) }
                null
            }
            is DataResult.Failure -> {
                // 서버가 사진을 거부했으면(413·415) 같은 사진으로 다시 보내도 실패하므로 고른 것을 버리고 원래 모습으로 돌린다
                if (result.error.photoErrorOrNull() != null) local.update { it.copy(selection = PhotoSelection.Keep) }
                result.error
            }
        }
    }

    /** 저장 중 플래그는 이 호출 자신이 켰으므로 빼고, 나머지 조건(이름이 비지 않음, 바뀐 것이 있음)만 본다 */
    private fun ProfileEditUiState.canSaveIgnoringFlag(): Boolean =
        loaded && !nameBlank && (nameChanged || photoChange != PhotoChange.None)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
