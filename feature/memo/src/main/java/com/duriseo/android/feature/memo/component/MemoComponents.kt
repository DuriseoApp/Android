package com.duriseo.android.feature.memo.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.format.memoDateChipText
import com.duriseo.android.core.designsystem.format.writtenAtText
import com.duriseo.android.core.designsystem.theme.Brand50
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Favorite600
import com.duriseo.android.core.designsystem.theme.Ink400
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink700
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.core.designsystem.theme.onPersonColor
import com.duriseo.android.feature.memo.R

// 핀 카드·메모 행·메모 날짜 칩·핀/별 버튼: SCR-MEMO-01·02·03이 같이 쓰는 부품. 수치는 보드 `Memo.dc.html`, `PinnedAll.dc.html`, `MemoDetail.dc.html`.
// 토큰에 없는 보드 수치는 이름을 붙인 상수로 둔다.

/** 핀 카드: 안쪽 위 14·오른쪽 6·아래 16·왼쪽 16, 모서리 20, 줄 사이 6 (`Memo.dc.html` 35행) */
private val CardPaddingTop = 14.dp
private val CardPaddingEnd = 6.dp
private val CardPaddingBottom = 16.dp
private val CardGap = 6.dp
private val CardBodyEndPadding = 10.dp
private val CardBodyGap = 4.dp

/** 카드 위 줄 글자 12 (`caption`은 12/16 500), 아바타 지름 24(`size-avatar-sm`)에 글자 11/700 */
private val AvatarFontSize = 11.sp
private val AvatarGap = DuriseoSpacing.Space2

/** 카드 미리보기 14/1.6 (22.4) */
private val CardPreviewLineHeight = 22.4.sp

/** 카드 제목 17/1.4 (23.8) */
private val CardTitleLineHeight = 23.8.sp

/** 메모 행: 위아래 12, 글줄 사이 3, 글과 버튼 사이 2 (`Memo.dc.html` 82·83행) */
private val RowPaddingVertical = DuriseoSpacing.Space3
private val RowLineGap = 3.dp
private val RowButtonGap = 2.dp
private val DotSize = 7.dp
private val DotGap = 6.dp

/** 메모 날짜 칩: 높이 22, 안쪽 좌우 8, 달력 아이콘 12, 사이 4 / 상세는 높이 28, 좌우 10, 아이콘 14, 사이 5 */
private val ChipHeight = 22.dp
private val ChipPaddingHorizontal = DuriseoSpacing.Space2
private val ChipIconSize = 12.dp
private val ChipGap = 4.dp
private val ChipLargeHeight = 28.dp
private val ChipLargePaddingHorizontal = 10.dp
private val ChipLargeIconSize = 14.dp
private val ChipLargeGap = 5.dp

/** 핀·별 아이콘 20, 터치 44 (`Memo.dc.html` 41~45행) */
private val ToggleIconSize = 20.dp

/** 작성자 닉네임 앞 사람 색 [ME]·[PARTNER]의 색. 사람 색은 설정에서 바뀌므로 테마에서 읽는다 */
@Composable
internal fun MemoAuthor.color(): Color =
    if (this == MemoAuthor.ME) DuriseoTheme.personColors.me else DuriseoTheme.personColors.partner

/** 핀 카드 맨 앞 작성자 아바타: 사람 색 원 24에 "나"·"너". 글자색은 배경 상대휘도로 정한다 (SCREENS 공통 "아바타 글자색") */
@Composable
internal fun MemoAuthorAvatar(author: MemoAuthor, modifier: Modifier = Modifier) {
    val color = author.color()
    Box(
        modifier = modifier
            .size(DuriseoSize.AvatarSm)
            .background(color, DuriseoShapes.Full)
            // 닉네임이 옆에 같이 읽히므로 아바타는 읽지 않는다
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(if (author == MemoAuthor.ME) R.string.memo_author_me else R.string.memo_author_partner),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = AvatarFontSize, fontWeight = FontWeight.Bold),
            color = onPersonColor(color),
        )
    }
}

/** 사람 색 점 7. 색만으로 뜻을 전하지 않도록 닉네임이 바로 옆에 있다 (FR-CAL-04) */
@Composable
internal fun MemoAuthorDot(author: MemoAuthor, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(DotSize).background(author.color(), DuriseoShapes.Full).clearAndSetSemantics { })
}

/**
 * 핀 버튼. 켜짐은 `brand-500` 채움, 꺼짐은 [offTint] 선 (design-system "아이콘"). 접근성 문구는 지금 상태의 반대 동작이다
 * ("고정 해제"·"상단에 고정", 보드 `Memo.dc.html` 160행).
 */
@Composable
internal fun PinToggleButton(
    pinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    offTint: Color = Ink400,
    iconSize: Dp = ToggleIconSize,
    height: Dp = DuriseoSize.Touch,
) {
    val description = stringResource(if (pinned) R.string.memo_pin_on_description else R.string.memo_pin_off_description)
    Box(
        modifier = modifier
            .size(width = DuriseoSize.Touch, height = height)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (pinned) DesignR.drawable.ic_pin_filled else DesignR.drawable.ic_pin),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = if (pinned) Brand500 else offTint,
        )
    }
}

/** 즐겨찾기 버튼. 켜짐은 `favorite-600` 채움, 꺼짐은 [offTint] 선. 접근성 문구는 "즐겨찾기 해제"·"즐겨찾기" (보드 `Memo.dc.html` 159행) */
@Composable
internal fun FavoriteToggleButton(
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    offTint: Color = Ink400,
    iconSize: Dp = ToggleIconSize,
    height: Dp = DuriseoSize.Touch,
) {
    val description = stringResource(if (favorite) R.string.memo_favorite_on_description else R.string.memo_favorite_off_description)
    Box(
        modifier = modifier
            .size(width = DuriseoSize.Touch, height = height)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (favorite) DesignR.drawable.ic_star_filled else DesignR.drawable.ic_star),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = if (favorite) Favorite600 else offTint,
        )
    }
}

/**
 * 메모 날짜 칩: 달력 아이콘 + "10월 17일 (토)". 올해가 아니면 연도가 붙고 높이·글자 크기는 그대로, 폭만 늘어난다 (D-129).
 * 접근성 문구는 "메모 날짜 10월 17일 (토)".
 *
 * @param background 카드 위에서는 `surface-0`, 흰 면 위에서는 `surface-100` (design-system "메모 날짜 칩")
 * @param large 상세 화면: 높이 28, 글자 13
 */
@Composable
internal fun MemoDateBadge(
    chip: com.duriseo.android.domain.format.MemoDateChip,
    background: Color,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val text = memoDateChipText(chip)
    val description = stringResource(R.string.memo_date_description, text)
    Row(
        modifier = modifier
            .height(if (large) ChipLargeHeight else ChipHeight)
            .background(background, DuriseoShapes.Xs)
            .padding(horizontal = if (large) ChipLargePaddingHorizontal else ChipPaddingHorizontal)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (large) ChipLargeGap else ChipGap),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_calendar),
            contentDescription = null,
            modifier = Modifier.size(if (large) ChipLargeIconSize else ChipIconSize),
            tint = Ink700,
        )
        Text(
            text = text,
            style = (if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium)
                .copy(fontWeight = FontWeight.SemiBold),
            color = Ink700,
            maxLines = 1,
        )
    }
}

/**
 * 핀 카드 (SCR-MEMO-01 고정 영역, SCR-MEMO-02). `brand-50` 면, 작성자 아바타·닉네임·작성 시각(D-129, unit-6.md 확인 필요 1), 핀·별 버튼,
 * 제목, 미리보기 2줄, 메모 날짜 칩. 본문을 누르면 상세다.
 *
 * @param onFavoriteClick null이면 별 버튼이 없다 (SCR-MEMO-02는 핀 버튼만 있다, `PinnedAll.dc.html` 34행)
 */
@Composable
internal fun PinCard(
    item: MemoItemUi,
    onOpen: () -> Unit,
    onPinClick: () -> Unit,
    onFavoriteClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brand50, DuriseoShapes.Xl)
            .padding(start = DuriseoSpacing.Space4, top = CardPaddingTop, end = CardPaddingEnd, bottom = CardPaddingBottom),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AvatarGap),
            ) {
                MemoAuthorAvatar(item.author)
                Text(
                    text = item.nickname + " · " + writtenAtText(item.writtenAt),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = Ink600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row {
                PinToggleButton(pinned = true, onClick = onPinClick)
                if (onFavoriteClick != null) FavoriteToggleButton(favorite = item.isFavorite, onClick = onFavoriteClick)
            }
        }
        Column(
            modifier = Modifier.padding(end = CardBodyEndPadding),
            verticalArrangement = Arrangement.spacedBy(CardBodyGap),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen),
                verticalArrangement = Arrangement.spacedBy(CardBodyGap),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = CardTitleLineHeight),
                    color = Ink900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.preview.isNotEmpty()) {
                    Text(
                        text = item.preview,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = CardPreviewLineHeight),
                        color = Ink700,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item.memoDate?.let { chip ->
                MemoDateBadge(chip = chip, background = Surface0, modifier = Modifier.padding(top = CardBodyGap))
            }
        }
    }
}

/**
 * 메모 행 (SCR-MEMO-01 전체 메모). 제목, 미리보기 한 줄, 사람 색 점·닉네임·작성일·메모 날짜 칩, 핀·별 버튼, 아래 선 `line-100`.
 */
@Composable
internal fun MemoRow(
    item: MemoItemUi,
    onOpen: () -> Unit,
    onPinClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Line100,
                    start = Offset(0f, size.height - 0.5.dp.toPx()),
                    end = Offset(size.width, size.height - 0.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(vertical = RowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowButtonGap),
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable(role = Role.Button, onClick = onOpen),
            verticalArrangement = Arrangement.spacedBy(RowLineGap),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = Ink900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.preview.isNotEmpty()) {
                Text(
                    text = item.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DotGap)) {
                MemoAuthorDot(item.author)
                Text(
                    text = item.nickname + " · " + writtenAtText(item.writtenAt),
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = Ink500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.memoDate?.let { chip -> MemoDateBadge(chip = chip, background = Surface100) }
            }
        }
        PinToggleButton(pinned = item.isPinned, onClick = onPinClick)
        FavoriteToggleButton(favorite = item.isFavorite, onClick = onFavoriteClick)
    }
}

/** 점선 모양: 4 그리고 4 쉬기 (CSS `1px dashed`의 기본 모양) */
private const val DASH_ON = 4f
private const val DASH_OFF = 4f

/** 모서리 [radius]의 둥근 점선 테두리 1. [날짜 추가] 버튼(`ink-300`)이 쓴다 (`AddEvent.dc.html` 61행) */
internal fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = width.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(DASH_ON.dp.toPx(), DASH_OFF.dp.toPx())),
        ),
    )
}
