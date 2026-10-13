package com.juco.duriseo

import timber.log.Timber
import android.util.Log
//import com.google.firebase.crashlytics.FirebaseCrashlytics



class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // VERBOSE = 사소한로그 (제외)
        if (priority == Log.VERBOSE) return

//        val crashlytics = FirebaseCrashlytics.getInstance()

        val logMessage = "[$tag] $message"
//        crashlytics.log(logMessage)

        if (priority == Log.ERROR || priority == Log.WARN) {
            if (t != null) {
//                crashlytics.recordException(t)
            } else {
//                crashlytics.recordException(Exception(message))
            }
        }
    }
}
