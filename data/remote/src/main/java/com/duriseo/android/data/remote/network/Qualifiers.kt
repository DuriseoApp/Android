package com.duriseo.android.data.remote.network

import javax.inject.Qualifier

/**
 * "인증 없음" OkHttp/Retrofit. `Authorization`을 붙이지 않는다.
 * `/auth/guest`, `/auth/social`, `/auth/refresh`가 쓴다 (API.md 1장).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoAuth

/** "인증" OkHttp/Retrofit. `Authorization: Bearer <access>`를 붙이고 401 `TOKEN_EXPIRED`면 토큰을 갱신한다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Authed
