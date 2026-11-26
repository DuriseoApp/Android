package com.duriseo.android.core.common.image

import android.content.Context
import coil3.request.ImageRequest
import coil3.size.Size

/**
 * 팝업 `https://` 이미지의 Coil 요청. 미리 받기(`PopupImagePrefetcher`)와 화면(`AsyncImage`)이 **같은 함수**로 만든다.
 * 메모리 캐시 키에 요청 크기가 들어가서, 두 쪽의 크기가 다르면 미리 받아도 캐시가 맞지 않아 팝업이 뜬 뒤에 이미지가 나온다 (Q-23).
 * 그래서 화면 크기에 맞추지 않고 원본 크기([Size.ORIGINAL])로 받고 그린다.
 */
fun popupImageRequest(context: Context, url: String): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(Size.ORIGINAL)
        .build()
