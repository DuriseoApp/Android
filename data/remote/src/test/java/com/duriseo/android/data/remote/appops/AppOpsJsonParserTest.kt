package com.duriseo.android.data.remote.appops

import com.duriseo.android.domain.appops.AppPlatform
import com.duriseo.android.domain.appops.BuildCondition
import com.duriseo.android.domain.appops.OsCondition
import com.duriseo.android.domain.appops.PopupImage
import com.duriseo.android.domain.appops.PopupLink
import com.duriseo.android.domain.appops.PopupPreset
import com.duriseo.android.domain.appops.UpdateMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 서버 응답과 Remote Config 값을 같은 코드로 읽는다 (D-176, D-177, D-183, SCREENS.md "값 거르기") */
class AppOpsJsonParserTest {

    private fun notices(vararg items: String) = AppOpsJsonParser.parseNotices("""{"items":[${items.joinToString(",")}]}""")
    private fun rules(vararg items: String) = AppOpsJsonParser.parseRules("""{"items":[${items.joinToString(",")}]}""")

    private val okNotice = """{"id":"ok","closable":true,"title":"안내","body":"본문"}"""

    @Test
    fun `D-176 Remote Config 값과 서버 응답을 같은 모양으로 읽는다`() {
        val text = """{"items":[{"id":"n1","closable":false,"title":"점검","body":"본문","image":"preset:error","platform":"ANDROID"},
            {"id":"n2","closable":true,"title":"이벤트","platform":null}]}"""
        val rulesText = """{"items":[{"id":"r1","mode":"RECOMMEND","platform":"ANDROID",
            "build":{"op":"IN","value":null,"min":null,"max":null,"values":[3,5,5]},"osVersion":null,"title":"업데이트"}]}"""

        // Remote Config는 문자열 그대로, 서버는 본문 문자열 그대로 같은 함수로 읽는다
        val data = AppOpsJsonParser.parse(text, rulesText)

        assertEquals(listOf("n1", "n2"), data.notices.map { it.id })
        assertEquals(PopupImage.Preset(PopupPreset.ERROR), data.notices[0].image)
        assertEquals(AppPlatform.ANDROID, data.notices[0].platform)
        assertEquals(BuildCondition.In(setOf(3L, 5L)), data.rules.single().build)
        assertEquals(UpdateMode.RECOMMEND, data.rules.single().mode)
        assertNull(data.rules.single().os)
        // 비었거나 JSON이 아닌 값은 빈 목록이다 (던지지 않는다)
        assertTrue(AppOpsJsonParser.parseNotices(null).isEmpty())
        assertTrue(AppOpsJsonParser.parseNotices("").isEmpty())
        assertTrue(AppOpsJsonParser.parseNotices("not json").isEmpty())
        assertTrue(AppOpsJsonParser.parseNotices("""{"items":"x"}""").isEmpty())
    }

    @Test
    fun `D-183 맞지 않는 항목은 건너뛴다`() {
        val parsed = notices(
            okNotice,
            // 제목 없이 본문만
            """{"id":"body-only","closable":true,"title":null,"body":"본문만"}""",
            // 제목이 없는데 https 이미지가 아님 (preset은 안 된다)
            """{"id":"preset-only","closable":true,"title":null,"image":"preset:notice","imageAlt":"설명"}""",
            // 제목이 없는데 imageAlt가 없음
            """{"id":"no-alt","closable":true,"title":null,"image":"https://example.com/a.png"}""",
            // 빈 문자열
            """{"id":"empty-title","closable":true,"title":""}""",
            """{"id":"empty-body","closable":true,"title":"제목","body":""}""",
            """{"id":"empty-alt","closable":true,"title":"제목","imageAlt":""}""",
            """{"id":"empty-link","closable":true,"title":"제목","linkText":"","linkUrl":"https://example.com"}""",
            // closable 없음, id 규칙 위반
            """{"id":"no-closable","title":"제목"}""",
            """{"id":"Bad Id","closable":true,"title":"제목"}""",
            // 이미지만 넣은 팝업은 읽는다
            """{"id":"image-only","closable":false,"title":null,"body":null,"image":"https://example.com/a.png","imageAlt":"배너"}""",
        )

        assertEquals(listOf("ok", "image-only"), parsed.map { it.id })
        val imageOnly = parsed.last()
        assertNull(imageOnly.title)
        assertEquals(PopupImage.Web("https://example.com/a.png"), imageOnly.image)
        assertEquals("배너", imageOnly.imageAlt)
    }

    @Test
    fun `D-183 https가 아닌 링크는 숨긴다`() {
        val parsed = notices(
            """{"id":"http","closable":true,"title":"제목","linkText":"열기","linkUrl":"http://example.com"}""",
            """{"id":"js","closable":true,"title":"제목","linkText":"열기","linkUrl":"javascript:alert(1)"}""",
            """{"id":"no-text","closable":true,"title":"제목","linkUrl":"https://example.com"}""",
            """{"id":"no-url","closable":true,"title":"제목","linkText":"열기"}""",
            """{"id":"https","closable":true,"title":"제목","linkText":"열기","linkUrl":"https://example.com/a"}""",
        )

        // 항목은 남고 링크만 숨는다
        assertEquals(5, parsed.size)
        assertEquals(listOf(null, null, null, null, PopupLink("열기", "https://example.com/a")), parsed.map { it.link })
    }

    @Test
    fun `D-181 모르는 이미지 값은 이미지 없음이다`() {
        val parsed = notices(
            """{"id":"a","closable":true,"title":"제목","image":"preset:unknown"}""",
            """{"id":"b","closable":true,"title":"제목","image":"http://example.com/a.png"}""",
            """{"id":"c","closable":true,"title":"제목","image":"ftp://x"}""",
            """{"id":"d","closable":true,"title":"제목","image":"preset:update"}""",
            """{"id":"e","closable":true,"title":"제목","image":"https://example.com/a.png"}""",
        )

        assertEquals(
            listOf(
                PopupImage.None,
                PopupImage.None,
                PopupImage.None,
                PopupImage.Preset(PopupPreset.UPDATE),
                PopupImage.Web("https://example.com/a.png"),
            ),
            parsed.map { it.image },
        )
    }

    private fun rule(build: String, extra: String = "", mode: String = "FORCE", platform: String = "ANDROID", id: String = "r") =
        """{"id":"$id","mode":"$mode","platform":"$platform","build":$build,"title":"업데이트"$extra}"""

    @Test
    fun `D-177 읽을 수 없는 규칙은 건너뛴다`() {
        val parsed = rules(
            rule("""{"op":"EQ","value":10}""", id = "eq"),
            rule("""{"op":"LTE","value":10}""", id = "lte"),
            rule("""{"op":"GTE","value":10}""", id = "gte"),
            rule("""{"op":"RANGE","min":5,"max":9}""", id = "range"),
            rule("""{"op":"EQ","value":10}""", ""","osVersion":{"min":30,"max":34}""", id = "os"),
            // 건너뛴다: 모르는 op·mode·platform, 필수값 없음, 범위 오류, 빈 IN, 음수
            rule("""{"op":"NE","value":10}""", id = "bad-op"),
            rule("""{"op":"EQ","value":10}""", mode = "WARN", id = "bad-mode"),
            rule("""{"op":"EQ","value":10}""", platform = "WEB", id = "bad-platform"),
            rule("""{"op":"EQ"}""", id = "no-value"),
            rule("""{"op":"RANGE","min":9,"max":5}""", id = "inverted"),
            rule("""{"op":"RANGE","min":5}""", id = "no-max"),
            rule("""{"op":"IN","values":[]}""", id = "empty-in"),
            rule("""{"op":"EQ","value":-1}""", id = "negative"),
            """{"id":"no-build","mode":"FORCE","platform":"ANDROID","title":"업데이트"}""",
            """{"mode":"FORCE","platform":"ANDROID","build":{"op":"EQ","value":1},"title":"업데이트"}""",
            rule("""{"op":"EQ","value":10}""", ""","osVersion":{"min":null,"max":null}""", id = "empty-os"),
            // 이미지만 넣은 규칙인데 https 이미지가 없음
            """{"id":"no-image","mode":"FORCE","platform":"ANDROID","build":{"op":"EQ","value":1},"title":null,"imageAlt":"설명"}""",
        )

        assertEquals(listOf("eq", "lte", "gte", "range", "os"), parsed.map { it.id })
        assertEquals(BuildCondition.Eq(10), parsed[0].build)
        assertEquals(BuildCondition.Lte(10), parsed[1].build)
        assertEquals(BuildCondition.Gte(10), parsed[2].build)
        assertEquals(BuildCondition.Range(5, 9), parsed[3].build)
        assertEquals(OsCondition(30, 34), parsed[4].os)
    }
}
