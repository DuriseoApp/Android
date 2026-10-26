package com.duriseo.android.data.remote.json

import com.duriseo.android.data.remote.SampleDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ApiJsonTest {
    private val json = createApiJson()

    @Test
    fun `API 1장 일시는 ISO 8601 UTC 문자열을 Instant로 읽는다`() {
        val dto = json.decodeFromString(
            SampleDto.serializer(),
            """{"id":"1","note":null,"createdAt":"2026-10-17T06:30:00Z"}""",
        )

        assertEquals(Instant.parse("2026-10-17T06:30:00Z"), dto.createdAt)
        assertEquals(1_792_218_600L, dto.createdAt.epochSecond)
    }

    @Test
    fun `API 1장 날짜는 YYYY-MM-DD를 LocalDate로 읽는다`() {
        val dto = json.decodeFromString(
            SampleDto.serializer(),
            """{"id":"1","note":null,"createdAt":"2026-10-17T06:30:00Z","day":"2026-10-17"}""",
        )

        assertEquals(LocalDate.of(2026, 10, 17), dto.day)
    }

    @Test
    fun `API 1장 일시와 날짜는 보낼 때도 같은 문자열 형식이다`() {
        val text = json.encodeToString(
            SampleDto.serializer(),
            SampleDto(
                id = "1",
                note = null,
                createdAt = Instant.parse("2026-10-17T06:30:00Z"),
                day = LocalDate.of(2026, 10, 17),
            ),
        )

        assertEquals(true, text.contains("\"createdAt\":\"2026-10-17T06:30:00Z\""))
        assertEquals(true, text.contains("\"day\":\"2026-10-17\""))
    }

    @Test
    fun `API 1_1 값이 없는 필드는 null로 읽고 모르는 필드는 무시한다`() {
        val dto = json.decodeFromString(
            SampleDto.serializer(),
            """{"id":"1","note":null,"createdAt":"2026-10-17T06:30:00Z","day":null,"newField":1}""",
        )

        assertNull(dto.note)
        assertNull(dto.day)
    }
}
