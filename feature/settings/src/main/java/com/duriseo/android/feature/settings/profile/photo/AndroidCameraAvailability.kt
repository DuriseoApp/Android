package com.duriseo.android.feature.settings.profile.photo

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 기기에 사진 찍기(`ACTION_IMAGE_CAPTURE`)를 받아 줄 앱이 있는지 본다 (unit-8.md 확인 필요 4).
 * 매니페스트의 `<queries>`가 있어서 Android 11 이상에서도 찾을 수 있다. 권한은 필요 없다 (D-149).
 */
class AndroidCameraAvailability @Inject constructor(
    @ApplicationContext private val context: Context,
) : CameraAvailability {
    override fun isAvailable(): Boolean =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null
}
