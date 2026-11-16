package com.duriseo.android.core.common.config

import javax.inject.Qualifier

/**
 * 오픈소스 라이선스 JSON의 리소스 ID(`R.raw.aboutlibraries`) 한정자 (SCR-SET-05, unit-8.md 결정 30).
 * 그 리소스는 `app` 모듈의 AboutLibraries 플러그인이 만들어 app의 R에만 있다. app의 `AppModule`이 컴파일 타임에 직접 참조해 넘기므로
 * 플러그인이 JSON을 못 만들면 빌드가 깨지고, 리소스 축소(R8)에서도 쓰는 리소스로 남는다. `feature:settings`는 이 값만 받아 읽는다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LicensesJsonRes
