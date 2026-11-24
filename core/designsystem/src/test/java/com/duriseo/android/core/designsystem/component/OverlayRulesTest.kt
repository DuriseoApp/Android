package com.duriseo.android.core.designsystem.component

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 덮개 규칙 지키기: 하단 시트·다이얼로그·요청 중 덮개는 공통 레이어(별도 창)로만 그린다.
 * 화면 안에 scrim `Box`나 M3 `ModalBottomSheet`·`AlertDialog`를 직접 쓰면 하단 탭이 덮개 위에 그려져 시트가 탭에 가려진다.
 * `feature/`, `app/`, `core/`(designsystem 밖 모듈)의 main 소스를 훑는다. 예외가 꼭 필요하면 [allowed]에 파일 경로 조각과 이유를 적는다.
 */
class OverlayRulesTest {
    /** 단위 테스트의 작업 폴더는 이 모듈(`core/designsystem`)이라 레포 루트는 두 단계 위다 */
    private val root = File("../..").canonicalFile

    private val forbidden = listOf(
        "colorScheme.scrim",
        "ModalBottomSheet(",
        "AlertDialog(",
        "BasicAlertDialog(",
        "DatePickerDialog(",
        "TimePickerDialog(",
        "DropdownMenu(",
        // Dialog·Popup·DialogProperties 등 창 API는 와일드카드 import까지 모두 막는다
        "import androidx.compose.ui.window.",
        "DuriseoOverlayWindow(",
    )

    /** 허용 목록: 경로 조각 -> 이유. 비어 있어야 정상이다 */
    private val allowed = emptyMap<String, String>()

    private fun mainSources(): List<File> = listOf("feature", "app", "core")
        .map { File(root, it) }
        .filter { it.exists() }
        .flatMap { dir ->
            dir.walkTopDown()
                // 덮개를 그리는 곳인 core/designsystem은 대상이 아니다
                .onEnter { it.name != "build" && it.name != ".gradle" && !(it.name == "designsystem" && it.parentFile.name == "core") }
                .filter { it.isFile && it.extension == "kt" && it.path.contains("${File.separator}src${File.separator}main${File.separator}") }
                .toList()
        }

    @Test
    fun `OVERLAY-01 feature와 app은 덮개 scrim과 M3 시트·다이얼로그·별도 창을 직접 쓰지 않는다`() {
        val sources = mainSources()
        // 경로가 틀려 아무것도 못 읽고 통과하는 것을 막는다
        for (part in listOf("feature${File.separator}calendar", "feature${File.separator}main", "app${File.separator}src", "core${File.separator}push")) {
            assertTrue("$part 소스를 찾지 못했어요: $root", sources.any { it.path.contains("${File.separator}$part${File.separator}") })
        }

        val violations = sources.flatMap { file ->
            if (allowed.keys.any { file.path.contains(it) }) return@flatMap emptyList()
            file.readLines().mapIndexedNotNull { index, line ->
                val code = line.trim()
                if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) return@mapIndexedNotNull null
                forbidden.firstOrNull { code.contains(it) }?.let { "${file.relativeTo(root).path}:${index + 1} `$it`" }
            }
        }
        assertTrue(
            "덮개는 DuriseoBottomSheetLayer·DuriseoDialogLayer(DuriseoDialog)·DuriseoLoadingLayer로만 그려요:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }
}
