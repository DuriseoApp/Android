package com.duriseo.android.domain.appops

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppPopupResolverTest {

    private fun resolveAll(
        remote: AppOpsData = AppOpsData.Empty,
        server: AppOpsData? = null,
        hidden: HiddenTodayIds = HiddenTodayIds.None,
        forced: ForceUpdateSignal? = null,
    ) = AppPopupResolver.resolveAll(remote, server, ANDROID_34, NOW, hidden, forced)

    @Test
    fun `FR-APP-01 AC5 같은 id는 Remote Config 값을 쓴다`() {
        val remote = AppOpsData(listOf(notice("same", title = "RC 제목")), emptyList())
        val server = AppOpsData(listOf(notice("same", title = "서버 제목"), notice("only-server")), emptyList())

        val popups = resolveAll(remote, server)

        assertEquals(listOf("same", "only-server"), popups.map { it.id })
        assertEquals("RC 제목", popups.first().title)
    }

    @Test
    fun `D-178 순서로 하나를 고른다`() {
        val remote = AppOpsData(
            notices = listOf(notice("info-rc"), notice("block-rc", closable = false)),
            rules = listOf(rule("rec-rc"), rule("force-rc", mode = UpdateMode.FORCE)),
        )
        val server = AppOpsData(
            notices = listOf(notice("info-sv"), notice("block-sv", closable = false)),
            rules = listOf(rule("rec-sv"), rule("force-sv", mode = UpdateMode.FORCE)),
        )

        val popups = resolveAll(remote, server)

        // 강제 -> 앱을 막는 공지 -> 권장 -> 알리는 공지, 같은 종류는 Remote Config 순 -> 서버 순
        assertEquals(
            listOf("force-rc", "force-sv", "block-rc", "block-sv", "rec-rc", "rec-sv", "info-rc", "info-sv"),
            popups.map { it.id },
        )
        assertEquals("force-rc", AppPopupResolver.resolve(remote, server, ANDROID_34, NOW)?.id)
    }

    @Test
    fun `FR-APP-02 AC5 공지와 규칙의 같은 id는 서로 다른 것이다`() {
        val remote = AppOpsData(listOf(notice("same")), listOf(rule("same")))

        val popups = resolveAll(remote)

        assertEquals(listOf(PopupKind.RECOMMEND_UPDATE, PopupKind.INFO_NOTICE), popups.map { it.kind })
    }

    @Test
    fun `D-179 오늘 숨긴 id는 건너뛰고 다음 날 다시 고른다`() {
        val remote = AppOpsData(listOf(notice("a"), notice("b")), listOf(rule("r")))
        val hiddenToday = HiddenTodayIds(notices = setOf("a"), rules = setOf("r"))

        assertEquals(listOf("b"), resolveAll(remote, hidden = hiddenToday).map { it.id })
        // 다음 날: 저장소가 지난 기록을 정리해 빈 값을 주면 다시 고른다
        assertEquals(listOf("r", "a", "b"), resolveAll(remote, hidden = HiddenTodayIds.None).map { it.id })
        // 공지에서 숨긴 id는 규칙에는 영향이 없다
        assertEquals(listOf("r", "b"), resolveAll(remote, hidden = HiddenTodayIds(notices = setOf("a"))).map { it.id })
    }

    @Test
    fun `FR-APP-02 AC4 강제는 오늘 하루 안 보기와 상관없다`() {
        val remote = AppOpsData(
            notices = listOf(notice("block", closable = false)),
            rules = listOf(rule("force", mode = UpdateMode.FORCE)),
        )
        val hidden = HiddenTodayIds(notices = setOf("block"), rules = setOf("force"))

        assertEquals(listOf("force", "block"), resolveAll(remote, hidden = hidden).map { it.id })
    }

    @Test
    fun `FR-APP-02 AC6 426 신호는 맨 앞이고 같은 규칙은 하나로 본다`() {
        val remote = AppOpsData(listOf(notice("block", closable = false)), listOf(rule("force", mode = UpdateMode.FORCE)))
        val signal = ForceUpdateSignal("force", "제목", null, PopupImage.None, null)

        val popups = resolveAll(remote, forced = signal)

        assertEquals(listOf("force", "block"), popups.map { it.id })
        assertEquals(PopupKind.FORCE_UPDATE, popups.first().kind)
        assertEquals(1, popups.count { it.kind == PopupKind.FORCE_UPDATE })
    }

    @Test
    fun `FR-APP-01 AC6 두 곳 모두 없으면 팝업이 없다`() {
        assertNull(AppPopupResolver.resolve(AppOpsData.Empty, null, ANDROID_34, NOW))
    }
}
