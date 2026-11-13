package com.duriseo.android.feature.memo.list

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoFabPositionTest {

    @Test
    fun `D-126 SCR-MEMO-01 스낵바가 없으면 FAB은 탭 위 20이다 (바닥에서 100)`() {
        assertEquals(20.dp, memoFabBottom(snackbarVisible = false))
    }

    @Test
    fun `D-126 SCR-MEMO-01 스낵바가 떠 있으면 FAB은 스낵바 위 16이다 (바닥에서 161)`() {
        // 탭 80 + 81 = 161
        assertEquals(81.dp, memoFabBottom(snackbarVisible = true))
    }
}
