package com.duriseo.android.feature.calendar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarFoldTest {
    private val range = 200f
    private val fling = 600f

    @Test
    fun `D-163 끄는 이동량만큼 progress가 따라가고 0에서 1로 자른다`() {
        assertEquals(0.25f, progressAfterDrag(0f, 50f, range), 0.0001f)
        assertEquals(0.5f, progressAfterDrag(0.75f, -50f, range), 0.0001f)
        assertEquals(1f, progressAfterDrag(0.9f, 100f, range), 0f)
        assertEquals(0f, progressAfterDrag(0.1f, -100f, range), 0f)
    }

    @Test
    fun `D-163 범위가 0이면 progress는 그대로다`() {
        assertEquals(0.3f, progressAfterDrag(0.3f, 50f, 0f), 0f)
    }

    @Test
    fun `D-163 손을 떼면 0점5 기준 가까운 쪽으로 맞춘다`() {
        assertTrue(settleTarget(0.5f, 0f, fling))
        assertTrue(settleTarget(0.8f, 100f, fling))
        assertFalse(settleTarget(0.49f, 0f, fling))
        assertFalse(settleTarget(0.2f, -100f, fling))
    }

    @Test
    fun `D-163 튕긴 속도가 기준 이상이면 위치와 상관없이 그 방향으로 맞춘다`() {
        assertTrue(settleTarget(0.1f, 600f, fling))
        assertFalse(settleTarget(0.9f, -600f, fling))
    }

    @Test
    fun `D-163 점은 앞쪽 절반에 사라지고 막대는 뒤쪽 절반에 나타나 동시에 보이지 않는다`() {
        assertEquals(1f, foldDotsAlpha(0f), 0f)
        assertEquals(0f, foldDotsAlpha(0.5f), 0f)
        assertEquals(0f, foldBarsAlpha(0.5f), 0f)
        assertEquals(1f, foldBarsAlpha(1f), 0f)
        assertEquals(0.5f, foldDotsAlpha(0.25f), 0.0001f)
        assertEquals(0.5f, foldBarsAlpha(0.75f), 0.0001f)
    }

    @Test
    fun `D-163 주 수는 일요일 시작 기준으로 센다`() {
        assertEquals(5, weekCount(YearMonth.of(2026, 10)))
        assertEquals(6, weekCount(YearMonth.of(2026, 8)))
        assertEquals(4, weekCount(YearMonth.of(2026, 2)))
    }

    /** progress와 소비량을 흉내 내는 가짜: dragBy가 CalendarFoldState와 같은 규칙을 쓴다 */
    private class Fake(var progress: Float, val range: Float) {
        var released: Float? = null
        val connection = FoldNestedScrollConnection(
            progress = { progress },
            dragBy = { dy ->
                val before = progress
                progress = progressAfterDrag(before, dy, range)
                (progress - before) * range
            },
            onRelease = { released = it },
        )
    }

    @Test
    fun `D-163 펼친 상태에서 목록을 위로 끌면 스크롤 전에 progress를 줄이며 소비하고 남은 양은 목록에 준다`() {
        val fake = Fake(progress = 1f, range = 200f)
        val first = fake.connection.onPreScroll(Offset(0f, -50f), NestedScrollSource.UserInput)
        assertEquals(-50f, first.y, 0.001f)
        assertEquals(0.75f, fake.progress, 0.0001f)
        val rest = fake.connection.onPreScroll(Offset(0f, -250f), NestedScrollSource.UserInput)
        assertEquals(-150f, rest.y, 0.001f)
        assertEquals(0f, fake.progress, 0f)
        // 0이 되면 이동량은 목록 스크롤 몫이다
        assertEquals(Offset.Zero, fake.connection.onPreScroll(Offset(0f, -30f), NestedScrollSource.UserInput))
    }

    @Test
    fun `D-163 접힌 상태에서 목록이 맨 위일 때 남은 아래 이동량으로 progress를 늘린다`() {
        val fake = Fake(progress = 0f, range = 200f)
        val used = fake.connection.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.UserInput)
        assertEquals(100f, used.y, 0.001f)
        assertEquals(0.5f, fake.progress, 0.0001f)
    }

    @Test
    fun `D-163 끄는 도중 방향을 바꿔도 progress가 따라간다`() {
        val fake = Fake(progress = 0f, range = 200f)
        fake.connection.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.UserInput)
        fake.connection.onPreScroll(Offset(0f, -40f), NestedScrollSource.UserInput)
        assertEquals(0.3f, fake.progress, 0.0001f)
    }

    @Test
    fun `D-163 이미 펼친 상태에서 시작한 아래 끌기는 소비하지 않고 새로고침에 맡긴다`() {
        val fake = Fake(progress = 1f, range = 200f)
        assertEquals(Offset.Zero, fake.connection.onPostScroll(Offset.Zero, Offset(0f, 80f), NestedScrollSource.UserInput))
    }

    @Test
    fun `D-163 접힌 상태에서 위로 끄는 건 목록 스크롤 그대로다`() {
        val fake = Fake(progress = 0f, range = 200f)
        assertEquals(Offset.Zero, fake.connection.onPreScroll(Offset(0f, -100f), NestedScrollSource.UserInput))
        assertEquals(Offset.Zero, fake.connection.onPostScroll(Offset.Zero, Offset(0f, -100f), NestedScrollSource.UserInput))
    }

    @Test
    fun `D-163 progress를 움직인 끌기만 손을 뗄 때 맞추고 속도를 소비한다`() {
        val fake = Fake(progress = 0f, range = 200f)
        runBlocking {
            // 목록만 튕긴 경우: 맞추지 않는다
            assertEquals(Velocity.Zero, fake.connection.onPreFling(Velocity(0f, 900f)))
            assertEquals(null, fake.released)
            fake.connection.onPostScroll(Offset.Zero, Offset(0f, 40f), NestedScrollSource.UserInput)
            assertEquals(Velocity(0f, 900f), fake.connection.onPreFling(Velocity(0f, 900f)))
            assertEquals(900f, fake.released)
        }
    }
}
