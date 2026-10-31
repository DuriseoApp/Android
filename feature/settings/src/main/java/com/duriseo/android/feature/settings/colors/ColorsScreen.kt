package com.duriseo.android.feature.settings.colors

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.Danger600
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.PersonMe
import com.duriseo.android.core.designsystem.theme.PersonPartner
import com.duriseo.android.core.designsystem.theme.PersonTogether
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.core.designsystem.theme.Warning50
import com.duriseo.android.core.designsystem.theme.Warning800
import com.duriseo.android.core.designsystem.theme.onPersonColor
import com.duriseo.android.feature.settings.R
import java.time.LocalDate

// SCR-SET-03 우리 색상 (FR-CAL-04). 수치는 보드 `ColorSettings.dc.html`. 토큰에 없는 값은 이름을 붙인 상수로 둔다
// (unit-3.md 확인 필요 14).

/** 앱바 [완료]: 높이 48, 좌우 14, 16/700 `brand-700` (21행) */
private val DoneHeight = 48.dp
private val DonePaddingHorizontal = 14.dp
private val BackButtonSize = 48.dp

/** 본문: 위 4·좌우 20·아래 28, 덩어리 사이 22 (24행) */
private val ContentTop = 4.dp
private val ContentBottom = 28.dp
private val BlockGap = 22.dp

/** 미리보기 패널: 안쪽 16, 모서리 20, 사이 12 (26행) */
private val PreviewPadding = 16.dp
private val PreviewGap = 12.dp

/** 미리보기 아바타 28, 서로 6 겹침, 바깥 링 2 (91행). 링까지 한 칸으로 그려서 칸 사이는 -(6 + 링 2개) = -10 */
private val PreviewAvatarSize = 28.dp
private val PreviewAvatarOverlap = (-10).dp
private val PreviewAvatarRing = 2.dp
private val PreviewAvatarFont = 12.sp

/** 한 주 달력: 안쪽 위아래 8, 모서리 14, 칸 안 사이 4 (32·34행). 요일 11, 숫자 14, 점 5(사이 3, 줄 높이 5) */
private val WeekPaddingVertical = 8.dp
private val WeekCellGap = 4.dp
private val WeekdayFont = 11.sp
private val WeekdayLineHeight = 14.sp
private val DayDotSize = 5.dp
private val DayDotGap = 3.dp

/** 일정 한 줄: 높이 40, 좌우 12, 모서리 12, 사이 10, 시간 칸 38 (39행) */
private val EventHeight = 40.dp
private val EventPaddingHorizontal = 12.dp
private val EventGap = 10.dp
private val EventTimeWidth = 38.dp
private val EventListGap = 6.dp

/** 일정 칩: 안쪽 위아래 2·왼쪽 6·오른쪽 8, 테두리 1, 점 8, 사이 5 (43행) */
private val ChipPaddingVertical = 2.dp
private val ChipPaddingStart = 6.dp
private val ChipPaddingEnd = 8.dp
private val ChipBorderWidth = 1.dp
private val ChipDotSize = 8.dp
private val ChipGap = 5.dp

/** 바꿀 대상 카드: 커플 높이 112(세로), 개인 높이 64(가로, 좌우 16), 사이 8, 모서리 16, 선택 테두리 2 (60·63행) */
private val TargetTitleGap = 10.dp
private val TargetHeightCouple = 112.dp
private val TargetHeightSolo = 64.dp
private val TargetGap = 8.dp
private val TargetInnerGap = 4.dp
private val TargetSoloGap = 12.dp
private val TargetSoloPadding = 16.dp
private val TargetBorderWidth = 1.dp
private val TargetSelectedBorderWidth = 2.dp
private val TargetCircleSize = 36.dp
private val TargetCircleFont = 14.sp

/** 팔레트: 위 제목과 사이 12, 6열, 행 사이 14, 색 원 44, 선택 링 안쪽 흰색 3 + 바깥 `ink-900` 2, 체크 20 (68·70·72행) */
private val PaletteGap = 12.dp
private val PaletteRowGap = 14.dp
private val SwatchSize = 44.dp
private val SwatchWhiteRing = 3.dp
private val SwatchInkRing = 2.dp
private val CheckSize = 20.dp
private const val PALETTE_COLUMNS = 6

/** 경고 배너: 안쪽 위아래 12·좌우 14, 모서리 12, 13/400 행간 1.6 (74행) */
private val WarningPaddingVertical = 12.dp
private val WarningPaddingHorizontal = 14.dp
private val WarningLineHeight = 20.8.sp

/** 아래 안내와 되돌리기: 사이 6, 버튼 높이 44 (77~79행) */
private val FooterGap = 6.dp
private val ResetHeight = 44.dp

/** 안내 문구 13/400 행간 1.6 = 20.8 (78행) */
private val NoteLineHeight = 20.8.sp

@Composable
fun ColorsRoute(
    padding: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ColorsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    ColorsScreen(
        state = state,
        padding = padding,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun ColorsScreen(
    state: ColorsUiState,
    padding: PaddingValues,
    onAction: (ColorsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(padding),
    ) {
        ColorsAppBar(onBack = onBack, onDone = { onAction(ColorsAction.DoneClicked) })
        if (state.loaded) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = DuriseoSpacing.Space5,
                        end = DuriseoSpacing.Space5,
                        top = ContentTop,
                        bottom = ContentBottom,
                    ),
                verticalArrangement = Arrangement.spacedBy(BlockGap),
            ) {
                PreviewPanel(state)
                TargetSection(state, onAction)
                PaletteSection(state, onAction)
                Column(verticalArrangement = Arrangement.spacedBy(FooterGap)) {
                    Text(
                        text = stringResource(if (state.isCouple) R.string.colors_note_couple else R.string.colors_note_solo),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Normal,
                            lineHeight = NoteLineHeight,
                        ),
                        color = Ink600,
                    )
                    Box(
                        modifier = Modifier
                            .height(ResetHeight)
                            .clickable(role = Role.Button) { onAction(ColorsAction.ResetClicked) },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = stringResource(R.string.colors_reset),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Ink600,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }
            }
        }
    }
}

/** 앱바: 왼쪽 뒤로, 가운데 "우리 색상" 17/700, 오른쪽 [완료] (`ColorSettings.dc.html` 18~21행) */
@Composable
private fun ColorsAppBar(onBack: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(horizontal = DuriseoSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(BackButtonSize)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_back),
                contentDescription = stringResource(DesignR.string.nav_back),
                modifier = Modifier.size(DuriseoSize.Icon),
                tint = Ink900,
            )
        }
        Text(
            text = stringResource(R.string.colors_title),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = Ink900,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .height(DoneHeight)
                .clickable(role = Role.Button, onClick = onDone)
                .padding(horizontal = DonePaddingHorizontal),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.colors_done),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Brand700,
            )
        }
    }
}

/** 키를 색으로 바꾼다. 팔레트에 없는 키면 [fallback]을 쓴다 (unit-3.md 확인 필요 18) */
private fun colorOfKey(key: String, fallback: Color): Color = DuriseoPickPalette.colorOf(key) ?: fallback

private fun fallbackOf(target: ColorTarget): Color = when (target) {
    ColorTarget.Me -> PersonMe
    ColorTarget.Partner -> PersonPartner
    ColorTarget.Together -> PersonTogether
}

@Composable
private fun colorNameOf(key: String): String {
    val labelRes = DuriseoPickPalette.colors.firstOrNull { it.key == key }?.labelRes ?: return ""
    return stringResource(labelRes)
}

@StringRes
private fun targetLabelRes(target: ColorTarget): Int = when (target) {
    ColorTarget.Me -> R.string.colors_target_me
    ColorTarget.Partner -> R.string.colors_target_partner
    ColorTarget.Together -> R.string.colors_target_together
}

@StringRes
private fun targetLetterRes(target: ColorTarget): Int = when (target) {
    ColorTarget.Me -> R.string.colors_letter_me
    ColorTarget.Partner -> R.string.colors_letter_partner
    ColorTarget.Together -> R.string.colors_letter_together
}

/** 사람 색 원 + 안의 글자(나·너·둘). 글자색은 배경 휘도로 정한다 (`onPersonColor`) */
@Composable
private fun LetterCircle(color: Color, letter: String, size: Dp, fontSize: TextUnit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, DuriseoShapes.Full),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = fontSize, fontWeight = FontWeight.Bold),
            color = onPersonColor(color),
        )
    }
}

@Composable
private fun PreviewPanel(state: ColorsUiState) {
    val me = colorOfKey(state.myColor, PersonMe)
    val partner = colorOfKey(state.partnerColor, PersonPartner)
    val together = colorOfKey(state.togetherColor, PersonTogether)
    val colorsOf = mapOf(ColorTarget.Me to me, ColorTarget.Partner to partner, ColorTarget.Together to together)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface100, DuriseoShapes.Xl)
            .padding(PreviewPadding),
        verticalArrangement = Arrangement.spacedBy(PreviewGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.colors_preview_label),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Ink600,
            )
            Row(
                modifier = Modifier.clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(PreviewAvatarOverlap),
            ) {
                AvatarWithRing(me, stringResource(R.string.colors_letter_me))
                if (state.isCouple) {
                    AvatarWithRing(partner, stringResource(R.string.colors_letter_partner))
                }
            }
        }
        WeekStrip(state, colorsOf)
        EventList(state, colorsOf)
    }
}

@Composable
private fun AvatarWithRing(color: Color, letter: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(PreviewAvatarSize + PreviewAvatarRing * 2)
            .background(Surface100, DuriseoShapes.Full),
        contentAlignment = Alignment.Center,
    ) {
        LetterCircle(color, letter, PreviewAvatarSize, PreviewAvatarFont)
    }
}

/** 날짜별로 찍히는 일정 점의 주인 (보드 `WEEK`, 일요일 0 ~ 토요일 6) */
private fun dotsOf(index: Int, isCouple: Boolean): List<ColorTarget> =
    if (isCouple) {
        when (index) {
            1 -> listOf(ColorTarget.Partner, ColorTarget.Together, ColorTarget.Me)
            3 -> listOf(ColorTarget.Partner)
            5 -> listOf(ColorTarget.Me)
            6 -> listOf(ColorTarget.Together)
            else -> emptyList()
        }
    } else {
        when (index) {
            1 -> listOf(ColorTarget.Me, ColorTarget.Me)
            4, 5 -> listOf(ColorTarget.Me)
            else -> emptyList()
        }
    }

@Composable
private fun WeekStrip(state: ColorsUiState, colorsOf: Map<ColorTarget, Color>) {
    val weekdays = stringArrayResource(R.array.colors_weekdays)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface0, MaterialTheme.shapes.medium)
            .padding(vertical = WeekPaddingVertical)
            .clearAndSetSemantics { },
    ) {
        state.weekDays.forEachIndexed { index, date ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WeekCellGap),
            ) {
                Text(
                    text = weekdays[index],
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = WeekdayFont,
                        lineHeight = WeekdayLineHeight,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = Ink500,
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (date == state.today) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (index == 0) Danger600 else Ink900,
                )
                Row(
                    modifier = Modifier.height(DayDotSize),
                    horizontalArrangement = Arrangement.spacedBy(DayDotGap),
                ) {
                    dotsOf(index, state.isCouple).forEach { owner ->
                        Box(
                            modifier = Modifier
                                .size(DayDotSize)
                                .background(colorsOf.getValue(owner), DuriseoShapes.Full),
                        )
                    }
                }
            }
        }
    }
}

private data class SampleEvent(
    val owner: ColorTarget,
    @StringRes val timeRes: Int,
    @StringRes val titleRes: Int,
)

private fun sampleEvents(isCouple: Boolean): List<SampleEvent> =
    if (isCouple) {
        listOf(
            SampleEvent(ColorTarget.Partner, R.string.colors_sample_time_dentist, R.string.colors_sample_dentist),
            SampleEvent(ColorTarget.Together, R.string.colors_sample_time_dinner, R.string.colors_sample_dinner),
            SampleEvent(ColorTarget.Me, R.string.colors_sample_memo, R.string.colors_sample_gifts),
        )
    } else {
        listOf(
            SampleEvent(ColorTarget.Me, R.string.colors_sample_time_pilates, R.string.colors_sample_pilates),
            SampleEvent(ColorTarget.Me, R.string.colors_sample_memo, R.string.colors_sample_gifts),
        )
    }

@Composable
private fun EventList(state: ColorsUiState, colorsOf: Map<ColorTarget, Color>) {
    Column(
        modifier = Modifier.clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(EventListGap),
    ) {
        sampleEvents(state.isCouple).forEach { event ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EventHeight)
                    .background(Surface0, MaterialTheme.shapes.small)
                    .padding(horizontal = EventPaddingHorizontal),
                horizontalArrangement = Arrangement.spacedBy(EventGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(event.timeRes),
                    modifier = Modifier.width(EventTimeWidth),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink600,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(event.titleRes),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .border(ChipBorderWidth, Line200, DuriseoShapes.Full)
                        .padding(start = ChipPaddingStart, end = ChipPaddingEnd, top = ChipPaddingVertical, bottom = ChipPaddingVertical),
                    horizontalArrangement = Arrangement.spacedBy(ChipGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(ChipDotSize)
                            .background(colorsOf.getValue(event.owner), DuriseoShapes.Full),
                    )
                    Text(
                        text = stringResource(targetLabelRes(event.owner)),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Ink900,
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetSection(state: ColorsUiState, onAction: (ColorsAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TargetTitleGap)) {
        Text(
            text = stringResource(R.string.colors_target_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            color = Ink600,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TargetGap)) {
            state.targets.forEach { target ->
                TargetCard(
                    target = target,
                    colorKey = state.colorKeyOf(target),
                    selected = state.target == target,
                    isCouple = state.isCouple,
                    onClick = { onAction(ColorsAction.TargetSelected(target)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TargetCard(
    target: ColorTarget,
    colorKey: String,
    selected: Boolean,
    isCouple: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    val borderWidth = if (selected) TargetSelectedBorderWidth else TargetBorderWidth
    val borderColor = if (selected) Ink900 else Line200
    val cardModifier = modifier
        .background(Surface0, shape)
        .border(borderWidth, borderColor, shape)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    val circle: @Composable () -> Unit = {
        LetterCircle(colorOfKey(colorKey, fallbackOf(target)), stringResource(targetLetterRes(target)), TargetCircleSize, TargetCircleFont)
    }
    val label: @Composable () -> Unit = {
        Text(
            text = stringResource(targetLabelRes(target)),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Ink900,
        )
    }
    val colorName: @Composable () -> Unit = {
        Text(
            text = colorNameOf(colorKey),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = Ink600,
        )
    }
    if (isCouple) {
        Column(
            modifier = cardModifier.height(TargetHeightCouple),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TargetInnerGap, Alignment.CenterVertically),
        ) {
            circle()
            label()
            colorName()
        }
    } else {
        Row(
            modifier = cardModifier
                .height(TargetHeightSolo)
                .padding(horizontal = TargetSoloPadding),
            horizontalArrangement = Arrangement.spacedBy(TargetSoloGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            circle()
            label()
            colorName()
        }
    }
}

@Composable
private fun PaletteSection(state: ColorsUiState, onAction: (ColorsAction) -> Unit) {
    val selectedKey = state.colorKeyOf(state.target)
    Column(verticalArrangement = Arrangement.spacedBy(PaletteGap)) {
        Text(
            text = stringResource(R.string.colors_palette_title, stringResource(targetLabelRes(state.target))),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            color = Ink600,
        )
        Column(verticalArrangement = Arrangement.spacedBy(PaletteRowGap)) {
            DuriseoPickPalette.colors.chunked(PALETTE_COLUMNS).forEach { rowColors ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowColors.forEach { pick ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Swatch(
                                color = pick.color,
                                name = stringResource(pick.labelRes),
                                selected = pick.key == selectedKey,
                                onClick = { onAction(ColorsAction.ColorPicked(pick.key)) },
                            )
                        }
                    }
                }
            }
        }
        if (state.hasDuplicate) {
            Text(
                text = stringResource(R.string.colors_duplicate_warning),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Warning50, MaterialTheme.shapes.small)
                    .padding(horizontal = WarningPaddingHorizontal, vertical = WarningPaddingVertical)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Normal,
                    lineHeight = WarningLineHeight,
                ),
                color = Warning800,
            )
        }
    }
}

/** 색 원 44. 선택되면 바깥에 흰 링 3 + `ink-900` 링 2가 둘러싸고 안에 체크가 보인다 (색만으로 뜻을 전하지 않는다) */
@Composable
private fun Swatch(color: Color, name: String, selected: Boolean, onClick: () -> Unit) {
    val selectedLabel = if (selected) stringResource(R.string.colors_swatch_selected, name) else name
    Box(
        modifier = Modifier
            .size(SwatchSize)
            .then(
                if (selected) {
                    Modifier.drawBehind {
                        val radius = size.minDimension / 2
                        drawCircle(Ink900, radius = radius + (SwatchWhiteRing + SwatchInkRing).toPx())
                        drawCircle(Surface0, radius = radius + SwatchWhiteRing.toPx())
                    }
                } else {
                    Modifier
                },
            )
            .background(color, DuriseoShapes.Full)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = selectedLabel },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(CheckSize),
                tint = onPersonColor(color),
            )
        }
    }
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 12)

private fun previewWeek(today: LocalDate): List<LocalDate> {
    val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
    return (0 until 7).map { sunday.plusDays(it.toLong()) }
}

private fun previewState(
    isCouple: Boolean = true,
    me: String = "pick-orange",
    partner: String = "pick-blue",
    together: String = "pick-purple",
    target: ColorTarget = ColorTarget.Me,
) = ColorsUiState(
    loaded = true,
    isCouple = isCouple,
    myColor = me,
    partnerColor = partner,
    togetherColor = together,
    target = target,
    today = PreviewToday,
    weekDays = previewWeek(PreviewToday),
)

@Composable
private fun PreviewColors(state: ColorsUiState) {
    DuriseoTheme {
        ColorsScreen(state = state, padding = PaddingValues(), onAction = {}, onBack = {})
    }
}

/** `SCR-SET-03.png` */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ColorsScreenCouplePreview() = PreviewColors(previewState())

/** 같은 색 경고 배너 (보드 `hasDup`): 나와 상대가 같은 주황 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ColorsScreenSameColorPreview() = PreviewColors(previewState(partner = "pick-orange"))

/** 상대 색을 고르는 중 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ColorsScreenPartnerTargetPreview() = PreviewColors(previewState(target = ColorTarget.Partner))

/** `SCR-SET-03--solo.png`: 개인 공간 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ColorsScreenSoloPreview() = PreviewColors(previewState(isCouple = false))
