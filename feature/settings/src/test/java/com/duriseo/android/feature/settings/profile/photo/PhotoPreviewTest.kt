package com.duriseo.android.feature.settings.profile.photo

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoPreviewTest {

    @Test
    fun `FR-AUTH-07 AC6 작은 사진은 줄이지 않고 읽는다`() {
        assertEquals(1, PhotoPreview.sampleSizeFor(width = 600, height = 800, targetSide = 512))
        assertEquals(1, PhotoPreview.sampleSizeFor(width = 512, height = 512, targetSide = 512))
    }

    @Test
    fun `FR-AUTH-07 AC6 큰 사진은 짧은 변이 목표 아래로 내려가지 않는 가장 큰 2의 거듭제곱으로 줄여 읽는다`() {
        // 짧은 변 4000 -> 2배로 2000, 4배로 1000, 8배로 500(<512) 이므로 4
        assertEquals(4, PhotoPreview.sampleSizeFor(width = 6000, height = 4000, targetSide = 512))
        // 가로로 긴 사진도 짧은 변 기준이다
        assertEquals(1, PhotoPreview.sampleSizeFor(width = 8000, height = 600, targetSide = 512))
    }
}
