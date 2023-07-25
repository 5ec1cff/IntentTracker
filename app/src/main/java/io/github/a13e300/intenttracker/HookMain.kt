package io.github.a13e300.intenttracker

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AndroidAppHelper
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
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
        handler.post {
            AndroidAppHelper.currentApplication().registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        logD("intent=$intent")
                        if (intent.action == IntentTrackerService.ACTION_REQUIRE_SERVICE) {
                            val binder = intent.extras?.getBinder(IntentTrackerService.KEY_SERVICE_FETCHER) ?: return
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
                null
            )
        }
        initialized = true
    }
}