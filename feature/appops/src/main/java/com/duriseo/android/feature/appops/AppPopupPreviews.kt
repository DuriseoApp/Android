package com.duriseo.android.feature.appops

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.domain.appops.AppPopup
import com.duriseo.android.domain.appops.PopupImage
import com.duriseo.android.domain.appops.PopupKind
import com.duriseo.android.domain.appops.PopupLink
import com.duriseo.android.domain.appops.PopupPreset

// Preview 글자는 보드(`AppNotice.dc.html`·`AppUpdate.dc.html`)의 예시 글이라 앱 문구(strings.xml)가 아니다.
// `https://` 이미지 자리는 견본 상자로 그린다 (Preview는 이미지를 받지 못한다).

private const val BannerUrl = "https://example.com/banner.png"
private const val BannerAlt = "서비스 점검 안내"

private val MaintenanceBody = "더 안정적으로 쓸 수 있게 점검하고 있어요.\n점검이 끝나면 다시 쓸 수 있어요."
private val InfoBody = "일부 알림이 늦게 도착하고 있어요.\n일정과 메모는 평소처럼 쓸 수 있어요."
private val LongBody = List(6) {
    "더 안정적으로 쓸 수 있게 점검하고 있어요. 점검하는 동안에는 일정과 메모를 보거나 쓸 수 없어요.\n점검이 끝나면 다시 쓸 수 있어요."
}.joinToString("\n\n")

private fun block(
    image: PopupImage = PopupImage.Preset(PopupPreset.MAINTENANCE),
    title: String? = "서비스 점검 중이에요",
    body: String? = MaintenanceBody,
    alt: String? = null,
    link: PopupLink? = null,
) = AppPopup(PopupKind.BLOCK_NOTICE, "block", title, body, image, alt, link)

private fun info(
    image: PopupImage = PopupImage.Preset(PopupPreset.NOTICE),
    title: String? = "알림이 늦게 올 수 있어요",
    body: String? = InfoBody,
    alt: String? = null,
    link: PopupLink? = null,
) = AppPopup(PopupKind.INFO_NOTICE, "info", title, body, image, alt, link)

private fun update(
    kind: PopupKind,
    image: PopupImage = PopupImage.Preset(PopupPreset.UPDATE),
    title: String? = "새 버전이 나왔어요",
    body: String? = "더 안정적인 새 버전으로 업데이트해 주세요.",
    alt: String? = null,
) = AppPopup(kind, "rule", title, body, image, alt, null)

private val Link = PopupLink("자세히 보기", "https://example.com/notice")

@Composable
private fun PopupPreview(popup: AppPopup) {
    DuriseoTheme {
        // 뒤 화면 견본
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
        AppPopupScreen(popup = popup, imageLoader = null, onAction = {}, onUpdate = {})
    }
}

// SCR-APP-01 공지 팝업

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_Block() = PopupPreview(block())

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_BlockNoImage() = PopupPreview(block(image = PopupImage.None))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_BlockLink() = PopupPreview(block(link = Link))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_BlockImageOnly() =
    PopupPreview(block(image = PopupImage.Web(BannerUrl), title = null, body = null, alt = BannerAlt))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_LongBody() = PopupPreview(block(body = LongBody))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_Info() = PopupPreview(info())

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_InfoNoImage() = PopupPreview(info(image = PopupImage.None))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_InfoLink() = PopupPreview(info(link = Link))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_InfoImageOnly() =
    PopupPreview(info(image = PopupImage.Web(BannerUrl), title = null, body = null, alt = BannerAlt))

/** 이미지만 넣은 팝업이 이미지를 받지 못함: 코디네이터가 이미지를 없앴고 `imageAlt`가 제목 자리에 보인다 (D-183) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNoticePreview_ImageOnlyFailed() =
    PopupPreview(info(image = PopupImage.None, title = null, body = null, alt = BannerAlt))

// SCR-APP-02 업데이트 팝업

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppUpdatePreview_Force() = PopupPreview(update(PopupKind.FORCE_UPDATE))

/** 426에 문구가 없음: 기본 문구와 `preset:update` */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppUpdatePreview_ForceDefault() =
    PopupPreview(update(PopupKind.FORCE_UPDATE, title = null, body = null))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppUpdatePreview_ForceImageOnly() = PopupPreview(
    update(PopupKind.FORCE_UPDATE, image = PopupImage.Web(BannerUrl), title = null, body = null, alt = "새 버전이 나왔어요"),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppUpdatePreview_Recommend() = PopupPreview(update(PopupKind.RECOMMEND_UPDATE))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppUpdatePreview_RecommendImageOnly() = PopupPreview(
    update(PopupKind.RECOMMEND_UPDATE, image = PopupImage.Web(BannerUrl), title = null, body = null, alt = "새 버전이 나왔어요"),
)
