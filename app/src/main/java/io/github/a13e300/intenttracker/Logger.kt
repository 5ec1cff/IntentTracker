package io.github.a13e300.intenttracker

import android.util.Log

private const val TAG = "IntentTracker"

fun logD(msg: String) {
    Log.d(TAG, msg)
}

fun logE(msg: String, throwable: Throwable) {
    Log.e(TAG, msg, throwable)
}

fun logE(msg: String) {
    Log.e(TAG, msg)
}
