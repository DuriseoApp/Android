package com.duriseo.android.feature.memo.list

import com.duriseo.android.feature.memo.component.MemoItemUi

/** 메모 5개마다 광고 자리를 하나 넣는다 (FR-AD-01 AC1, D-185) */
const val MEMO_AD_INTERVAL = 5

/** SCR-MEMO-01 일반 목록의 한 줄: 메모이거나 광고 자리 */
sealed interface MemoListRow {
    /** LazyColumn 키. 메모는 ID, 광고는 자리 번호 */
    val key: String

    data class Memo(val item: MemoItemUi) : MemoListRow {
        override val key: String get() = item.id
    }

    /**
     * 광고 자리. 메모가 아니라서 개수·정렬·5개 세기에 들어가지 않는다 (FR-AD-01 AC2).
     * @property slot 0부터 센 자리 번호 (5번째 뒤 = 0, 10번째 뒤 = 1 …). 자리마다 광고를 따로 받는다 (결정 6)
     */
    data class Ad(val slot: Int) : MemoListRow {
        override val key: String get() = "ad-$slot"
    }
}

/**
 * 일반 목록(고정 영역 제외)에 광고 자리를 끼운다. 메모 [MEMO_AD_INTERVAL]개마다 그 뒤에 하나(5·10·15번째 뒤).
 * 메모가 5개보다 적으면 자리가 없다. 보기(최신순·오래된순·즐겨찾기)와 상관없이 같은 규칙이다.
 */
fun memoRowsWithAds(items: List<MemoItemUi>): List<MemoListRow> = buildList {
    items.forEachIndexed { index, item ->
        add(MemoListRow.Memo(item))
        val count = index + 1
        if (count % MEMO_AD_INTERVAL == 0) add(MemoListRow.Ad(slot = count / MEMO_AD_INTERVAL - 1))
    }
}
