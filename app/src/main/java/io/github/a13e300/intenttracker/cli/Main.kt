package io.github.a13e300.intenttracker.cli

import android.app.ActivityManagerHidden
import android.app.IActivityController
import android.app.IActivityManager
import android.app.IActivityTaskManager
import android.app.IAssistDataReceiver
import android.app.assist.AssistContent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.os.ServiceManager
import androidx.core.os.bundleOf
import io.github.a13e300.intenttracker.broadcastIntentCompat
import io.github.a13e300.intenttracker.getParcelableCompat
import io.github.a13e300.intenttracker.print
import io.github.a13e300.intenttracker.service.ActivityStartedInfo
import io.github.a13e300.intenttracker.service.IIntentTrackerListener
import io.github.a13e300.intenttracker.service.IIntentTrackerService
import io.github.a13e300.intenttracker.service.IServiceFetcher
import io.github.a13e300.intenttracker.service.IntentTrackerService
import io.github.a13e300.intenttracker.service.StartActivityInfo
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.util.Scanner

class ServiceFetcher(private val flags: Int) : IServiceFetcher.Stub() {
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
                    flags
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

class MyActivityController : IActivityController.Stub() {
    override fun activityStarting(intent: Intent?, pkg: String?): Boolean {
        println("ActivityController: starting activity in $pkg, intent=$intent")
        return true
    }

    override fun activityResuming(pkg: String?): Boolean = true

    override fun appCrashed(
        processName: String?,
        pid: Int,
        shortMsg: String?,
        longMsg: String?,
        timeMillis: Long,
        stackTrace: String?
    ): Boolean = true

    override fun appEarlyNotResponding(processName: String?, pid: Int, annotation: String?): Int = 0

    override fun appNotResponding(processName: String?, pid: Int, processStats: String?): Int = 0

    override fun systemNotResponding(msg: String?): Int = 0

}

val am: IActivityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"))

fun discoveryServices(fetcher: IServiceFetcher) {
    am.broadcastIntentCompat(Intent(
        IntentTrackerService.ACTION_REQUIRE_SERVICE
    ).apply {
        putExtras(bundleOf(IntentTrackerService.KEY_SERVICE_FETCHER to fetcher))
    })
}

// https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/wm/ActivityTaskManagerInternal.java;l=115;drc=2fe80cbc458c12a418e438f9c7dc576942abb561
const val ASSIST_KEY_STRUCTURE = "structure"
const val ASSIST_TASK_ID = "taskId"
const val ASSIST_KEY_CONTENT = "content"

fun printAssistContentExtras(bundle: Bundle?) {
    if (bundle == null) {
        println("got null assist content extras, maybe timeout?")
        return
    }
    // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/wm/ActivityTaskManagerService.java;l=2776;drc=8bc4eeaea4a778d80c44054d3cf3c9d6d2bbe28e
    // val struct = bundle.getParcelableCompat(ASSIST_KEY_STRUCTURE, AssistStructure::class.java)
    var printSomething = false
    println("keys in bundle: ${bundle.keySet().joinToString(",")}")
    bundle.getParcelableCompat(ASSIST_KEY_CONTENT, AssistContent::class.java)?.let {
        it.intent.print()
        printSomething = true
    }
    if (!printSomething) {
        println("nothing received")
    }
}

fun main(args: Array<String>) {
    val options = Options().apply {
        addOption(
            Option.builder("sa").longOpt("start-activity").desc("monitor startActivity")
                .hasArg(false)
                .build()
        )
        addOption(
            Option.builder("as").longOpt("activity-start").desc("monitor activities started")
                .hasArg(false).build()
        )
        addOption(
            Option.builder("st").longOpt("stack-trace").desc("get stack traces").hasArg(false)
                .build()
        )
        addOption(
            Option.builder("ac").longOpt("activity-controller")
                .desc("use IActivityController to monitor (no xposed required)").hasArg(false)
                .build()
        )
        addOption(
            Option.builder("ace").longOpt("assist-context-extras")
                .desc("")
                .hasArg(false)
                .build()
        )
    }
    val parser = DefaultParser()
    val cmd = parser.parse(options, args)
    val helpFormatter = HelpFormatter()
    var flags = 0
    val requiredOptions =
        arrayListOf("start-activity", "activity-start", "activity-controller", "ace")
    if (requiredOptions.all { !cmd.hasOption(it) }) {
        println("Required at least 1 of these options: ${requiredOptions.joinToString(",")}")
        helpFormatter.printHelp("itc", options)
        return
    }
    if (cmd.hasOption("ace")) {
        val atm = IActivityTaskManager.Stub.asInterface(ServiceManager.getService("activity_task"))
        println("fetching assist struct ...")
        val lock = Object()
        var received = false
        atm.requestAssistContextExtras(
            ActivityManagerHidden.ASSIST_CONTEXT_FULL, object : IAssistDataReceiver.Stub() {
                override fun onHandleAssistData(resultData: Bundle?) {
                    synchronized(lock) {
                        printAssistContentExtras(resultData)
                        received = true
                        lock.notifyAll()
                    }
                }

                override fun onHandleAssistScreenshot(screenshot: Bitmap?) {

                }

            }, null, null, true, true
        )
        // printAssistContentExtras(atm.getAssistContextExtras(ActivityManagerHidden.ASSIST_CONTEXT_FULL))
        synchronized(lock) {
            while (!received) {
                lock.wait()
            }
        }
        return
    }
    if (cmd.hasOption("start-activity")) {
        flags = flags or IntentTrackerService.FLAG_LISTEN_START_ACTIVITY
    }
    if (cmd.hasOption("activity-start")) {
        flags = flags or IntentTrackerService.FLAG_LISTEN_ACTIVITY_STARTED
    }
    if (cmd.hasOption("stack-trace")) {
        flags = flags or IntentTrackerService.FLAG_GET_STACK_TRACE
    }
    if (cmd.hasOption("activity-controller")) {
        am.setActivityController(MyActivityController(), false)
    }
    val fetcher = ServiceFetcher(flags)
    if (flags and (IntentTrackerService.FLAG_LISTEN_ACTIVITY_STARTED or IntentTrackerService.FLAG_LISTEN_START_ACTIVITY) != 0)
        discoveryServices(fetcher)
    val scanner = Scanner(System.`in`)
    while (scanner.hasNextLine()) {
        println("r -> re-discovery, q -> exit")
        val l = scanner.nextLine()
        when (l) {
            "r" -> discoveryServices(fetcher)
            "q" -> break
        }
    }
}