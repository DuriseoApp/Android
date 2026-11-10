package com.duriseo.android.core.designsystem.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D-115 "화면 전체": 위층 호스트가 하단 탭까지 덮는다. 여러 화면이 알려도 덮개는 하나이고 모두 끝나야 걷힌다 */
class DuriseoLoadingHostStateTest {
    @Test
    fun `D-115 요청 중인 화면이 없으면 덮개가 없다`() {
        assertFalse(DuriseoLoadingHostState().active)
    }

    @Test
    fun `D-115 요청 중이면 덮개가 켜지고 끝나면 걷힌다`() {
        val host = DuriseoLoadingHostState()
        host.acquire()
        assertTrue(host.active)
        host.release()
        assertFalse(host.active)
    }

    @Test
    fun `D-115 두 화면이 동시에 요청 중이면 둘 다 끝나야 걷힌다`() {
        val host = DuriseoLoadingHostState()
        host.acquire()
        host.acquire()
        host.release()
        assertTrue(host.active)
        host.release()
        assertFalse(host.active)
    }

    @Test
    fun `D-115 알리지 않은 해제는 덮개를 켜지 않는다`() {
        val host = DuriseoLoadingHostState()
        host.release()
        assertFalse(host.active)
        host.acquire()
        assertTrue(host.active)
    }
}
