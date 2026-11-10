package com.duriseo.android.feature.anniversary.edit

import androidx.annotation.StringRes
import java.time.LocalDate
import java.time.YearMonth

/** 날짜 선택 시트 (일정과 같은 날짜 시트를 쓴다, 고를 수 있는 범위 1950-01-01 ~ 2050-12-31) */
data class DateSheet(val visibleMonth: YearMonth, val selected: LocalDate)

/**
 * SCR-ANNIV-02 직접 기념일 작성·수정 (FR-ANNIV-03, D-122).
 *
 * @property isEdit 수정 화면이면 true (앱바 "기념일 수정", [기념일 삭제])
 * @property isCouple 커플 공간이면 안내 문구가 "두 사람 모두에게 알림이 가요"
 * @property name 입력한 그대로(앞뒤 공백 포함, 30자까지). 저장할 때 앞뒤 공백을 지운다 (D-104)
 * @property originalName 수정 화면의 저장된 이름. 삭제 확인 문구에 쓴다
 * @property date 날짜 칸. 수정 화면에서는 누른 발생일이고 등록한 날짜가 아닐 수 있다 (unit-5.md 확인 필요 1)
 * @property saving 요청 중. 중복 요청을 막는다 (로딩 덮개는 단위 5 묶음 C)
 */
data class AnniversaryEditUiState(
    val loaded: Boolean = false,
    val isEdit: Boolean = false,
    val isCouple: Boolean = false,
    val name: String = "",
    val originalName: String = "",
    val date: LocalDate = LocalDate.of(2026, 1, 1),
    val repeatsYearly: Boolean = true,
    val saveEnabled: Boolean = false,
    val saving: Boolean = false,
    val dateSheet: DateSheet? = null,
    val deleteDialogVisible: Boolean = false,
    val today: LocalDate = LocalDate.of(2026, 1, 1),
) {
    /** 글자 수 표시 "9 / 30"의 앞 숫자 (코드 포인트) */
    val nameLength: Int
        get() = com.duriseo.android.domain.anniversary.AnniversaryRules.nameLength(name)
}

sealed interface AnniversaryEditAction {
    data class NameChanged(val text: String) : AnniversaryEditAction
    data class YearlyChanged(val on: Boolean) : AnniversaryEditAction
    data object DateRowClicked : AnniversaryEditAction

    data object DatePreviousMonth : AnniversaryEditAction
    data object DateNextMonth : AnniversaryEditAction
    data class DateMonthSelected(val month: YearMonth) : AnniversaryEditAction
    data class DatePicked(val date: LocalDate) : AnniversaryEditAction
    data object DateConfirmed : AnniversaryEditAction
    data object DateSheetDismissed : AnniversaryEditAction

    data object SaveClicked : AnniversaryEditAction
    data object CloseClicked : AnniversaryEditAction

    data object DeleteClicked : AnniversaryEditAction
    data object DeleteConfirmed : AnniversaryEditAction
    data object DeleteDismissed : AnniversaryEditAction
}

sealed interface AnniversaryEditSideEffect {
    /** 저장·삭제를 마쳤거나 닫는다 */
    data object Close : AnniversaryEditSideEffect
    data class ShowMessage(@StringRes val messageRes: Int) : AnniversaryEditSideEffect
}
