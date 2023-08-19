package io.github.a13e300.intenttracker.cli

import android.app.IActivityManager
import android.content.Intent
import android.os.IBinder
import android.os.ServiceManager
import androidx.core.os.bundleOf
import io.github.a13e300.intenttracker.broadcastIntentCompat
import io.github.a13e300.intenttracker.print
import io.github.a13e300.intenttracker.service.ActivityStartedInfo
import io.github.a13e300.intenttracker.service.IIntentTrackerListener
import io.github.a13e300.intenttracker.service.IIntentTrackerService
import io.github.a13e300.intenttracker.service.IServiceFetcher
import io.github.a13e300.intenttracker.service.IntentTrackerService
import io.github.a13e300.intenttracker.service.StartActivityInfo
import java.util.Scanner

class ServiceFetcher : IServiceFetcher.Stub() {
    private val mServices = mutableSetOf<IBinder>()
    override fun publishService(binder: IBinder) {
        kotlin.runCatching {
            val service = IIntentTrackerService.Stub.asInterface(binder)
            if (service.version != IntentTrackerService.CURRENT_VERSION) {
                println("remote version not match")
            } else {
                synchronized(this) {
                    if (mServices.contains(binder)) return
                    mServices.add(binder)
                }
                val serviceInfo = service.serviceInfo
                println("service registered: $serviceInfo")
                service.registerListener(
                    object : IIntentTrackerListener.Stub() {
                        override fun onStartActivity(info: StartActivityInfo) {
                            println("start activity from $serviceInfo")
                            info.intent.print()
                            println("stack trace:")
                            info.stackTraceElements.forEach {
                                println("  ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})")
                            }
                            println()
                        }

                        override fun onActivityStarted(info: ActivityStartedInfo) {
                            println("activity ${info.component} started in $serviceInfo referrer=${info.referrer}")
                            info.intent.print()
                            println("stack trace:")
                            info.stackTraceElements.forEach {
                                println("  ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})")
                            }
                            println()
                        }
                    },
                    IntentTrackerService.FLAG_LISTEN_START_ACTIVITY or IntentTrackerService.FLAG_GET_STACK_TRACE or IntentTrackerService.FLAG_LISTEN_ACTIVITY_STARTED
                )
                binder.linkToDeath(
                    {
                        println("$serviceInfo died")
                        synchronized(this) {
                            mServices.remove(binder)
                        }
                    }, 0
                )
            }
        }.onFailure {
            println("failed to get service")
            it.printStackTrace()
        }
    }
}

val am: IActivityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"))
val fetcher = ServiceFetcher()

fun discoveryServices() {
    am.broadcastIntentCompat(Intent(
        IntentTrackerService.ACTION_REQUIRE_SERVICE
    ).apply {
        putExtras(bundleOf(IntentTrackerService.KEY_SERVICE_FETCHER to fetcher))
    })
}

fun main(args: Array<String>) {
    discoveryServices()
    val scanner = Scanner(System.`in`)
    while (scanner.hasNextLine()) {
        val l = scanner.nextLine()
        when (l) {
            "r" -> discoveryServices()
            "q" -> break
        }
    }
}