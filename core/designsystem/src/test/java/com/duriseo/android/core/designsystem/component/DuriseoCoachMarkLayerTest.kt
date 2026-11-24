package com.duriseo.android.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** 코치 마크 레이어의 누름·뒤로 가기·위치 없음 동작 (FR-CAL-06 AC3, SCREENS.md SCR-CAL-01 "튜토리얼" 동작) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DuriseoCoachMarkLayerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var next = 0
    private var skip = 0

    /** 화면 아래쪽 반(Robolectric 기본 화면 320x470)에 있는 대상이라 말풍선은 구멍 위에 뜬다 */
    private val target = Rect(100f, 300f, 220f, 360f)

    private fun show(target: Rect?) {
        composeRule.setContent {
            DuriseoTheme {
                DuriseoCoachMarkLayer(
                    target = target,
                    holeShape = DuriseoShapes.Fab,
                    message = MESSAGE,
                    stepNumber = 2,
                    stepCount = 5,
                    onNext = { next += 1 },
                    onSkip = { skip += 1 },
                )
            }
        }
    }

    @Test
    fun `FR-CAL-06 AC3 구멍 안을 눌러도 다음 단계로 넘어간다`() {
        show(target)

        composeRule.onNode(isDialog()).performTouchInput { click(Offset(160f, 330f)) }

        assertEquals(1, next)
        assertEquals(0, skip)
    }

    @Test
    fun `FR-CAL-06 AC3 구멍 바깥 아무 곳이나 눌러도 다음 단계로 넘어간다`() {
        show(target)

        composeRule.onNode(isDialog()).performTouchInput { click(Offset(5f, 5f)) }

        assertEquals(1, next)
        assertEquals(0, skip)
    }

    @Test
    fun `FR-CAL-06 AC3 말풍선의 설명 문구와 진행 표시를 눌러도 다음 단계로 넘어간다`() {
        show(target)

        composeRule.onNodeWithText(MESSAGE).performClick()
        composeRule.onNodeWithText("2/5").performClick()

        assertEquals(2, next)
        assertEquals(0, skip)
    }

    @Test
    fun `FR-CAL-06 AC3 건너뛰기를 누르면 다음으로 넘어가지 않고 바로 닫는다`() {
        show(target)

        composeRule.onNodeWithText("건너뛰기").performClick()

        assertEquals(1, skip)
        assertEquals(0, next)
    }

    @Test
    fun `FR-CAL-06 AC3 뒤로 가기는 바로 닫는다`() {
        show(target)
        composeRule.waitForIdle()

        composeRule.runOnUiThread { ShadowDialog.getLatestDialog().onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(1, skip)
        assertEquals(0, next)
    }

    @Test
    fun `FR-CAL-06 AC2 말풍선에 진행 표시와 설명 문구와 건너뛰기가 보인다`() {
        show(target)

        composeRule.onNodeWithText("2/5").assertIsDisplayed()
        composeRule.onNodeWithText(MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText("건너뛰기").assertIsDisplayed()
    }

    @Test
    fun `FR-CAL-06 접근성 건너뛰기는 설명 문구와 합쳐지지 않는 독립 버튼이다`() {
        show(target)

        composeRule.onNodeWithText("건너뛰기")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            // 합쳐진 노드라면 문구가 함께 들어 있다: 건너뛰기 노드의 글자는 건너뛰기뿐이다
            .assert(SemanticsMatcher("글자는 건너뛰기뿐") { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } == listOf("건너뛰기")
            })
    }

    @Test
    fun `FR-CAL-06 대상 위치를 모르면 아무것도 그리지 않는다`() {
        show(null)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTextCount(MESSAGE, 0)
        composeRule.onAllNodesWithTextCount("건너뛰기", 0)
    }

    @Test
    fun `FR-CAL-06 대상 위치가 비어 있으면(크기 0) 그리지 않는다`() {
        show(Rect(100f, 300f, 100f, 300f))
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTextCount(MESSAGE, 0)
        composeRule.onAllNodesWithTextCount("건너뛰기", 0)
    }

    private fun ComposeContentTestRule.onAllNodesWithTextCount(text: String, expected: Int) {
        assertEquals(expected, onAllNodesWithText(text).fetchSemanticsNodes().size)
    }

    private companion object {
        /** 견본 글자라 앱 문구가 아니다 */
        const val MESSAGE = "Coach mark sample message"
    }
}
