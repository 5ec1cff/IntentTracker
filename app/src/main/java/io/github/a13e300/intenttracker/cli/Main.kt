package io.github.a13e300.intenttracker.cli

import android.app.ActivityThread
import android.app.IActivityManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.os.ServiceManager
import androidx.core.os.bundleOf
import io.github.a13e300.intenttracker.broadcastIntentCompat
import io.github.a13e300.intenttracker.print
import io.github.a13e300.intenttracker.service.IIntentTrackerListener
import io.github.a13e300.intenttracker.service.IIntentTrackerService
import io.github.a13e300.intenttracker.service.IServiceFetcher
import io.github.a13e300.intenttracker.service.IntentTrackerService
import io.github.a13e300.intenttracker.service.StartActivityInfo
import java.io.File
import java.io.PrintStream

class ServiceFetcher : IServiceFetcher.Stub() {
    override fun publishService(binder: IBinder) {
        kotlin.runCatching {
            val service = IIntentTrackerService.Stub.asInterface(binder)
            if (service.version != IntentTrackerService.CURRENT_VERSION) {
                println("remote version not match")
            } else {
                println("service registered: ${service.serviceInfo}")
                service.registerListener(object : IIntentTrackerListener.Stub() {
                    override fun onStartActivity(info: StartActivityInfo) {
                        println("start activity from ${service.serviceInfo}")
                        info.intent.print()
                        println("stack trace:")
                        info.stackTraceElements.forEach {
                            println("  ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})")
                        }
                        println()
                    }
                }, IntentTrackerService.FLAG_LISTEN_START_ACTIVITY or IntentTrackerService.FLAG_GET_STACK_TRACE)
            }
        }.onFailure {
            println("failed to get service")
            it.printStackTrace()
        }
    }
}

fun main(args: Array<String>) {
    Looper.prepareMainLooper()
    val am = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"))
    am.broadcastIntentCompat(Intent(
        IntentTrackerService.ACTION_REQUIRE_SERVICE
    ).apply {
        putExtras(bundleOf(IntentTrackerService.KEY_SERVICE_FETCHER to ServiceFetcher()))
    })
    Looper.loop()
}