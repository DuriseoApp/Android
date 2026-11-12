package com.duriseo.android.feature.memo.edit

import androidx.annotation.StringRes
import com.duriseo.android.domain.memo.MemoRules
import java.time.LocalDate
import java.time.YearMonth

/** 날짜 선택 시트 (SCR-MEMO-04 "날짜 영역"): 보이는 달과 고른 날. [완료]를 눌러야 메모 날짜가 바뀐다 */
data class MemoDateSheetState(
    val visibleMonth: YearMonth,
    val selected: LocalDate,
)

/**
 * SCR-MEMO-04 메모 작성·수정의 상태 (FR-MEMO-02, FR-MEMO-03, FR-NOTI-04, D-132, D-133).
 * 화면 바깥(앱바·[저장] 버튼)은 [saveEnabled]·[loading]으로 그리고, 입력 부분은 [MemoForm]이 그린다.
 *
 * @property isEdit 수정이다: 종류 탭이 없고 알림 스위치가 없다
 * @property isCouple 커플 공간이다. 개인 공간에는 알림 스위치가 없다
 * @property memoDate 메모 날짜. 기본은 없음이고 [날짜 추가]로 넣는다. 캘린더에서 고른 날로 미리 채우지 않는다 (D-133)
 * @property notifyPartner "상대에게 알림 보내기" 값. 기본 켜짐 (커플의 새 메모만 쓴다)
 * @property saving 저장 요청이 도는 동안 true: 입력과 [저장]을 막는다
 * @property loading 저장 요청이 도는 동안만 true: 로딩 덮개 (D-115). 성공하면 닫기 전에 먼저 걷는다
 * @property today 기기 시계의 오늘. 날짜 시트가 처음 보여 줄 달(메모 날짜가 없을 때)이다
 */
data class MemoEditUiState(
    val loaded: Boolean = false,
    val isEdit: Boolean = false,
    val isCouple: Boolean = false,
    val title: String = "",
    val body: String = "",
    val memoDate: LocalDate? = null,
    val notifyPartner: Boolean = true,
    val dateSheet: MemoDateSheetState? = null,
    val saving: Boolean = false,
    val loading: Boolean = false,
    val today: LocalDate = LocalDate.of(2026, 1, 1),
) {
    /**
     * [저장] 활성: 제목과 본문이 앞뒤 공백을 지운 뒤 모두 비면 비활성이다. 날짜만 있어도 비활성이다 (D-132, FR-MEMO-02 AC2).
     * 수정해서 둘 다 비게 되는 경우도 같다 (AC5).
     */
    val saveEnabled: Boolean get() = loaded && !saving && MemoRules.isSavable(title, body)

    /** "상대에게 알림 보내기" 스위치는 커플 공간의 새 메모에서만 보인다. 개인 공간과 수정에는 없다 (FR-NOTI-04 AC3) */
    val showNotify: Boolean get() = isCouple && !isEdit

    val hasDate: Boolean get() = memoDate != null
}

sealed interface MemoEditAction {
    data class TitleChanged(val text: String) : MemoEditAction
    data class BodyChanged(val text: String) : MemoEditAction
    data class NotifyChanged(val on: Boolean) : MemoEditAction

    /** [날짜 추가]: 날짜 시트를 연다 (고른 날이 날짜 행이 된다) */
    data object DateAddClicked : MemoEditAction

    /** 날짜 행: 날짜를 바꾸는 시트를 연다 */
    data object DateRowClicked : MemoEditAction

    /** 날짜 행 안의 "날짜 빼기" */
    data object DateRemoved : MemoEditAction

    data object DatePreviousMonth : MemoEditAction
    data object DateNextMonth : MemoEditAction
    data class DateMonthSelected(val month: YearMonth) : MemoEditAction
    data class DatePicked(val date: LocalDate) : MemoEditAction
    data object DateConfirmed : MemoEditAction
    data object DateSheetDismissed : MemoEditAction

    data object SaveClicked : MemoEditAction
    data object CloseClicked : MemoEditAction
}

sealed interface MemoEditSideEffect {
    /** 저장했거나(같은 값이면 요청 없이) 닫는다. 새로 쓰기는 앞 화면으로, 수정은 상세로 돌아간다 */
    data object Close : MemoEditSideEffect

    /** 수정 화면에서 서버가 그 메모가 없다고 했다 (D-121): 수정 화면과 상세를 함께 닫는다 */
    data object CloseWithDetail : MemoEditSideEffect

    data class ShowMessage(@StringRes val messageRes: Int) : MemoEditSideEffect
}
