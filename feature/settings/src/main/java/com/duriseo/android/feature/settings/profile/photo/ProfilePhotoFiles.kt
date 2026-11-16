package com.duriseo.android.feature.settings.profile.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * 카메라 앱(`TakePicture`)이 사진을 쓸 임시 파일 (FR-AUTH-07 AC9, D-149). 앱 캐시 아래 `profile_photo/`에만 만들고
 * `FileProvider`로 그 파일의 Uri만 카메라 앱에 건넨다. 그래서 저장소·카메라 권한을 요청하지 않는다.
 * 매니페스트의 provider(`authorities = ${applicationId}.profilephoto`)와 `res/xml/profile_photo_paths.xml`이 짝이다.
 * 올리고 나면 [clear]로 지운다.
 */
object ProfilePhotoFiles {
    /** authority는 `<applicationId>.profilephoto`. 매니페스트의 `${applicationId}.profilephoto`와 같아야 한다 */
    const val AUTHORITY_SUFFIX = ".profilephoto"

    /** 앱 캐시 아래 폴더 이름. `res/xml/profile_photo_paths.xml`의 `cache-path`와 같아야 한다 */
    const val CACHE_FOLDER = "profile_photo"

    /** 카메라가 쓸 새 파일과 그 Uri. 파일은 아직 비어 있다 */
    fun newCameraTarget(context: Context): CameraTarget {
        val folder = File(context.cacheDir, CACHE_FOLDER).apply { mkdirs() }
        val file = File.createTempFile("camera_", ".jpg", folder)
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        return CameraTarget(file, uri)
    }

    /** 임시 사진을 모두 지운다 (올린 뒤·취소한 뒤) */
    fun clear(context: Context) {
        File(context.cacheDir, CACHE_FOLDER).listFiles()?.forEach { it.delete() }
    }
}

class CameraTarget(val file: File, val uri: Uri)
