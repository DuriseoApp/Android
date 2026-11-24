package com.duriseo.android.feature.calendar.tutorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 튜토리얼 5단계와 문구 (FR-CAL-06 AC2, SCREENS.md SCR-CAL-01 "튜토리얼") */
class CalendarTutorialStepTest {

    /** 단위 테스트의 작업 폴더는 이 모듈(`feature/calendar`)이다 */
    private val strings = File("src/main/res/values/strings.xml").readText()

    private fun stringValue(name: String): String {
        val match = Regex("""<string name="$name">(.*?)</string>""").find(strings)
        assertTrue("strings.xml에 $name 이 없어요", match != null)
        return match!!.groupValues[1]
    }

    @Test
    fun `FR-CAL-06 AC2 튜토리얼은 5단계이고 순서는 달력 추가 버튼 기념일 링크 메모 탭 전체 메뉴 버튼이다`() {
        assertEquals(
            listOf("Calendar", "Fab", "Anniversary", "MemoTab", "MenuButton"),
            CalendarTutorialStep.entries.map { it.name },
        )
        assertEquals(CalendarTutorialStep.Calendar, CalendarTutorialStep.first)
        assertEquals(CalendarTutorialStep.Fab, CalendarTutorialStep.Calendar.next())
        assertNull(CalendarTutorialStep.MenuButton.next())
    }

    @Test
    fun `FR-CAL-06 AC2 단계 문구는 PRD 그대로다`() {
        assertEquals(
            "옆으로 쓸면 다른 달로 가고, 아래로 쓸어 내리면 달력을 크게 볼 수 있어요.",
            stringValue("calendar_tutorial_calendar"),
        )
        assertEquals("여기를 누르면 일정이나 메모를 추가할 수 있어요.", stringValue("calendar_tutorial_fab"))
        assertEquals("함께한 날과 기념일은 여기서 볼 수 있어요.", stringValue("calendar_tutorial_anniversary_couple"))
        assertEquals("기념일은 여기서 볼 수 있어요.", stringValue("calendar_tutorial_anniversary_solo"))
        assertEquals("메모는 여기서 모아 볼 수 있어요.", stringValue("calendar_tutorial_memo_tab"))
        assertEquals("프로필과 알림 설정은 전체 메뉴에서 바꿀 수 있어요.", stringValue("calendar_tutorial_menu"))
    }
}
