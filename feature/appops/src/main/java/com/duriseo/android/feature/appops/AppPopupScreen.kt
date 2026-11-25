package com.duriseo.android.feature.appops

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.duriseo.android.core.designsystem.component.DuriseoButton
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoPopupDialog
import com.duriseo.android.core.designsystem.component.popupPresetDrawable
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.domain.appops.AppPopup
import com.duriseo.android.domain.appops.PopupImage
import com.duriseo.android.domain.appops.PopupKind

// SCR-APP-01 공지 팝업, SCR-APP-02 업데이트 팝업 (design-system.md "공지·업데이트 팝업", 보드 `AppNotice.dc.html`·`AppUpdate.dc.html`).
// 틀(뒤 막·패널·스크롤·맨 위 창)은 `DuriseoPopupDialog`다.

/** 기본 이미지(`preset:`) 크기 112×112, 가운데 (보드 값) */
private val PresetImageSize = 112.dp

/** 이미지 아래 더 띄우는 8: 내용 영역의 간격 8에 더해 이미지와 제목 사이가 16이 된다 (`space-2`) */
private val ImageExtraGap = DuriseoSpacing.Space2

/** Preview 전용: `https://` 이미지 자리 견본의 비율 (보드 `PopupBanner.dc.html` 280×350) */
private const val PreviewBannerAspectRatio = 280f / 350f

/** 화면에 그릴 글: 운영자가 정한 값, 이미지만 넣은 팝업의 `imageAlt`, 426 기본 문구 */
internal data class PopupCopy(
    /** 패널 이름(접근성). 이미지만 넣은 팝업은 `imageAlt` */
    val paneTitle: String?,
    val title: String?,
    val body: String?,
)

/**
 * 팝업에 그릴 글을 정한다.
 * - 제목이 있으면 제목·본문 그대로.
 * - 제목이 없고 `imageAlt`가 있으면 이미지만 넣은 팝업(D-183): `https://` 이미지가 있으면 글을 그리지 않고 `imageAlt`는 패널 이름이다.
 *   이미지를 받지 못했으면(이미지가 `https://`가 아님) `imageAlt`를 제목 자리에 보여준다.
 * - 제목도 `imageAlt`도 없으면 426에 문구가 없는 것이라 기본 문구를 쓴다 (D-180).
 */
@Composable
internal fun AppPopup.popupCopy(): PopupCopy {
    val titleText = title
    if (titleText != null) return PopupCopy(paneTitle = titleText, title = titleText, body = body)
    val alt = imageAlt
    if (alt != null) {
        return if (image is PopupImage.Web) {
            PopupCopy(paneTitle = alt, title = null, body = null)
        } else {
            PopupCopy(paneTitle = alt, title = alt, body = null)
        }
    }
    val defaultTitle = stringResource(R.string.popup_default_title)
    return PopupCopy(paneTitle = defaultTitle, title = defaultTitle, body = stringResource(R.string.popup_default_body))
}

/**
 * 공지·업데이트 팝업 하나. 닫을 수 없는 팝업(강제·앱을 막는 공지)은 뒤로 가기를 막고, 닫을 수 있는 팝업의 뒤로 가기는
 * [AppPopupAction.Back] (Q-21, D-179).
 *
 * @param imageLoader `https://` 이미지를 그리는 로더 (`@PopupImageLoader`). null이면 견본 상자(Preview)
 */
@Composable
fun AppPopupScreen(
    popup: AppPopup,
    imageLoader: ImageLoader?,
    onAction: (AppPopupAction) -> Unit,
    modifier: Modifier = Modifier,
    onUpdate: () -> Unit = { },
) {
    val copy = popup.popupCopy()
    DuriseoPopupDialog(
        paneTitle = copy.paneTitle.orEmpty(),
        onBack = if (popup.kind.canHideToday) ({ onAction(AppPopupAction.Back) }) else null,
        modifier = modifier,
        actions = { PopupActions(popup, onAction, onUpdate) },
    ) {
        PopupContent(popup.image, imageLoader, copy)
    }
}

@Composable
private fun ColumnScope.PopupContent(image: PopupImage, imageLoader: ImageLoader?, copy: PopupCopy) {
    val hasText = copy.title != null
    when (image) {
        is PopupImage.Preset -> {
            // 기본 이미지는 장식이다: 뜻은 제목과 본문이 전한다 (스크린리더가 읽지 않는다)
            Image(
                painter = painterResource(popupPresetDrawable(image.preset)),
                contentDescription = null,
                modifier = Modifier
                    .size(PresetImageSize)
                    .align(Alignment.CenterHorizontally),
            )
            if (hasText) Spacer(Modifier.height(ImageExtraGap))
        }

        is PopupImage.Web -> {
            PopupWebImage(image.url, imageLoader)
            // 이미지만 넣은 팝업은 이미지 아래 바로 버튼 영역이다
            if (hasText) Spacer(Modifier.height(ImageExtraGap))
        }

        PopupImage.None -> Unit
    }
    copy.title?.let { title ->
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(lineBreak = KeepAllLineBreak),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
    copy.body?.let { body ->
        Text(
            // 본문의 줄바꿈(\n)은 그대로 보인다
            text = body,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(lineBreak = KeepAllLineBreak),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 낱말 단위 줄바꿈 (`word-break: keep-all`) */
private val KeepAllLineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)

/** `https://` 이미지: 패널 안쪽 폭에 꽉 차게 원래 비율로, 모서리 `radius-lg`. 팝업을 띄우기 전에 미리 받아 둔다 (Q-23) */
@Composable
private fun PopupWebImage(url: String, imageLoader: ImageLoader?) {
    val shape = MaterialTheme.shapes.large
    if (imageLoader == null || LocalInspectionMode.current) {
        // Preview 견본: 받은 이미지 자리
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PreviewBannerAspectRatio)
                .clip(shape)
                .background(Surface100),
        )
        return
    }
    AsyncImage(
        model = url,
        // 이미지만 넣은 팝업의 이름은 패널의 `imageAlt`가 전한다: 이미지는 따로 읽지 않는다
        contentDescription = null,
        imageLoader = imageLoader,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        contentScale = ContentScale.FillWidth,
    )
}

@Composable
private fun ColumnScope.PopupActions(popup: AppPopup, onAction: (AppPopupAction) -> Unit, onUpdate: () -> Unit) {
    popup.link?.let { link ->
        DuriseoButton(
            text = link.text,
            onClick = { onAction(AppPopupAction.OpenLink) },
            modifier = Modifier.fillMaxWidth(),
            style = DuriseoButtonStyle.Outline,
        )
    }
    when (popup.kind) {
        // 앱을 막는 공지: [앱 종료] 하나
        PopupKind.BLOCK_NOTICE -> DuriseoButton(
            text = stringResource(R.string.popup_exit),
            onClick = { onAction(AppPopupAction.Exit) },
            modifier = Modifier.fillMaxWidth(),
            style = DuriseoButtonStyle.Brand,
        )

        // 알리는 공지: [오늘 하루 안 보기 | 확인]
        PopupKind.INFO_NOTICE -> TwoButtons(
            leftText = stringResource(R.string.popup_hide_today),
            onLeft = { onAction(AppPopupAction.HideToday) },
            rightText = stringResource(R.string.popup_confirm),
            onRight = { onAction(AppPopupAction.Dismiss) },
        )

        // 강제 업데이트: [앱 종료 | 업데이트]
        PopupKind.FORCE_UPDATE -> TwoButtons(
            leftText = stringResource(R.string.popup_exit),
            onLeft = { onAction(AppPopupAction.Exit) },
            rightText = stringResource(R.string.popup_update),
            onRight = onUpdate,
        )

        // 권장 업데이트: [업데이트] → [오늘 하루 안 보기 | 닫기]
        PopupKind.RECOMMEND_UPDATE -> {
            DuriseoButton(
                text = stringResource(R.string.popup_update),
                onClick = onUpdate,
                modifier = Modifier.fillMaxWidth(),
                style = DuriseoButtonStyle.Brand,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
            ) {
                DuriseoButton(
                    text = stringResource(R.string.popup_hide_today),
                    onClick = { onAction(AppPopupAction.HideToday) },
                    modifier = Modifier.weight(1f),
                    style = DuriseoButtonStyle.Outline,
                )
                DuriseoButton(
                    text = stringResource(R.string.popup_close),
                    onClick = { onAction(AppPopupAction.Dismiss) },
                    modifier = Modifier.weight(1f),
                    style = DuriseoButtonStyle.Outline,
                )
            }
        }
    }
}

/** 한 줄 두 칸(같은 폭, 사이 `space-2`). 왼쪽은 보조(아웃라인), 오른쪽은 주 버튼(`brand-500`) */
@Composable
private fun TwoButtons(leftText: String, onLeft: () -> Unit, rightText: String, onRight: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
    ) {
        DuriseoButton(
            text = leftText,
            onClick = onLeft,
            modifier = Modifier.weight(1f),
            style = DuriseoButtonStyle.Outline,
        )
        DuriseoButton(
            text = rightText,
            onClick = onRight,
            modifier = Modifier.weight(1f),
            style = DuriseoButtonStyle.Brand,
        )
    }
}
