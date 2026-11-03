package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink300
import com.duriseo.android.core.designsystem.theme.Ink400
import com.duriseo.android.core.designsystem.theme.PersonMe
import com.duriseo.android.core.designsystem.theme.PersonPartner
import com.duriseo.android.core.designsystem.theme.PersonTogether

// 전체 메뉴 프로필 카드와 연결 정보의 커플 표시 (보드 `Menu.dc.html` 52·79·87행, `ConnectionInfo.dc.html`).
// 보드 수치는 토큰에 없어 이름 붙인 상수로 둔다.

/** "함께" 표시의 그림 틀: viewBox `22 32 64 44`에서 두 원 중심 (44, 54)·(64, 54), 반지름 20 */
private const val MarkViewWidth = 64f
private const val MarkViewHeight = 44f
private const val MarkLeftCenterX = 44f - 22f
private const val MarkRightCenterX = 64f - 22f
private const val MarkCenterY = 54f - 32f
private const val MarkRadius = 20f

/** 점선: 선 두께 2.5, 점 2 · 간격 8, 둥근 끝 (`stroke-dasharray="2 8"`) */
private val DashStrokeWidth = 2.5.dp
private val DashOn = 2.dp
private val DashOff = 8.dp

/** 점선 원 테두리 두께 2. 보드는 CSS `dashed`(브라우저가 정하는 간격)라 점 6 · 간격 4로 비슷하게 그린다 (이미지와 맞는지는 확인 필요) */
private val RingStrokeWidth = 2.dp
private val RingDash = 6.dp
private val RingGap = DuriseoSpacing.Space1

/** "아직 없음" 원 안의 더하기 아이콘: 지름 72에서 26, 56에서 22 */
private val PlusIconInLarge = 26.dp
private val PlusIconInMedium = 22.dp

/**
 * 나·상대가 겹친 "함께" 표시. 두 원이 겹친 부분이 함께 색이다.
 * 글자 없이 그림만 그리므로 의미는 곁에 있는 글자("D+N", "연결됨")가 맡는다. 접근성으로는 읽히지 않는다.
 * 모르는 팔레트 키는 기본 사람 색으로 그린다 (API.md 3.2 앱 동작).
 *
 * @param width 보드는 프로필 카드 34, 연결 정보 40. 높이도 같이 정해서 넘긴다 (22, 28)
 */
@Composable
fun DuriseoTogetherMark(
    width: Dp,
    height: Dp,
    meColorKey: String,
    partnerColorKey: String,
    togetherColorKey: String,
    modifier: Modifier = Modifier,
) {
    val me = DuriseoPickPalette.colorOf(meColorKey) ?: PersonMe
    val partner = DuriseoPickPalette.colorOf(partnerColorKey) ?: PersonPartner
    val together = DuriseoPickPalette.colorOf(togetherColorKey) ?: PersonTogether
    Canvas(modifier = modifier.size(width, height).clearAndSetSemantics { }) {
        // SVG 기본값처럼 비율을 지켜 가운데에 맞춘다
        val scale = minOf(size.width / MarkViewWidth, size.height / MarkViewHeight)
        val originX = (size.width - MarkViewWidth * scale) / 2f
        val originY = (size.height - MarkViewHeight * scale) / 2f
        fun circle(centerX: Float): Path = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    center = Offset(originX + centerX * scale, originY + MarkCenterY * scale),
                    radius = MarkRadius * scale,
                ),
            )
        }
        val left = circle(MarkLeftCenterX)
        val right = circle(MarkRightCenterX)
        drawPath(left, me)
        drawPath(right, partner)
        drawPath(Path.combine(PathOperation.Intersect, left, right), together)
    }
}

/** 두 사람 사이의 점선 (혼자일 때 프로필 카드·연결 정보). 높이는 선 두께만큼 */
@Composable
fun DuriseoDashedLink(width: Dp, modifier: Modifier = Modifier, color: Color = Ink300) {
    Canvas(modifier = modifier.size(width, DashStrokeWidth + DashStrokeWidth).clearAndSetSemantics { }) {
        val y = size.height / 2f
        val inset = DashStrokeWidth.toPx() / 2f
        drawLine(
            color = color,
            start = Offset(inset, y),
            end = Offset(size.width - inset, y),
            strokeWidth = DashStrokeWidth.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx())),
        )
    }
}

/**
 * 아직 연결되지 않은 상대 자리: 점선 원 안에 더하기. 지름은 [size] (프로필 카드 72, 연결 정보 56).
 * 이름 자리의 "아직 없음"은 부르는 쪽이 글자로 둔다.
 */
@Composable
fun DuriseoEmptyPartnerSlot(size: DuriseoAvatarSize, modifier: Modifier = Modifier) {
    val iconSize = if (size == DuriseoAvatarSize.Large) PlusIconInLarge else PlusIconInMedium
    Box(
        modifier = modifier
            .size(size.diameter)
            .drawBehind {
                val stroke = RingStrokeWidth.toPx()
                drawCircle(
                    color = Ink300,
                    radius = (this.size.minDimension - stroke) / 2f,
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(RingDash.toPx(), RingGap.toPx())),
                    ),
                )
            }
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = Ink400,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DuriseoCoupleMarksPreview() {
    DuriseoTheme {
        Column(
            modifier = Modifier.padding(DuriseoSpacing.Space4),
            verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space4),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space4)) {
                DuriseoTogetherMark(34.dp, 22.dp, "pick-orange", "pick-blue", "pick-purple")
                DuriseoTogetherMark(40.dp, 28.dp, "pick-rose", "unknown-key", "pick-green")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DuriseoEmptyPartnerSlot(DuriseoAvatarSize.Large)
                DuriseoDashedLink(48.dp)
                DuriseoEmptyPartnerSlot(DuriseoAvatarSize.Medium)
            }
        }
    }
}
