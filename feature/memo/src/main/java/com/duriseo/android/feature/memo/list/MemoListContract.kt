package com.duriseo.android.feature.memo.list

import androidx.annotation.StringRes
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.feature.memo.component.MemoItemUi

/** 고정 영역에 보이는 핀 카드 최대 개수. 4개 이상이면 최근 고정 3개 + [고정 메모 전체 보기] (FR-MEMO-05 AC2·AC3) */
const val MAX_VISIBLE_PINS = 3

/**
 * SCR-MEMO-01 메모 목록 (FR-MEMO-01, FR-MEMO-05, FR-MEMO-06). 화면은 Room만 구독한다 (NFR-OFF-01).
 *
 * @property loaded 내 정보를 읽었다 (작성자 닉네임·색을 알 수 있다)
 * @property pinsLoaded 고정 목록을 한 번이라도 받았다. false인 빈 `pins`는 "고정 없음"이 아니라 "아직 안 받음"이다
 * @property pins 고정 메모 전체, 최근 고정순. "고정된 핀 N개"의 N은 길이다 (SCREENS "개수 표시 출처")
 * @property view 고른 보기 칩
 * @property items 고른 보기의 목록. 고정된 메모는 없다 (D-130)
 * @property totalCount "전체 메모 N개"의 N: 지금 고른 보기의 개수 (`totalCount`)
 * @property favoriteCount "★ 즐겨찾기 N"의 N (`favoriteCount`, 고정되지 않은 내 즐겨찾기)
 * @property hasMore 다음 페이지가 있다 (스크롤 끝에서 이어 붙인다)
 * @property listLoaded 이 보기를 한 번이라도 받았다. false인 빈 목록은 "없음"이 아니라 "아직 안 받음"이다
 * @property writing 핀·별 요청이 도는 동안 true (로딩 덮개, D-115)
 */
data class MemoListUiState(
    val loaded: Boolean = false,
    val pins: List<MemoItemUi> = emptyList(),
    val pinsLoaded: Boolean = false,
    val view: MemoView = MemoView.NEWEST,
    val items: List<MemoItemUi> = emptyList(),
    val totalCount: Int = 0,
    val favoriteCount: Int = 0,
    val hasMore: Boolean = false,
    val listLoaded: Boolean = false,
    val writing: Boolean = false,
) {
    /** 로딩 덮개를 그린다. 목록 조회·다음 페이지는 덮지 않는다 (D-115) */
    val loading: Boolean get() = writing

    /** 고정 영역에 그릴 핀 카드: 최근 고정 3개까지 */
    val visiblePins: List<MemoItemUi> get() = pins.take(MAX_VISIBLE_PINS)

    /** 고정 4개 이상이면 [고정 메모 전체 보기 (N) ›] */
    val showPinsMore: Boolean get() = pins.size > MAX_VISIBLE_PINS

    /** 고정 없음: 점선 상자. 고정 목록을 받기 전에는 숨긴다 (받기 전의 빈 목록은 "없음"이 아니다) */
    val showNoPins: Boolean get() = pinsLoaded && pins.isEmpty()

    /**
     * 즐겨찾기 없음: "즐겨찾기한 메모가 없어요. 메모 옆 별을 눌러 추가해 보세요." 메모가 하나도 없는 최신순·오래된순은
     * 문서에 문구가 없어 "전체 메모 0개"와 빈 목록만 그린다 (unit-6.md 확인 필요 2).
     */
    val showFavoritesEmpty: Boolean get() = view == MemoView.FAVORITES && listLoaded && items.isEmpty()
}

sealed interface MemoListAction {
    /** 화면이 보이기 시작했다 (탭으로 들어올 때, 하위 화면에서 돌아올 때): 고정 목록과 지금 보기의 첫 페이지를 다시 불러온다 */
    data object Shown : MemoListAction

    data class ViewSelected(val view: MemoView) : MemoListAction
    data class ItemClicked(val item: MemoItemUi) : MemoListAction
    data class PinClicked(val item: MemoItemUi) : MemoListAction
    data class FavoriteClicked(val item: MemoItemUi) : MemoListAction
    data object PinsMoreClicked : MemoListAction
    data object AddClicked : MemoListAction

    /** 목록 끝 가까이까지 스크롤했다: 다음 페이지를 이어 붙인다 */
    data object EndReached : MemoListAction
}

sealed interface MemoListSideEffect {
    /** 메모 상세 (SCR-MEMO-03) */
    data class OpenDetail(val memoId: String) : MemoListSideEffect

    /** 고정 메모 전체 (SCR-MEMO-02) */
    data object OpenPinned : MemoListSideEffect

    /** 새 메모 (SCR-MEMO-04) */
    data object OpenCreate : MemoListSideEffect

    data class ShowMessage(@StringRes val messageRes: Int) : MemoListSideEffect
}
