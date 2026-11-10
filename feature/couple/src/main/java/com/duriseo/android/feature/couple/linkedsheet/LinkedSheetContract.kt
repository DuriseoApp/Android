package com.duriseo.android.feature.couple.linkedsheet

import androidx.annotation.StringRes
import com.duriseo.android.domain.me.ImportCounts
import java.time.LocalDate
import java.time.YearMonth

/** 혼자 쓴 기록을 어떻게 할지 (FR-COUPLE-03 AC1·AC2) */
enum class ImportChoice {
    /** [가져오기]: `POST /couple/import {import: true}` */
    Keep,

    /** [가져오지 않기]: 확인 다이얼로그 뒤 `{import: false}` */
    Drop,
}

/** 칸을 눌러 위에 한 겹 더 뜬 처음 만난 날 선택 시트의 상태 (D-98). [저장]은 칸만 채우고 서버를 부르지 않는다 */
data class StartDatePickerState(
    val visibleMonth: YearMonth,
    val selected: LocalDate?,
)

/**
 * SCR-COUPLE-04 연결 완료 시트의 상태 (FR-COUPLE-02 AC1, FR-COUPLE-03, FR-COUPLE-04 AC4, D-93, D-98).
 *
 * 값은 Room의 내 정보(`MeSpace`)에서 오고, 고른 날짜·기록 선택·열린 시트만 이 화면이 들고 있다.
 *
 * @property partnerName 상대 닉네임 ("[상대 이름]님과 연결됐어요")
 * @property startDate 칸에 보이는 처음 만난 날. 이미 있는 날짜(상대가 먼저 정했거나 복구된 공간)나 방금 고른 날짜. 없으면 null
 * @property serverStartDate 서버에 이미 있는 처음 만난 날. 있으면 [나중에 하기]가 없다 (D-98, D-84 지우기 없음)
 * @property hasRecords 가져오기 대기 중인 혼자 쓴 기록이 있다 (`importPending`). 있으면 고르기 전에는 닫을 수 없다 (FR-COUPLE-03 AC4)
 * @property counts 혼자 쓴 기록 개수 (D-93). 0개인 종류도 보여 준다 (Q-13)
 * @property choice 기록 선택. 시트를 처음 열면 [ImportChoice.Keep]이 미리 선택돼 있다 (FR-COUPLE-03 AC5, D-76)
 * @property datePicker 처음 만난 날 선택 시트가 떠 있으면 그 상태
 * @property dropDialogVisible "혼자 쓴 기록을 삭제할까요?" 확인 다이얼로그
 * @property requesting [시작하기] 요청 중. 버튼이 꺼진다 (SCREENS 공통 "요청 중")
 * @property today 처음 만난 날 선택 시트의 마지막 날짜 (기기 시계의 오늘)
 */
data class LinkedSheetUiState(
    val partnerName: String = "",
    val startDate: LocalDate? = null,
    val serverStartDate: LocalDate? = null,
    val hasRecords: Boolean = false,
    val counts: ImportCounts? = null,
    val choice: ImportChoice = ImportChoice.Keep,
    val datePicker: StartDatePickerState? = null,
    val dropDialogVisible: Boolean = false,
    val requesting: Boolean = false,
    val today: LocalDate = LocalDate.of(1970, 1, 1),
) {
    /** [시작하기] 요청이 도는 동안 true. 로딩 덮개를 그린다 (D-115) */
    val loading: Boolean get() = requesting

    /** 기록이 없을 때만 닫을 수 있다: 제목 오른쪽 닫기 버튼, 뒤 막, 뒤로 가기 (FR-COUPLE-03 AC4) */
    val canDismiss: Boolean get() = !hasRecords && !requesting

    /** 서버에 날짜가 이미 있으면 [나중에 하기]를 숨긴다 (D-98) */
    val showLater: Boolean get() = serverStartDate == null
}

sealed interface LinkedSheetAction {
    /** 처음 만난 날 칸을 눌렀다 → 선택 시트를 위에 한 겹 더 띄운다 */
    data object DateFieldClicked : LinkedSheetAction

    data object PreviousMonth : LinkedSheetAction

    data object NextMonth : LinkedSheetAction

    /** 연·월 제목에서 고른 달 (연도·월 선택) */
    data class MonthSelected(val month: YearMonth) : LinkedSheetAction

    data class DateSelected(val date: LocalDate) : LinkedSheetAction

    /** 선택 시트의 [저장]: 칸에 날짜를 채우기만 한다 (서버를 부르지 않음) */
    data object DateSaved : LinkedSheetAction

    /** 선택 시트의 뒤 막·뒤로 가기 */
    data object DatePickerDismissed : LinkedSheetAction

    /** [나중에 하기]: 고른 날짜를 비운다 */
    data object LaterClicked : LinkedSheetAction

    data class ChoiceSelected(val choice: ImportChoice) : LinkedSheetAction

    /** [시작하기] */
    data object StartClicked : LinkedSheetAction

    /** 다이얼로그 [삭제하고 시작] */
    data object DropConfirmed : LinkedSheetAction

    /** 다이얼로그 [취소] */
    data object DropCancelled : LinkedSheetAction

    /** 기록이 없을 때 제목 오른쪽의 닫기 버튼, 뒤 막 */
    data object CloseClicked : LinkedSheetAction

    /** 기기 뒤로 가기: 열린 겹부터 닫고, 마지막은 기록이 없을 때만 시트를 닫는다 */
    data object BackPressed : LinkedSheetAction
}

sealed interface LinkedSheetSideEffect {
    /** 시트의 스낵바에 문구를 보여 준다 (공통 오류, 오프라인 안내) */
    data class ShowMessage(@StringRes val messageRes: Int) : LinkedSheetSideEffect
}
