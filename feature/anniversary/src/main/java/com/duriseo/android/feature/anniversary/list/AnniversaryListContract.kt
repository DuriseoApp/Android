package com.duriseo.android.feature.anniversary.list

import androidx.annotation.StringRes
import com.duriseo.android.domain.anniversary.AnniversaryRules
import java.time.LocalDate
import java.time.YearMonth

/** 만든 사람을 보는 사람 기준으로 나눈 것 (라벨 "내가"·"상대가", 사람 색 점) */
enum class AnniversaryAuthor { ME, PARTNER }

/**
 * 다가오는 기념일 한 줄 (SCR-ANNIV-01).
 *
 * @property anniversaryId 직접 기념일의 id. 자동은 null
 * @property author 직접 기념일의 만든 사람. 자동은 null (라벨 "자동 기념일")
 * @property editable 내가 만든 직접 기념일만 true: 누르면 SCR-ANNIV-02로 간다. 자동·상대 것은 누를 수 없다 (FR-ANNIV-03 AC2, 확인 필요 3)
 */
data class AnniversaryItemUi(
    val key: String,
    val anniversaryId: String?,
    val name: String,
    val date: LocalDate,
    val daysLeft: Int,
    val author: AnniversaryAuthor?,
    val repeatsYearly: Boolean,
    val editable: Boolean,
) {
    val isAuto: Boolean get() = anniversaryId == null

    /** 오늘이 기념일이면 배지가 "D-day" (FR-ANNIV-04 AC1) */
    val isToday: Boolean get() = daysLeft == 0

    /** 오늘과 30일 이내는 `brand-100`/`brand-700`로 강조한다 */
    val isNear: Boolean get() = AnniversaryRules.isNear(daysLeft)
}

/** 지난 기념일 한 줄. 내가 만든 항목만 › 가 있고 누를 수 있다 (D-71) */
data class PastItemUi(
    val key: String,
    val anniversaryId: String,
    val name: String,
    val date: LocalDate,
    val author: AnniversaryAuthor,
    val editable: Boolean,
)

/** 함께한 날 카드 (커플, 처음 만난 날이 있을 때). 색은 서버 팔레트 키 그대로 [com.duriseo.android.core.designsystem.component.DuriseoTogetherMark]에 넘긴다 */
data class TogetherCardUi(
    val dayNumber: Long,
    val startDate: LocalDate,
    val meColorKey: String,
    val partnerColorKey: String,
    val togetherColorKey: String,
)

/** 처음 만난 날 선택 시트 (SCR-SET-01 소속 시트를 재사용, `PATCH /space`). [저장]은 바로 서버에 저장한다 */
data class StartDateSheetState(
    val visibleMonth: YearMonth,
    val selected: LocalDate?,
    val saving: Boolean = false,
)

/**
 * SCR-ANNIV-01 기념일 목록 (FR-ANNIV-02, FR-ANNIV-04).
 *
 * @property isCouple 커플 공간이면 함께한 날 카드(또는 안내 카드)와 커플 하단 안내가 있다. 개인 공간은 없다 (FR-ANNIV-04 AC2)
 * @property together 함께한 날 카드. 커플이고 처음 만난 날이 있을 때만 값이 있다
 * @property pastCount "지난 기념일 N개"의 N. 0이면 행을 숨긴다 (D-71)
 * @property hasMoreUpcoming 다음 페이지가 있다 (스크롤 끝에서 이어 붙인다)
 */
data class AnniversaryListUiState(
    val loaded: Boolean = false,
    val isCouple: Boolean = false,
    val together: TogetherCardUi? = null,
    val items: List<AnniversaryItemUi> = emptyList(),
    val pastCount: Int = 0,
    val pastItems: List<PastItemUi> = emptyList(),
    val pastOpen: Boolean = false,
    val hasMoreUpcoming: Boolean = false,
    val hasMorePast: Boolean = false,
    val startDateSheet: StartDateSheetState? = null,
    val today: LocalDate = LocalDate.of(2026, 1, 1),
) {
    /** 처음 만난 날 저장 요청이 도는 동안 true. 로딩 덮개를 그린다 (D-115). 목록 조회·다음 페이지는 덮지 않는다 */
    val loading: Boolean get() = startDateSheet?.saving == true

    /** 커플인데 처음 만난 날이 없으면 함께한 날 카드 자리에 입력 안내를 보인다 (FR-ANNIV-04 AC4) */
    val showStartDatePrompt: Boolean get() = isCouple && together == null

    /** 비었을 때 "아직 기념일이 없어요" + [기념일 추가] (개인 공간, FR-ANNIV-04 AC2) */
    val isEmpty: Boolean get() = loaded && items.isEmpty()

    val showPastRow: Boolean get() = pastCount > 0
}

sealed interface AnniversaryListAction {
    data object BackClicked : AnniversaryListAction
    data object AddClicked : AnniversaryListAction
    data class ItemClicked(val item: AnniversaryItemUi) : AnniversaryListAction
    data class PastItemClicked(val item: PastItemUi) : AnniversaryListAction
    data object PastToggled : AnniversaryListAction

    /** 목록 끝 가까이까지 스크롤했다: 다음 페이지를 이어 붙인다 */
    data object UpcomingEndReached : AnniversaryListAction
    data object PastEndReached : AnniversaryListAction

    data object StartDateInputClicked : AnniversaryListAction
    data object StartDatePreviousMonth : AnniversaryListAction
    data object StartDateNextMonth : AnniversaryListAction
    data class StartDateSelected(val date: LocalDate) : AnniversaryListAction
    data class StartDateMonthSelected(val month: YearMonth) : AnniversaryListAction
    data object StartDateSaved : AnniversaryListAction
    data object StartDateSheetDismissed : AnniversaryListAction
}

sealed interface AnniversaryListSideEffect {
    data object Back : AnniversaryListSideEffect

    /** 새 기념일 (SCR-ANNIV-02) */
    data object OpenCreate : AnniversaryListSideEffect

    /** 내 직접 기념일 수정 (SCR-ANNIV-02). [date]는 누른 항목의 날짜다 (확인 필요 1) */
    data class OpenEdit(val anniversaryId: String, val date: LocalDate) : AnniversaryListSideEffect

    data class ShowMessage(@StringRes val messageRes: Int) : AnniversaryListSideEffect
}
