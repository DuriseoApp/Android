package com.duriseo.android.feature.settings.profile.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 소스 매니페스트(병합 전) 기준 확인 (FR-AUTH-07 AC9, D-149). 병합 매니페스트는 빌드 산출물이라 단위 테스트에서 읽지 않는다.
 * 레포의 모든 모듈 `src/main/AndroidManifest.xml`을 훑는다. 소셜 SDK 같은 라이브러리가 병합 때 더하는 권한은 여기서 보지 못한다.
 */
class ManifestPermissionTest {
    /** 단위 테스트의 작업 폴더는 이 모듈(`feature/settings`)이라 레포 루트는 두 단계 위다 */
    private val root = File("../..").canonicalFile

    private fun sourceManifests(): List<File> = root.walkTopDown()
        .onEnter { it.name != "build" && it.name != ".gradle" && it.name != ".git" }
        .filter { it.name == "AndroidManifest.xml" && it.parentFile.name == "main" && it.parentFile.parentFile.name == "src" }
        .toList()

    @Test
    fun `FR-AUTH-07 AC9 카메라와 저장소 권한을 선언하지 않는다`() {
        val manifests = sourceManifests()
        // 앱과 이 모듈의 매니페스트는 반드시 찾아야 한다 (경로가 틀려 아무것도 못 읽고 통과하는 것을 막는다)
        assertTrue("앱 매니페스트를 찾지 못했어요: $root", manifests.any { it.path.contains("${File.separator}app${File.separator}src") })
        assertTrue(manifests.any { it.path.contains("${File.separator}feature${File.separator}settings${File.separator}") })

        val forbidden = listOf(
            "android.permission.CAMERA",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
        )
        for (manifest in manifests) {
            val text = manifest.readText()
            for (permission in forbidden) {
                assertFalse("${manifest.path}에 $permission 이 있어요", text.contains(permission))
            }
        }
    }

    @Test
    fun `FR-AUTH-07 AC9 FileProvider는 applicationId 기반 authority로 앱 캐시 폴더만 연다`() {
        val manifest = File(root, "feature/settings/src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("androidx.core.content.FileProvider"))
        assertTrue(manifest.contains("android:authorities=\"\${applicationId}${ProfilePhotoFiles.AUTHORITY_SUFFIX}\""))
        // 다른 앱이 직접 열지 못하고 Uri 권한만 건넨다
        assertTrue(manifest.contains("android:exported=\"false\""))
        assertTrue(manifest.contains("android:grantUriPermissions=\"true\""))

        val paths = File(root, "feature/settings/src/main/res/xml/profile_photo_paths.xml").readText()
        assertTrue(paths.contains("<cache-path"))
        assertTrue(paths.contains("path=\"${ProfilePhotoFiles.CACHE_FOLDER}/\""))
        // 앱 캐시 밖(외부 저장소·files)은 열지 않는다
        assertFalse(paths.contains("external"))
        assertFalse(paths.contains("<files-path"))
        assertEquals(1, Regex("<cache-path").findAll(paths).count())
    }
}
