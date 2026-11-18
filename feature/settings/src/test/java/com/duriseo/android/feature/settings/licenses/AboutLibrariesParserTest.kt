package com.duriseo.android.feature.settings.licenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutLibrariesParserTest {

    @Test
    fun `FR-SET-02 AC3 라이브러리 이름과 라이선스 이름과 본문을 읽는다`() {
        val json = """
            {
              "libraries": [
                { "uniqueId": "com.squareup.okhttp3:okhttp", "name": "OkHttp", "licenses": ["Apache-2.0"], "developers": [] }
              ],
              "licenses": {
                "Apache-2.0": { "name": "Apache License 2.0", "url": "https://www.apache.org/licenses/LICENSE-2.0", "content": "Apache text" }
              }
            }
        """.trimIndent()

        val entries = parseAboutLibraries(json)

        assertEquals(1, entries.size)
        assertEquals("com.squareup.okhttp3:okhttp", entries[0].id)
        assertEquals("OkHttp", entries[0].name)
        assertEquals("Apache License 2.0", entries[0].licenseName)
        assertEquals("Apache text", entries[0].body)
    }

    @Test
    fun `FR-SET-02 AC3 라이선스 본문이 없으면 이름과 주소만 보인다`() {
        val json = """
            {
              "libraries": [ { "uniqueId": "x:y", "name": "Y", "licenses": ["Custom"] } ],
              "licenses": { "Custom": { "name": "Custom License", "url": "https://example.com/license" } }
            }
        """.trimIndent()

        val entry = parseAboutLibraries(json).single()

        assertEquals("Custom License", entry.licenseName)
        assertEquals("https://example.com/license", entry.body)
    }

    @Test
    fun `FR-SET-02 AC3 읽을 수 없는 JSON이면 빈 목록이다`() {
        assertTrue(parseAboutLibraries("not json").isEmpty())
        assertTrue(parseAboutLibraries("{}").isEmpty())
    }
}
