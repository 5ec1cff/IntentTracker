package io.github.a13e300.intenttracker

import android.app.ActivityThread
import android.app.IActivityManager
import android.content.Context
import android.content.IIntentReceiver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import java.io.File
import java.io.PrintStream


fun prepareSystemContext(): Context {
    // On MIUI it always prints an error when call systemMain(), throw it to black hole
    val origErr = System.err
    val newErr = PrintStream(File("/dev/null"))
    System.setErr(newErr)
    Looper.prepareMainLooper()
    val at = ActivityThread.systemMain()
    System.setErr(origErr)
    newErr.close()
    return at.systemContext
}

fun IActivityManager.broadcastIntentCompat(
    intent: Intent?, callingFeatureId: String? = null,
    resolvedType: String? = null, resultTo: IIntentReceiver? = null, resultCode: Int = 0,
    resultData: String? = null, map: Bundle? = null, requiredPermissions: Array<String?>? = null,
    appOp: Int = -1, options: Bundle? = null, serialized: Boolean = true, sticky: Boolean = false,
    userId: Int = 0 // TODO: support multi user
): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            broadcastIntentWithFeature(
                null, callingFeatureId, intent, resolvedType, resultTo,
                resultCode, resultData, null, requiredPermissions, null, null, appOp, null,
                serialized, sticky, userId
            )
        } catch (ignored: NoSuchMethodError) {
            broadcastIntentWithFeature(
                null, callingFeatureId, intent, resolvedType, resultTo,
                resultCode, resultData, null, requiredPermissions, null, appOp, null,
                serialized, sticky, userId
            )
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        broadcastIntentWithFeature(
            null,
            callingFeatureId,
            intent,
            resolvedType,
            resultTo,
            resultCode,
            resultData,
            map,
            requiredPermissions,
            appOp,
            options,
            serialized,
            sticky,
            userId
        )
    } else {
        broadcastIntent(
            null,
            intent,
            resolvedType,
            resultTo,
            resultCode,
            resultData,
            map,
            requiredPermissions,
            appOp,
            options,
            serialized,
            sticky,
            userId
        )
    }
}