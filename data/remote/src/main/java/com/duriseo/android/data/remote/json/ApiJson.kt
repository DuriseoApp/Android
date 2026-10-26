package com.duriseo.android.data.remote.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** 일시: ISO 8601 UTC 문자열 (`2026-10-17T06:30:00Z`) <-> [Instant] (API.md 1장, NFR-TIME-01) */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        val text = decoder.decodeString()
        return try {
            Instant.parse(text)
        } catch (e: DateTimeParseException) {
            // 모양이 틀린 값은 직렬화 오류로 바꿔 ApiCaller가 INTERNAL_ERROR로 처리하게 한다
            throw SerializationException("일시 형식이 맞지 않아요: ISO 8601 UTC가 아님", e)
        }
    }

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
}

/** 날짜: `YYYY-MM-DD` 문자열 <-> [LocalDate] (API.md 1장, NFR-TIME-01) */
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        val text = decoder.decodeString()
        return try {
            LocalDate.parse(text)
        } catch (e: DateTimeParseException) {
            throw SerializationException("날짜 형식이 맞지 않아요: YYYY-MM-DD가 아님", e)
        }
    }

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
}

/**
 * API 본문용 `Json` (API.md 1장). 서버는 표준 JSON이라 `isLenient`는 켜지 않고,
 * 요청 크기를 줄이려고 `prettyPrint`도 끈다.
 *
 * DTO에서 `@Contextual val createdAt: Instant`, `@Contextual val date: LocalDate`로 쓴다.
 */
fun createApiJson(): Json = Json {
    ignoreUnknownKeys = true // 서버가 필드를 더해도 앱이 깨지지 않게
    encodeDefaults = true
    explicitNulls = false
    serializersModule = SerializersModule {
        contextual(Instant::class, InstantSerializer)
        contextual(LocalDate::class, LocalDateSerializer)
    }
}
