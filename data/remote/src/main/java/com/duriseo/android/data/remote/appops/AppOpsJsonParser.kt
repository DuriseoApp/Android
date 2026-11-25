package com.duriseo.android.data.remote.appops

import com.duriseo.android.domain.appops.AppNotice
import com.duriseo.android.domain.appops.AppOpsData
import com.duriseo.android.domain.appops.AppPlatform
import com.duriseo.android.domain.appops.AppUpdateRule
import com.duriseo.android.domain.appops.BuildCondition
import com.duriseo.android.domain.appops.ForceUpdateSignal
import com.duriseo.android.domain.appops.OsCondition
import com.duriseo.android.domain.appops.PopupImage
import com.duriseo.android.domain.appops.PopupLink
import com.duriseo.android.domain.appops.PopupPreset
import com.duriseo.android.domain.appops.UpdateMode
import com.duriseo.android.domain.appops.isHttpsUrl
import com.duriseo.android.domain.appops.parsePopupImage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * 서버 응답 `{"items":[…]}`와 Remote Config 값(같은 모양의 JSON 문자열)을 **같은 코드로** 읽는다 (D-176, D-177, API.md 3.7).
 *
 * Remote Config 값은 서버가 검사하지 않으므로 항목마다 따로 읽고, 읽을 수 없는 항목은 건너뛴다(없는 것으로 본다):
 * - 모르는 필드는 무시한다.
 * - 공지: `id`(1~50자 `[a-z0-9_-]`)·`closable`(boolean)이 없거나 틀림, `platform`·일시 형식이 틀림.
 * - 규칙: `id`·`mode`·`platform`·`build`가 없거나 모르는 enum·값이 틀림 (`build.op`별 필수값, 음수, `RANGE`의 `min` > `max`, `IN`이 비어 있음).
 * - D-183: `title`·`body`·`imageAlt`·`linkText`가 빈 문자열, 제목 없이 본문만 있음, 제목이 없는데 `https://` 이미지나 `imageAlt`가 없음.
 * - 건너뛰지 않고 고치는 것: `linkUrl`이 `https://`가 아니면 링크 버튼 없음, `image`가 `preset:` 4종·`https://`가 아니면 이미지 없음.
 *   (이미지를 없앤 결과 제목 없는 팝업이 이미지 조건을 못 채우면 그 항목은 건너뛴다.)
 *
 * 최상위가 JSON이 아니거나 `items`가 배열이 아니면 빈 목록이다. 던지지 않는다.
 */
object AppOpsJsonParser {

    private val json = Json { ignoreUnknownKeys = true }
    private val idPattern = Regex("^[a-z0-9_-]{1,50}$")

    fun parseNotices(text: String?): List<AppNotice> = items(text).mapNotNull { parseNotice(it) }

    fun parseRules(text: String?): List<AppUpdateRule> = items(text).mapNotNull { parseRule(it) }

    fun parse(noticesText: String?, rulesText: String?): AppOpsData =
        AppOpsData(parseNotices(noticesText), parseRules(rulesText))

    private fun items(text: String?): List<JsonElement> {
        if (text.isNullOrBlank()) return emptyList()
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: Exception) {
            return emptyList()
        }
        return (((root as? JsonObject)?.get("items")) as? JsonArray)?.toList() ?: emptyList()
    }

    private fun parseNotice(element: JsonElement): AppNotice? {
        val obj = element as? JsonObject ?: return null
        val id = obj.id() ?: return null
        val closable = (obj["closable"] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull ?: return null
        val text = obj.text() ?: return null
        val platform = when (val raw = obj.optionalString("platform")) {
            is Optional.Invalid -> return null
            is Optional.Value -> raw.value?.let { value -> AppPlatform.entries.firstOrNull { it.name == value } ?: return null }
        }
        val startsAt = obj.instant("startsAt") ?: return null
        val endsAt = obj.instant("endsAt") ?: return null
        val linkText = when (val raw = obj.optionalNonEmptyString("linkText")) {
            is Optional.Invalid -> return null
            is Optional.Value -> raw.value
        }
        val linkUrl = (obj["linkUrl"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        val link = if (linkText != null && linkUrl != null && isHttpsUrl(linkUrl)) PopupLink(linkText, linkUrl) else null
        return AppNotice(
            id = id,
            closable = closable,
            title = text.title,
            body = text.body,
            image = text.image,
            imageAlt = text.imageAlt,
            link = link,
            platform = platform,
            startsAt = startsAt.value,
            endsAt = endsAt.value,
        )
    }

    private fun parseRule(element: JsonElement): AppUpdateRule? {
        val obj = element as? JsonObject ?: return null
        val id = obj.id() ?: return null
        val mode = (obj["mode"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?.let { value -> UpdateMode.entries.firstOrNull { it.name == value } } ?: return null
        val platform = (obj["platform"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?.let { value -> AppPlatform.entries.firstOrNull { it.name == value } } ?: return null
        val build = parseBuild(obj["build"]) ?: return null
        val os = when (val raw = obj["osVersion"]) {
            null, JsonNull -> null
            else -> parseOs(raw) ?: return null
        }
        val text = obj.text() ?: return null
        // 규칙에 image가 없으면(null·생략) 앱은 preset:update를 쓴다 (FR-APP-02 AC7). 모르는 값은 이미지 없음 그대로다
        val imageRaw = obj["image"]
        val image = if (imageRaw == null || imageRaw == JsonNull) PopupImage.Preset(PopupPreset.UPDATE) else text.image
        return AppUpdateRule(
            id = id,
            mode = mode,
            platform = platform,
            build = build,
            os = os,
            title = text.title,
            body = text.body,
            image = image,
            imageAlt = text.imageAlt,
        )
    }

    private fun parseBuild(element: JsonElement?): BuildCondition? {
        val obj = element as? JsonObject ?: return null
        val op = (obj["op"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
        return when (op) {
            "EQ" -> obj.nonNegativeLong("value")?.let { BuildCondition.Eq(it) }
            "LTE" -> obj.nonNegativeLong("value")?.let { BuildCondition.Lte(it) }
            "GTE" -> obj.nonNegativeLong("value")?.let { BuildCondition.Gte(it) }
            "RANGE" -> {
                val min = obj.nonNegativeLong("min")
                val max = obj.nonNegativeLong("max")
                if (min == null || max == null || min > max) null else BuildCondition.Range(min, max)
            }
            "IN" -> {
                val array = obj["values"] as? JsonArray
                val values = array?.map { item -> (item as? JsonPrimitive)?.nonNegativeLongOrNull() ?: return null }
                if (values.isNullOrEmpty()) null else BuildCondition.In(values.toSet())
            }
            else -> null
        }
    }

    private fun parseOs(element: JsonElement): OsCondition? {
        val obj = element as? JsonObject ?: return null
        val minElement = obj["min"]
        val maxElement = obj["max"]
        val min = if (minElement == null || minElement == JsonNull) null else obj.nonNegativeLong("min") ?: return null
        val max = if (maxElement == null || maxElement == JsonNull) null else obj.nonNegativeLong("max") ?: return null
        if (min == null && max == null) return null
        if (min != null && max != null && min > max) return null
        return OsCondition(min = min?.toInt(), max = max?.toInt())
    }

    // ---- 제목·본문·이미지 조합 (D-183) ----

    private class Text(val title: String?, val body: String?, val image: PopupImage, val imageAlt: String?)

    private fun JsonObject.text(): Text? {
        val title = when (val v = optionalNonEmptyString("title")) {
            is Optional.Invalid -> return null
            is Optional.Value -> v.value
        }
        val body = when (val v = optionalNonEmptyString("body")) {
            is Optional.Invalid -> return null
            is Optional.Value -> v.value
        }
        val imageAlt = when (val v = optionalNonEmptyString("imageAlt")) {
            is Optional.Invalid -> return null
            is Optional.Value -> v.value
        }
        val image = parsePopupImage((this["image"] as? JsonPrimitive)?.takeIf { it.isString }?.content)
        if (title == null) {
            // 이미지만 넣은 팝업: 본문만 있고 제목 없음은 안 되고, https 이미지와 imageAlt가 있어야 한다
            if (body != null) return null
            if (image !is PopupImage.Web || imageAlt == null) return null
        }
        return Text(title, body, image, imageAlt)
    }

    // ---- 낱개 읽기 ----

    private sealed interface Optional<out T> {
        data class Value<T>(val value: T?) : Optional<T>
        data object Invalid : Optional<Nothing>
    }

    /** 없거나 null이면 null 값, 문자열이면 그 값, 다른 타입이면 읽을 수 없음 */
    private fun JsonObject.optionalString(key: String): Optional<String> = when (val e = this[key]) {
        null, JsonNull -> Optional.Value(null)
        is JsonPrimitive -> if (e.isString) Optional.Value(e.content) else Optional.Invalid
        else -> Optional.Invalid
    }

    /** [optionalString]과 같지만 빈 문자열도 읽을 수 없음 (D-183: 빈 문자열은 null로 보지 않는다) */
    private fun JsonObject.optionalNonEmptyString(key: String): Optional<String> =
        when (val v = optionalString(key)) {
            is Optional.Value -> if (v.value != null && v.value.isEmpty()) Optional.Invalid else v
            Optional.Invalid -> v
        }

    private fun JsonObject.id(): String? =
        (this["id"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { idPattern.matches(it) }

    private class InstantValue(val value: Instant?)

    /** 없거나 null이면 값 null, 형식이 틀리면 null(읽을 수 없음) */
    private fun JsonObject.instant(key: String): InstantValue? = when (val e = this[key]) {
        null, JsonNull -> InstantValue(null)
        is JsonPrimitive -> if (!e.isString) null else try {
            InstantValue(Instant.parse(e.content))
        } catch (ex: DateTimeParseException) {
            null
        }
        else -> null
    }

    private fun JsonObject.nonNegativeLong(key: String): Long? = (this[key] as? JsonPrimitive)?.nonNegativeLongOrNull()

    private fun JsonPrimitive.nonNegativeLongOrNull(): Long? =
        if (isString) null else longOrNull?.takeIf { it in 0..MAX_INT }

    private const val MAX_INT = 2_147_483_647L
}

/**
 * 426 `details`를 강제 업데이트 신호로 읽는다 (API.md 1.1 `APP_VERSION_UNSUPPORTED`).
 * `details`가 비었거나 모양이 틀리면 문구 없는 신호다(앱 기본 문구, 이미지는 `preset:update`). `image`가 없을 때도 `preset:update`다.
 * 제목이 없는 규칙(이미지만 넣은 팝업)이 `https://` 이미지·`imageAlt`를 못 채우면 기본 문구와 `preset:update`로 보이게 문구를 비운다 (D-183).
 */
internal fun parseForceUpdateDetails(details: JsonElement?): ForceUpdateSignal {
    val obj = details as? JsonObject
        ?: return ForceUpdateSignal(
            ruleId = null,
            title = null,
            body = null,
            image = PopupImage.Preset(PopupPreset.UPDATE),
            imageAlt = null,
        )
    fun string(key: String): String? =
        (obj[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotEmpty() }

    val title = string("title")
    val body = string("body")
    val rawImage = string("image")
    val image = if (rawImage == null) PopupImage.Preset(PopupPreset.UPDATE) else parsePopupImage(rawImage)
    val imageAlt = string("imageAlt")
    val imageOnly = title == null && body == null && image is PopupImage.Web && imageAlt != null
    return ForceUpdateSignal(
        ruleId = string("ruleId"),
        title = title,
        body = if (title == null) null else body,
        image = if (title == null && !imageOnly) PopupImage.Preset(PopupPreset.UPDATE) else image,
        imageAlt = if (title == null && !imageOnly) null else imageAlt,
    )
}
