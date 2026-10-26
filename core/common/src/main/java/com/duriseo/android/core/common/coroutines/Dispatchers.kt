package com.duriseo.android.core.common.coroutines

import javax.inject.Qualifier

/** IO 작업용 디스패처 한정자. 테스트에서 바꿔 끼울 수 있게 `Dispatchers.IO`를 직접 쓰지 않고 주입받는다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
