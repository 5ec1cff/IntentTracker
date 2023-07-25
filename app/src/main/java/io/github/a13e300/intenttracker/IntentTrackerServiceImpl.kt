package io.github.a13e300.intenttracker

import android.content.Intent
import android.os.IBinder
import android.os.Process
import io.github.a13e300.intenttracker.service.IIntentTrackerListener
import io.github.a13e300.intenttracker.service.IIntentTrackerService
import io.github.a13e300.intenttracker.service.IntentTrackerService
import io.github.a13e300.intenttracker.service.ServiceInfo
import io.github.a13e300.intenttracker.service.StartActivityInfo
import java.util.concurrent.ConcurrentHashMap

class IntentTrackerServiceImpl: IIntentTrackerService.Stub() {
    private data class ListenerInfo(
        val listener: IIntentTrackerListener,
        val flags: Int
    )

    private val mListeners = ConcurrentHashMap<IBinder, ListenerInfo>()

    override fun getVersion(): Int = IntentTrackerService.CURRENT_VERSION

    override fun getServiceInfo(): ServiceInfo {
        return ServiceInfo(
            Process.myPid(),
            HookMain.processName
        )
    }

    override fun registerListener(listener: IIntentTrackerListener?, flags: Int) {
        if (listener == null) return
        listener.asBinder().let { binder ->
            mListeners.putIfAbsent(binder, ListenerInfo(listener, flags))
            binder.linkToDeath({ handleListenerDied(binder) }, 0)
        }
    }

    override fun unregisterListener(listener: IIntentTrackerListener?) {
        if (listener == null) return
        mListeners.remove(listener.asBinder())
    }

    private fun handleListenerDied(binder: IBinder) {
        mListeners.remove(binder)
    }

    fun dispatchStartActivity(intent: Intent) {
        logD("dispatchStartActivity $intent")
        val stackTrace = Thread.currentThread().stackTrace.let { s ->
            var start = s.indexOfFirst { it.methodName == "execStartActivity" }
            if (start < 0) start = 0
            s.sliceArray( start until s.size)
        }
        mListeners.forEach { (_, info) ->
            val resultIntent: Intent = if (info.flags and IntentTrackerService.FLAG_GET_ORIGINAL_PARCELABLE != 0)
                intent
            else
                intent.convert()
            val result = if (info.flags and IntentTrackerService.FLAG_GET_STACK_TRACE != 0) {
                StartActivityInfo(resultIntent, stackTrace)
            } else {
                StartActivityInfo(resultIntent, emptyArray())
            }
            info.listener.onStartActivity(result)
        }
    }
}