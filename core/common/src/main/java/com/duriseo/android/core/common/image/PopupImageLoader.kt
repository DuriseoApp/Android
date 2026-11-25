package com.duriseo.android.core.common.image

import javax.inject.Qualifier

/**
 * 공지·업데이트 팝업의 `https://` 이미지를 받는 Coil `ImageLoader` 한정자 (D-181, unit-9 결정 3).
 * 우리 서버가 아닐 수도 있는 주소라서 토큰도 공통 헤더도 붙이지 않는 별도 OkHttp로 받는다.
 * 프로필 사진용 기본 `ImageLoader`(인증 OkHttp)를 쓰면 안 된다. 미리 받기(`PopupImagePrefetcher`)와 화면이 같은 로더를 쓰면 캐시가 맞는다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PopupImageLoader
