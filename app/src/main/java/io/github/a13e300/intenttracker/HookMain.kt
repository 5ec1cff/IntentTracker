package io.github.a13e300.intenttracker

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityThread
import android.app.AndroidAppHelper
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.a13e300.intenttracker.service.IServiceFetcher
import io.github.a13e300.intenttracker.service.IntentTrackerService

class HookMain : IXposedHookLoadPackage {
    private var initialized = false
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val service by lazy { IntentTrackerServiceImpl() }
    companion object {
        lateinit var processName: String
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (initialized) return
        processName = lpparam.processName
        XposedHelpers.findAndHookMethod(
            Instrumentation::class.java,"execStartActivity",
            Context::class.java,
            IBinder::class.java,
            IBinder::class.java,
            Activity::class.java,
            Intent::class.java,
            Integer.TYPE,
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    service.dispatchStartActivity(param.args[4] as Intent)
                }
            }
        )
        XposedBridge.hookAllMethods(
            ActivityThread::class.java, "performLaunchActivity",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val acr = param.args[0] // ActivityClientRecord
                    val intent = XposedHelpers.getObjectField(acr, "intent") as? Intent ?: return
                    val activityInfo =
                        XposedHelpers.getObjectField(acr, "activityInfo") as? ActivityInfo ?: return
                    val component = ComponentName(activityInfo.packageName, activityInfo.name)
                    val referrer =
                        XposedHelpers.getObjectField(acr, "referrer") as? String ?: "null"
                    service.dispatchActivityStarted(
                        intent, component, referrer
                    )
                }
            }
        )
        handler.post {
            AndroidAppHelper.currentApplication().registerReceiverCompat(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        logD("intent=$intent")
                        if (intent.action == IntentTrackerService.ACTION_REQUIRE_SERVICE) {
                            val binder =
                                intent.extras?.getBinder(IntentTrackerService.KEY_SERVICE_FETCHER)
                                    ?: return
                            kotlin.runCatching {
                                IServiceFetcher.Stub.asInterface(binder)
                                    .publishService(service.asBinder())
                            }.onFailure {
                                logE("failed to publish to remote $binder", it)
                            }
                        }
                    }

                },
                IntentFilter(IntentTrackerService.ACTION_REQUIRE_SERVICE),
                "android.permission.INTERACT_ACROSS_USERS",
                0
            )
        }
        initialized = true
    }
}