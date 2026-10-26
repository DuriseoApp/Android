package com.duriseo.android.core.designsystem.message

import com.duriseo.android.core.designsystem.R
import com.duriseo.android.domain.error.DataError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ErrorMessagesTest {

    /** strings.xml을 직접 읽는다 (Robolectric 없이 문구 값을 확인하기 위해). 테스트 작업 폴더는 모듈 폴더다. */
    private fun stringsXml(): String = File("src/main/res/values/strings.xml").readText(Charsets.UTF_8)

    @Test
    fun `NFR-OFF-02 Offline 오류는 인터넷 연결이 필요해요 문구로 바뀐다`() {
        assertEquals(R.string.offline_required, DataError.Offline.toMessageRes())
    }

    @Test
    fun `NFR-COPY-01 오프라인 안내 문구는 PRD 문구 그대로 해요체다`() {
        assertTrue(stringsXml().contains("<string name=\"offline_required\">인터넷 연결이 필요해요</string>"))
    }

    @Test
    fun `API 5장 서버 오류는 code와 관계없이 공통 오류 문구로 바뀐다`() {
        val known = DataError.ApiError(code = "INTERNAL_ERROR", details = null, traceId = "t1")
        val unknown = DataError.ApiError(code = "SOMETHING_NEW", details = null, traceId = null)

        assertEquals(R.string.error_common, known.toMessageRes())
        assertEquals(R.string.error_common, unknown.toMessageRes())
        assertTrue(stringsXml().contains("<string name=\"error_common\">문제가 생겼어요. 잠시 후 다시 시도해 주세요</string>"))
    }

    @Test
    fun `D-30 탭 라벨 문구는 캘린더와 메모다`() {
        val xml = stringsXml()
        assertTrue(xml.contains("<string name=\"tab_calendar\">캘린더</string>"))
        assertTrue(xml.contains("<string name=\"tab_memo\">메모</string>"))
    }
}
