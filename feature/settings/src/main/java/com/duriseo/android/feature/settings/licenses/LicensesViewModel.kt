package com.duriseo.android.feature.settings.licenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SCR-SET-05 오픈소스 라이선스. 라이브러리는 이름순(대소문자 무시)이고, 글꼴 IBM Plex Sans KR·IBM Plex Mono는
 * 목록 끝에 온다 (D-152). 행을 누르면 그 자리에서 펼치고 다시 누르면 접는다.
 */
@HiltViewModel
class LicensesViewModel @Inject constructor(
    private val source: LicenseSource,
) : ViewModel() {

    private val mutableState = MutableStateFlow(LicensesUiState())
    val uiState: StateFlow<LicensesUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val libraries = source.libraries()
                .distinctBy { it.id }
                .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
            val fontText = source.fontLicenseText()
            val fonts = FontNames.map {
                LicenseEntry(id = "font:$it", name = it, licenseName = FontLicenseName, body = fontText)
            }
            mutableState.update { state ->
                state.copy(rows = (libraries + fonts).map { LicenseRow(it.id, it.name, it.licenseName, it.body) })
            }
        }
    }

    fun onAction(action: LicensesAction) {
        when (action) {
            is LicensesAction.RowClicked -> mutableState.update { state ->
                state.copy(expandedId = if (state.expandedId == action.id) null else action.id)
            }
        }
    }

    private companion object {
        /** 앱이 쓰는 글꼴 (Google Fonts 다운로드). 목록 끝에 이 순서로 둔다 */
        val FontNames = listOf("IBM Plex Sans KR", "IBM Plex Mono")
        const val FontLicenseName = "SIL OFL 1.1"
    }
}
