package com.duriseo.android.core.common.coroutines

import javax.inject.Qualifier

/** IO 작업용 디스패처 한정자. 테스트에서 바꿔 끼울 수 있게 `Dispatchers.IO`를 직접 쓰지 않고 주입받는다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** 앱 프로세스 수명의 코루틴 범위 한정자. 화면과 상관없이 끝까지 해야 하는 일(세션 정리, 앱 시작 작업)에 쓴다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
