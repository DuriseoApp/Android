package com.duriseo.android.feature.settings.profile.photo

import kotlin.math.floor
import kotlin.math.sqrt

/** 사진의 가로·세로 픽셀 수 */
internal data class PixelSize(val width: Int, val height: Int) {
    val pixels: Long get() = width.toLong() * height
}

/**
 * 픽셀 수가 [maxPixels]를 넘으면 가로세로 비율을 지키며 [maxPixels] 안으로 줄인 크기를, 넘지 않으면 null(줄일 필요 없음)을 돌려준다
 * (D-153, NFR-FILE-01: 서버는 25MP를 넘으면 413 `IMAGE_TOO_LARGE`다). 순수 함수라 Android 없이 테스트한다.
 *
 * 비율은 `sqrt(max / (w * h))`로 가로세로를 같게 줄이고 내린다. 내림이라 결과 픽셀 수는 [maxPixels] 이하다. 한 변이 1 아래로 내려가는
 * 극단적으로 가는 사진은 그 변을 1로 두므로 이 보장이 없다 (그런 사진은 서버가 거른다).
 */
internal fun fitWithinPixels(width: Int, height: Int, maxPixels: Long): PixelSize? {
    if (width <= 0 || height <= 0) return null
    val pixels = width.toLong() * height
    if (pixels <= maxPixels) return null
    val scale = sqrt(maxPixels.toDouble() / pixels.toDouble())
    val newWidth = floor(width * scale).toInt().coerceAtLeast(1)
    val newHeight = floor(height * scale).toInt().coerceAtLeast(1)
    return PixelSize(newWidth, newHeight)
}
