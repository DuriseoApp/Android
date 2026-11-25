package com.duriseo.android.core.common.config

/**
 * 이 기기의 OS 버전: Android API 레벨(`Build.VERSION.SDK_INT`). 공통 요청 헤더 `X-OS-Version`과 기기 등록의 `osVersion`이 쓴다
 * (API.md 1장, 3.1, D-182). 값은 app의 Hilt 모듈이 넘기고 테스트는 임의 값을 넣는다.
 */
data class OsVersion(val apiLevel: Int)
