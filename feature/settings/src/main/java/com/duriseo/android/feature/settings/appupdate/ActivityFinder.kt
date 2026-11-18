package com.duriseo.android.feature.settings.appupdate

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Compose의 `LocalContext`를 감싼 Context에서 Activity를 찾는다. 없으면 null */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
