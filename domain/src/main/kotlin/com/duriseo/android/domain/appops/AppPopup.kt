package com.duriseo.android.domain.appops

/** `id` 이름 공간. 공지와 규칙은 같은 `id`여도 서로 다른 것이다 (D-178) */
enum class PopupIdSpace {
    NOTICE,
    UPDATE_RULE,
}

/**
 * 팝업 종류. 선언 순서가 표시 순서다: 강제 업데이트 → 앱을 막는 공지 → 권장 업데이트 → 알리는 공지 (D-178).
 *
 * @property idSpace "오늘 하루 안 보기"를 기억하는 `id` 공간
 * @property canHideToday [오늘 하루 안 보기]가 있는 종류. 강제 업데이트와 앱을 막는 공지는 없다 (D-179, FR-APP-02 AC4)
 */
enum class PopupKind(val idSpace: PopupIdSpace, val canHideToday: Boolean) {
    FORCE_UPDATE(PopupIdSpace.UPDATE_RULE, canHideToday = false),
    BLOCK_NOTICE(PopupIdSpace.NOTICE, canHideToday = false),
    RECOMMEND_UPDATE(PopupIdSpace.UPDATE_RULE, canHideToday = true),
    INFO_NOTICE(PopupIdSpace.NOTICE, canHideToday = true),
}

/**
 * 화면이 그릴 팝업 하나 (SCR-APP-01·02). 업데이트 규칙의 `image`가 없을 때 `preset:update`를 쓰는 것과 426 기본 문구는
 * 화면이 정한다([image]가 [PopupImage.None]이고 [title]이 null이 아닐 때).
 *
 * @property id 규칙·공지 `id`. 426에서 details가 비었으면 null
 * @property link 공지의 링크 버튼. 업데이트에는 없다
 */
data class AppPopup(
    val kind: PopupKind,
    val id: String?,
    val title: String?,
    val body: String?,
    val image: PopupImage,
    val imageAlt: String?,
    val link: PopupLink?,
) {
    /** 같은 팝업인지(내용이 아니라 종류와 `id`). 다음 확인에서 떠 있던 팝업이 그대로인지 볼 때 쓴다 */
    val identity: Pair<PopupKind, String?> get() = kind to id
}

fun AppNotice.toPopup(): AppPopup = AppPopup(
    kind = if (closable) PopupKind.INFO_NOTICE else PopupKind.BLOCK_NOTICE,
    id = id,
    title = title,
    body = body,
    image = image,
    imageAlt = imageAlt,
    link = link,
)

fun AppUpdateRule.toPopup(): AppPopup = AppPopup(
    kind = if (mode == UpdateMode.FORCE) PopupKind.FORCE_UPDATE else PopupKind.RECOMMEND_UPDATE,
    id = id,
    title = title,
    body = body,
    image = image,
    imageAlt = imageAlt,
    link = null,
)

fun ForceUpdateSignal.toPopup(): AppPopup = AppPopup(
    kind = PopupKind.FORCE_UPDATE,
    id = ruleId,
    title = title,
    body = body,
    image = image,
    imageAlt = imageAlt,
    link = null,
)
