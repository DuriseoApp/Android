package com.duriseo.android.core.common.update

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppForegroundTrackerTest {

    @Test
    fun `D-178 백그라운드를 거치지 않은 포그라운드는 돌아옴이 아니다 화면 회전`() = runTest {
        val tracker = AppForegroundTracker()
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { tracker.returns.collect { count++ } }

        tracker.onForeground() // 앱 시작이나 회전: 백그라운드가 없었다
        tracker.onForeground()

        assertEquals(0, count)
    }

    @Test
    fun `D-178 백그라운드를 거쳐 돌아오면 한 번만 알린다`() = runTest {
        val tracker = AppForegroundTracker()
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { tracker.returns.collect { count++ } }

        tracker.onBackground()
        tracker.onForeground()
        tracker.onForeground() // 이어서 만들어지는 Activity

        assertEquals(1, count)
    }
}
