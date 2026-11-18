package com.duriseo.android.feature.settings.licenses

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * AboutLibraries Gradle 플러그인이 만든 `aboutlibraries.json`을 [LicenseEntry] 목록으로 바꾼다.
 * JSON 모양: `{ "libraries": [ { "uniqueId", "name", "licenses": ["<라이선스 키>"] } ], "licenses": { "<키>": { "name", "url", "content" } } }`.
 * 알 수 없는 필드는 무시하고, 읽을 수 없는 항목은 건너뛴다 (목록 전체를 못 보이는 것보다 낫다).
 * 라이선스 본문이 없으면 이름과 주소를, 그것도 없으면 라이브러리 이름을 보인다.
 */
internal fun parseAboutLibraries(json: String): List<LicenseEntry> {
    val root = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return emptyList()
    val licenseTable = root["licenses"] as? JsonObject ?: JsonObject(emptyMap())
    val libraries = root["libraries"] as? JsonArray ?: return emptyList()

    return libraries.mapNotNull { element ->
        val library = element as? JsonObject ?: return@mapNotNull null
        val name = library.string("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val id = library.string("uniqueId")?.takeIf { it.isNotBlank() } ?: name
        val licenses = (library["licenses"] as? JsonArray).orEmpty().mapNotNull { resolveLicense(it, licenseTable) }
        LicenseEntry(
            id = id,
            name = name,
            licenseName = licenses.map { it.name }.filter { it.isNotBlank() }.distinct().joinToString(", "),
            body = licenses.map { it.body }.filter { it.isNotBlank() }.distinct().joinToString("\n\n").ifBlank { name },
        )
    }
}

private class ResolvedLicense(val name: String, val body: String)

/** 라이브러리의 라이선스 항목은 표의 키(문자열)이거나 객체 자체일 수 있다 */
private fun resolveLicense(element: JsonElement, table: JsonObject): ResolvedLicense? {
    val obj = when (element) {
        is JsonPrimitive -> {
            val key = element.contentOrNull ?: return null
            table[key] as? JsonObject ?: return ResolvedLicense(name = key, body = key)
        }
        is JsonObject -> element
        else -> return null
    }
    val name = obj.string("name") ?: obj.string("spdxId").orEmpty()
    val body = obj.string("content")?.takeIf { it.isNotBlank() }
        ?: obj.string("url")?.takeIf { it.isNotBlank() }
        ?: name
    return ResolvedLicense(name = name, body = body)
}

private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()
