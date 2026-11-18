package com.duriseo.android.feature.settings.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSheetTest {

    @Test
    fun `FR-AUTH-07 AC9 카메라 앱이 없으면 사진 찍기를 숨긴다`() {
        assertEquals(
            listOf(PhotoSheetRow.Album, PhotoSheetRow.DefaultImage),
            photoSheetRows(cameraAvailable = false),
        )
    }

    @Test
    fun `FR-AUTH-07 AC9 카메라 앱이 있으면 앨범 사진 찍기 기본 이미지 순서로 보인다`() {
        assertEquals(
            listOf(PhotoSheetRow.Album, PhotoSheetRow.Camera, PhotoSheetRow.DefaultImage),
            photoSheetRows(cameraAvailable = true),
        )
    }
}
