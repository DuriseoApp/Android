package com.duriseo.android.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** FR-AD-04 AC1: 받기 전·실패는 숨기고, 받은 광고는 화면에 있는 동안 바꾸지 않는다. */
class NativeAdSlotStateTest {

    private class FakeAd(val name: String) {
        var destroyed = false
    }

    /** 요청을 받아 두었다가 테스트가 원할 때 결과를 돌려준다 */
    private class ManualRequester : AdRequester<FakeAd> {
        val pending = mutableListOf<Pair<Int, (FakeAd?) -> Unit>>()
        var requestCount = 0
        override fun request(slot: Int, onResult: (FakeAd?) -> Unit) {
            requestCount++
            pending += slot to onResult
        }
    }

    private fun slots(requester: ManualRequester) =
        NativeAdSlots(requester) { ad: FakeAd -> ad.destroyed = true }

    @Test
    fun `FR-AD-04 AC1 받기 전과 실패면 숨긴다`() {
        val requester = ManualRequester()
        val slots = slots(requester)

        // 받기 전: 자리 없음 (로딩·빈 틀 없음)
        assertTrue(slots.ads.value.isEmpty())
        slots.request(0)
        assertTrue(slots.ads.value.isEmpty())

        // 실패: 그대로 없음
        requester.pending[0].second(null)
        assertNull(slots.ads.value[0])
        assertTrue(slots.ads.value.isEmpty())
    }

    @Test
    fun `FR-AD-04 AC1 받은 광고는 화면에 있는 동안 바꾸지 않는다`() {
        val requester = ManualRequester()
        val slots = slots(requester)
        val first = FakeAd("first")
        val second = FakeAd("second")

        slots.request(0)
        requester.pending[0].second(first)
        assertEquals(first, slots.ads.value[0])

        // 같은 자리를 다시 요청해도 새로 받지 않는다
        slots.request(0)
        assertEquals(1, requester.requestCount)

        // 늦게 온 다른 광고는 바꾸지 않고 버린다
        requester.pending[0].second(second)
        assertEquals(first, slots.ads.value[0])
        assertTrue(second.destroyed)
        assertFalse(first.destroyed)
    }

    @Test
    fun `실패한 자리는 다시 요청하지 않는다`() {
        val requester = ManualRequester()
        val slots = slots(requester)

        slots.request(0)
        requester.pending[0].second(null)
        slots.request(0)

        assertEquals(1, requester.requestCount)
    }

    @Test
    fun `자리마다 다른 광고를 받는다`() {
        val requester = ManualRequester()
        val slots = slots(requester)
        val a = FakeAd("a")
        val b = FakeAd("b")

        slots.request(0)
        slots.request(1)
        requester.pending[0].second(a)
        requester.pending[1].second(b)

        assertEquals(mapOf(0 to a, 1 to b), slots.ads.value)
    }

    @Test
    fun `화면을 떠나면 받은 광고를 destroy하고 늦게 온 광고도 버린다`() {
        val requester = ManualRequester()
        val slots = slots(requester)
        val a = FakeAd("a")
        val late = FakeAd("late")

        slots.request(0)
        slots.request(1)
        requester.pending[0].second(a)
        slots.clear()

        assertTrue(a.destroyed)
        assertTrue(slots.ads.value.isEmpty())

        requester.pending[1].second(late)
        assertTrue(late.destroyed)
        assertTrue(slots.ads.value.isEmpty())

        // 떠난 뒤에는 요청하지 않는다
        slots.request(2)
        assertEquals(2, requester.requestCount)
    }
}
