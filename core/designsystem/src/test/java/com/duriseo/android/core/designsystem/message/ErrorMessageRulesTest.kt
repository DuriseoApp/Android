package com.duriseo.android.core.designsystem.message

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 오류 문구 규칙 지키기: 화면은 `DataError.toMessageResOrNull()`로만 스낵바 문구를 얻는다. 426 `APP_VERSION_UNSUPPORTED`와 세션 종료 코드는
 * 문구가 없는데(강제 업데이트 팝업이 대신 뜬다, API.md 4.9 7단계), `toMessageRes()`를 바로 쓰면 공통 오류 스낵바가 같이 뜬다.
 * `feature/`·`app/`의 main 소스를 훑는다. `PhotoError.toMessageRes()`(사진 오류 문구)는 다른 함수라 예외다.
 */
class ErrorMessageRulesTest {
    private val root = File("../..").canonicalFile

    @Test
    fun `FR-APP-02 AC6 화면은 DataError toMessageRes를 바로 쓰지 않고 toMessageResOrNull을 쓴다`() {
        val sources = listOf("feature", "app").map { File(root, it) }.filter { it.exists() }.flatMap { dir ->
            dir.walkTopDown()
                .onEnter { it.name != "build" && it.name != ".gradle" }
                .filter { it.isFile && it.extension == "kt" && it.path.contains("${File.separator}src${File.separator}main${File.separator}") }
                .toList()
        }
        assertTrue("feature 소스를 찾지 못했어요: $root", sources.any { it.path.contains("${File.separator}feature${File.separator}") })

        val violations = sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val code = line.trim()
                if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) return@mapIndexedNotNull null
                val direct = Regex("""(?<!OrNull)\.toMessageRes\(\)""").containsMatchIn(code)
                if (direct && !code.contains("photoErrorOrNull")) "${file.relativeTo(root).path}:${index + 1}" else null
            }
        }
        assertTrue("toMessageResOrNull()?.let { … }로 바꿔 주세요(426에는 스낵바가 없어요):\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
