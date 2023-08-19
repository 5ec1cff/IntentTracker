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

val am: IActivityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"))

fun discoveryServices(fetcher: IServiceFetcher) {
    am.broadcastIntentCompat(Intent(
        IntentTrackerService.ACTION_REQUIRE_SERVICE
    ).apply {
        putExtras(bundleOf(IntentTrackerService.KEY_SERVICE_FETCHER to fetcher))
    })
}

fun main(args: Array<String>) {
    val options = Options()
    options.addOption(
        Option.builder("sa").longOpt("start-activity").desc("monitor startActivity").hasArg(false)
            .build()
    )
    options.addOption(
        Option.builder("as").longOpt("activity-start").desc("monitor activities started")
            .hasArg(false).build()
    )
    options.addOption(
        Option.builder("st").longOpt("stack-trace").desc("get stack traces").hasArg(false).build()
    )
    val parser = DefaultParser()
    val cmd = parser.parse(options, args)
    val helpFormatter = HelpFormatter()
    var flags = 0
    if (cmd.hasOption("start-activity")) {
        flags = flags or IntentTrackerService.FLAG_LISTEN_START_ACTIVITY
    }
    if (cmd.hasOption("activity-start")) {
        flags = flags or IntentTrackerService.FLAG_LISTEN_ACTIVITY_STARTED
    }
    if (flags == 0) {
        println("Neither start-activity nor activity-start is specified!")
        helpFormatter.printHelp("itc", options)
        return
    }
    if (cmd.hasOption("stack-trace")) {
        flags = flags or IntentTrackerService.FLAG_GET_STACK_TRACE
    }
    val fetcher = ServiceFetcher(flags)
    println("r -> re-discovery, q -> exit")
    discoveryServices(fetcher)
    val scanner = Scanner(System.`in`)
    while (scanner.hasNextLine()) {
        val l = scanner.nextLine()
        when (l) {
            "r" -> discoveryServices(fetcher)
            "q" -> break
        }
    }
}