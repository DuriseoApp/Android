package com.duriseo.android.domain.appops

import java.time.Instant

/** 팝업·규칙의 `platform` (API.md 3.7). 이 앱은 [ANDROID]다 */
enum class AppPlatform {
    ANDROID,
    IOS,
}

/** 앱에 넣어 둔 기본 이미지 4종의 키 (D-181, `preset:<키>`) */
enum class PopupPreset(val key: String) {
    MAINTENANCE("maintenance"),
    ERROR("error"),
    UPDATE("update"),
    NOTICE("notice"),
    ;

    companion object {
        fun fromKey(key: String): PopupPreset? = entries.firstOrNull { it.key == key }
    }
}

/** 팝업 이미지 (D-181). 모르는 값이거나 없으면 [None] */
sealed interface PopupImage {
    /** `preset:<키>`: 앱에 넣어 둔 기본 이미지를 작게(가운데 원형) 쓴다 */
    data class Preset(val preset: PopupPreset) : PopupImage

    /** `https://` 주소: 받아서 팝업 안쪽 폭에 원래 비율로 쓴다 */
    data class Web(val url: String) : PopupImage

    data object None : PopupImage
}

private const val PRESET_PREFIX = "preset:"
private const val HTTPS_PREFIX = "https://"

/** `https://`로 시작하는 주소인지 (링크·이미지 공통, D-183) */
fun isHttpsUrl(value: String): Boolean = value.startsWith(HTTPS_PREFIX) && value.length > HTTPS_PREFIX.length

/** `image` 문자열을 읽는다. `preset:` 4종·`https://` 주소가 아니면 [PopupImage.None] (SCREENS.md "값 거르기") */
fun parsePopupImage(raw: String?): PopupImage {
    if (raw == null) return PopupImage.None
    if (raw.startsWith(PRESET_PREFIX)) {
        val preset = PopupPreset.fromKey(raw.removePrefix(PRESET_PREFIX)) ?: return PopupImage.None
        return PopupImage.Preset(preset)
    }
    return if (isHttpsUrl(raw)) PopupImage.Web(raw) else PopupImage.None
}

/** 링크 버튼. 글자와 `https://` 주소가 둘 다 있을 때만 만든다 (FR-APP-01 AC3) */
data class PopupLink(val text: String, val url: String)

/**
 * 공지 팝업 (API.md 3.7 `AppNotice`). 값 거르기를 끝낸 모양이다: 읽는 쪽([image], [link])이 이미 정리했다.
 *
 * @property closable false = 앱을 막는 공지, true = 알리는 공지
 * @property platform null이면 모든 플랫폼
 */
data class AppNotice(
    val id: String,
    val closable: Boolean,
    val title: String?,
    val body: String?,
    val image: PopupImage,
    val imageAlt: String?,
    val link: PopupLink?,
    val platform: AppPlatform?,
    val startsAt: Instant?,
    val endsAt: Instant?,
)

enum class UpdateMode {
    FORCE,
    RECOMMEND,
}

/** 업데이트 규칙의 빌드 번호(versionCode) 조건 (API.md 3.7) */
sealed interface BuildCondition {
    data class Eq(val value: Long) : BuildCondition
    data class Lte(val value: Long) : BuildCondition
    data class Gte(val value: Long) : BuildCondition

    /** 양 끝 포함 */
    data class Range(val min: Long, val max: Long) : BuildCondition
    data class In(val values: Set<Long>) : BuildCondition
}

/** OS 버전(Android는 API 레벨) 조건. 양 끝 포함, null이면 그쪽은 제한 없음 */
data class OsCondition(val min: Int?, val max: Int?)

/** 업데이트 규칙 (API.md 3.7 `AppUpdateRule`). 값 거르기를 끝낸 모양이다 */
data class AppUpdateRule(
    val id: String,
    val mode: UpdateMode,
    val platform: AppPlatform,
    val build: BuildCondition,
    val os: OsCondition?,
    val title: String?,
    val body: String?,
    val image: PopupImage,
    val imageAlt: String?,
)

/** 한 곳(Remote Config 또는 서버)에서 읽은 공지와 규칙. 목록 순서를 그대로 둔다 (D-178) */
data class AppOpsData(
    val notices: List<AppNotice>,
    val rules: List<AppUpdateRule>,
) {
    companion object {
        val Empty = AppOpsData(emptyList(), emptyList())
    }
}

/** 이 앱의 플랫폼·빌드 번호(versionCode)·OS 버전(API 레벨). 규칙 맞춤 판단에 쓴다 */
data class AppEnvironment(
    val platform: AppPlatform,
    val build: Long,
    val osVersion: Int,
)

/**
 * 서버가 426 `APP_VERSION_UNSUPPORTED`로 알려 준 강제 업데이트 (API.md 1.1 details, 4.9 7단계).
 * [title]·[body]가 null이면 앱 기본 문구를 쓰고, [image]가 [PopupImage.None]이면 `preset:update`를 쓴다 (D-180, FR-APP-02 AC7).
 * 규칙이 이미지만 넣은 것이면 [title]이 null이고 [image]는 `https://`, [imageAlt]가 있다.
 *
 * @property ruleId details가 비었으면 null
 */
data class ForceUpdateSignal(
    val ruleId: String?,
    val title: String?,
    val body: String?,
    val image: PopupImage,
    val imageAlt: String?,
)
