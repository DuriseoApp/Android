package com.duriseo.android.feature.calendar.detail

import androidx.annotation.StringRes
import com.duriseo.android.domain.event.EventTarget
import com.duriseo.android.domain.event.ReminderOption
import java.time.LocalDate
import java.time.LocalTime

/** 일정 한 건의 상세 (SCR-CAL-02). 시각은 기기 시간대로 바꾼 값이다 */
data class EventDetail(
    val id: String,
    val title: String,
    val note: String?,
    val isAllDay: Boolean,
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val endDate: LocalDate,
    val endTime: LocalTime?,
    val target: EventTarget,
    /** 작성한 사람의 닉네임 (FR-CAL-03 AC3). 모르면 빈 문자열 */
    val authorName: String,
    /** 내 알림. 없음이면 null (FR-CAL-05) */
    val myReminder: ReminderOption?,
    /** 수정·삭제 메뉴 노출: 작성자이거나 "함께" 일정 (PRD 7장 권한 매트릭스, FR-CAL-03 AC2) */
    val canEdit: Boolean,
)

data class EventDetailUiState(
    /** 아직 Room에서 읽지 못했다 */
    val event: EventDetail? = null,
    val deleteDialogVisible: Boolean = false,
    /** 삭제 요청 중에는 다이얼로그 버튼과 뒤 막이 눌리지 않는다 (D-88) */
    val deleting: Boolean = false,
    /** 알림 변경 요청 중 */
    val reminderSaving: Boolean = false,
)

sealed interface EventDetailAction {
    /** 화면이 보이기 시작했다. 일정을 다시 읽는다 (NFR-SYNC-02) */
    data object Shown : EventDetailAction
    data object BackClicked : EventDetailAction
    data object EditClicked : EventDetailAction
    data class ReminderSelected(val option: ReminderOption?) : EventDetailAction
    data object DeleteClicked : EventDetailAction
    data object DeleteCancelled : EventDetailAction
    data object DeleteConfirmed : EventDetailAction
}

sealed interface EventDetailSideEffect {
    data object Close : EventDetailSideEffect
    data class OpenEdit(val id: String) : EventDetailSideEffect
    data class ShowMessage(@StringRes val messageRes: Int) : EventDetailSideEffect
}
