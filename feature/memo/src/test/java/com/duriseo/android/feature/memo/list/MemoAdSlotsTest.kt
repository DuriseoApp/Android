package com.duriseo.android.feature.memo.list

import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.domain.memo.MemoView
import com.duriseo.android.feature.memo.component.MemoAuthor
import com.duriseo.android.feature.memo.component.MemoItemUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoAdSlotsTest {

    private fun memos(count: Int) = (1..count).map {
        MemoItemUi(
            id = "m$it",
            title = "제목 $it",
            preview = "미리보기 $it",
            author = MemoAuthor.ME,
            nickname = "나",
            writtenAt = WrittenAt.Yesterday,
            isPinned = false,
            isFavorite = false,
            canEdit = true,
        )
    }

    /** 줄들을 "M3"(메모 3번째)와 "A0"(광고 자리 0) 문자열로 줄여 순서를 비교한다 */
    private fun List<MemoListRow>.shape() = map {
        when (it) {
            is MemoListRow.Memo -> "M" + it.item.id.removePrefix("m")
            is MemoListRow.Ad -> "A${it.slot}"
        }
    }

    @Test
    fun `FR-AD-01 AC1 5개마다 자리를 넣는다`() {
        val shape = memoRowsWithAds(memos(5)).shape()
        assertEquals(listOf("M1", "M2", "M3", "M4", "M5", "A0"), shape)
    }

    @Test
    fun `FR-AD-01 AC1 4개면 자리가 없다`() {
        val rows = memoRowsWithAds(memos(4))
        assertEquals(4, rows.size)
        assertTrue(rows.none { it is MemoListRow.Ad })
    }

    @Test
    fun `FR-AD-01 AC1 10개면 5번째와 10번째 뒤`() {
        val shape = memoRowsWithAds(memos(10)).shape()
        assertEquals(5, shape.indexOf("A0"))
        assertEquals(11, shape.indexOf("A1"))
        assertEquals(12, shape.size)
    }

    @Test
    fun `FR-AD-01 AC1 즐겨찾기 보기에도 같은 규칙`() {
        val favorites = MemoListUiState(
            loaded = true, view = MemoView.FAVORITES, items = memos(6), totalCount = 6, adUnitId = "unit",
        )
        assertEquals(listOf("M1", "M2", "M3", "M4", "M5", "A0", "M6"), favorites.rows.shape())

        val oldest = favorites.copy(view = MemoView.OLDEST)
        assertEquals(favorites.rows.shape(), oldest.rows.shape())
    }

    @Test
    fun `FR-AD-01 AC2 광고는 개수에 들어가지 않는다`() {
        val state = MemoListUiState(loaded = true, items = memos(10), totalCount = 10, adUnitId = "unit")
        assertEquals(10, state.totalCount)
        assertEquals(10, state.rows.filterIsInstance<MemoListRow.Memo>().size)
        // 광고 자리의 키는 메모 키와 겹치지 않는다
        assertEquals(state.rows.size, state.rows.map { it.key }.toSet().size)
    }

    @Test
    fun `FR-AD-04 AC4 광고가 꺼져 있으면 자리가 없다`() {
        val state = MemoListUiState(loaded = true, items = memos(10), totalCount = 10, adUnitId = null)
        assertEquals(10, state.rows.size)
        assertNull(state.adUnitId)
        assertTrue(state.rows.none { it is MemoListRow.Ad })
    }
}
