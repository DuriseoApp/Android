package com.duriseo.android.feature.settings.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoAvatar
import com.duriseo.android.core.designsystem.component.DuriseoAvatarSize
import com.duriseo.android.core.designsystem.component.DuriseoDashedLink
import com.duriseo.android.core.designsystem.component.DuriseoEmptyPartnerSlot
import com.duriseo.android.core.designsystem.component.DuriseoTogetherMark
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.PersonMe
import com.duriseo.android.core.designsystem.theme.PersonPartner
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.feature.settings.R
import java.time.LocalDate

// SCR-SET-01 우리 프로필 카드 (SCREENS.md "프로필 카드", 보드 `Menu.dc.html` 41~97행).
// 수치는 보드 값을 이름 붙인 상수로 둔다. 토큰이 있는 값(모서리 24, 버튼 높이 44, 모서리 12)은 토큰을 쓴다.
//
// 그리지 않은 것: 아바타의 연필 배지와 [내 프로필 수정] 버튼. 이 둘이 가는 SCR-SET-04(프로필 수정)는 단위 8이라
// 아직 화면이 없다. SCREENS.md는 두 가지를 그대로 두고 있어 단위 8에서 더한다 (unit-3.md "문서와 다른 점").

/** 카드 안쪽 여백: 위 22, 좌우 16, 아래 16. 덩어리 사이 18 */
private val CardPaddingTop = 22.dp
private val CardPaddingHorizontal = 16.dp
private val CardPaddingBottom = 16.dp
private val CardGap = 18.dp

/** 아바타 아래 이름 줄: 사이 8, 이름 최대 폭 120 */
private val PersonGap = 8.dp
private val NameMaxWidth = 120.dp

/** 이름 아래 "나"·"상대" 표지: 점 지름 8, 점과 글자 사이 5 */
private val TagDot = 8.dp
private val TagGap = 5.dp

/** 가운데 칸: 커플은 위 22 띄우고 최소 폭 64, 마크와 D+N 사이 6. 혼자는 폭 64 고정, 높이 72 (아바타와 같음) */
private val CenterTop = 22.dp
private val CenterMinWidth = 64.dp
private val CenterGap = 6.dp
private val DashedLinkWidth = 48.dp

/** "함께" 마크 34 x 22 */
private val MarkWidth = 34.dp
private val MarkHeight = 22.dp

/** 버튼 테두리 두께 1 */
private val OutlineWidth = 1.dp

/**
 * 커플이면 두 사람과 D+N, 혼자면 나와 "아직 없음". [startDate]가 없으면 D+N과 "부터 함께하고 있어요" 줄은 숨긴다 (D-99).
 *
 * **디자인 반영 후 맞춤(이미지 대기)**: 처음 만난 날 없는 커플 (`SCR-SET-01--no-start-date.png` 예정). 지금은 D+N 글자와
 * "부터 함께하고 있어요" 줄만 빼고 "함께" 마크와 나머지는 그대로 둔다.
 */
@Composable
internal fun SettingsProfileCard(
    space: SettingsSpace,
    onEditStartDate: () -> Unit,
    onConnect: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = when (space) {
        is SettingsSpace.Couple -> stringResource(R.string.profile_label_couple)
        is SettingsSpace.Alone -> stringResource(R.string.profile_label_alone)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface100, MaterialTheme.shapes.extraLarge)
            .padding(
                start = CardPaddingHorizontal,
                end = CardPaddingHorizontal,
                top = CardPaddingTop,
                bottom = CardPaddingBottom,
            )
            .semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        when (space) {
            is SettingsSpace.Couple -> CoupleBody(space, onEditStartDate, enabled)
            is SettingsSpace.Alone -> AloneBody(space, onConnect, enabled)
        }
    }
}

@Composable
private fun CoupleBody(space: SettingsSpace.Couple, onEditStartDate: () -> Unit, enabled: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        PersonColumn(
            person = space.me,
            letter = stringResource(R.string.profile_letter_me),
            tag = stringResource(R.string.profile_tag_me),
            fallback = PersonMe,
            modifier = Modifier.weight(1f),
        )
        TogetherColumn(space)
        PersonColumn(
            person = space.partner,
            letter = stringResource(R.string.profile_letter_partner),
            tag = stringResource(R.string.profile_tag_partner),
            fallback = PersonPartner,
            modifier = Modifier.weight(1f),
        )
    }
    val startDate = space.startDate
    if (startDate != null) {
        Text(
            text = stringResource(R.string.profile_together_since, formatDate(startDate)),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = Ink600,
            textAlign = TextAlign.Center,
        )
    }
    ProfileButton(
        text = stringResource(R.string.profile_edit_start_date),
        onClick = onEditStartDate,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    )
}

/** 가운데 칸: "함께" 마크 + D+N. 읽는 이름은 "함께한 지 D+N" 하나다 (SCREENS.md 접근성 문구) */
@Composable
private fun TogetherColumn(space: SettingsSpace.Couple) {
    val days = space.dayNumber
    val description = days?.let { stringResource(R.string.profile_days_together_description, it) }
    Column(
        modifier = Modifier
            .padding(top = CenterTop)
            .widthIn(min = CenterMinWidth)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CenterGap),
    ) {
        DuriseoTogetherMark(
            width = MarkWidth,
            height = MarkHeight,
            meColorKey = space.me.colorKey,
            partnerColorKey = space.partner.colorKey,
            togetherColorKey = space.togetherColorKey,
        )
        if (days != null) {
            Text(
                text = stringResource(R.string.profile_days_together, days),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Brand700,
            )
        }
    }
}

@Composable
private fun AloneBody(space: SettingsSpace.Alone, onConnect: () -> Unit, enabled: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        PersonColumn(
            person = space.me,
            letter = stringResource(R.string.profile_letter_me),
            tag = stringResource(R.string.profile_tag_me),
            fallback = PersonMe,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .widthIn(min = CenterMinWidth)
                .height(DuriseoAvatarSize.Large.diameter),
            contentAlignment = Alignment.Center,
        ) {
            DuriseoDashedLink(width = DashedLinkWidth)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PersonGap),
        ) {
            DuriseoEmptyPartnerSlot(DuriseoAvatarSize.Large)
            Text(
                text = stringResource(R.string.profile_partner_empty),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Ink600,
            )
        }
    }
    ProfileButton(
        text = stringResource(R.string.profile_connect_partner),
        onClick = onConnect,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        container = Brand500,
        contentColor = OnBrand,
        emphasized = true,
    )
}

/** 아바타 + 이름 + "나"/"상대" 표지 */
@Composable
private fun PersonColumn(
    person: SettingsPerson,
    letter: String,
    tag: String,
    fallback: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PersonGap),
    ) {
        DuriseoAvatar(label = letter, colorKey = person.colorKey, size = DuriseoAvatarSize.Large, fallback = fallback)
        Text(
            text = person.name,
            modifier = Modifier.widthIn(max = NameMaxWidth),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Ink900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TagGap),
        ) {
            Box(
                modifier = Modifier
                    .size(TagDot)
                    .background(DuriseoPickPalette.colorOf(person.colorKey) ?: fallback, DuriseoShapes.Full),
            )
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Ink600,
            )
        }
    }
}

/**
 * 카드 아래 버튼: 높이 `size-button-sm` 44, 모서리 `radius-sm` 12, 글자 14/600. 기본은 `line-200` 테두리의 흰 면,
 * [emphasized]면 [container] 면과 14/700 글자 ([상대와 연결하기]).
 */
@Composable
private fun ProfileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Surface0,
    contentColor: Color = Ink900,
    emphasized: Boolean = false,
) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .height(DuriseoSize.ButtonSm)
            .clip(shape)
            .background(container)
            .then(if (emphasized) Modifier else Modifier.border(OutlineWidth, Line200, shape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "2026년 10월 12일" (연·월·일, 요일 없음, 앞자리 0 없음) */
@Composable
internal fun formatDate(date: LocalDate): String =
    stringResource(DesignR.string.date_year_month_day, date.year, date.monthValue, date.dayOfMonth)
