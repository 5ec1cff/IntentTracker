package io.github.a13e300.intenttracker.service

import android.content.ComponentName
import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ActivityStartedInfo(
    val intent: Intent,
    val component: ComponentName,
    val referrer: String,
    val stackTraceElements: Array<StackTraceElement>
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActivityStartedInfo

        if (intent != other.intent) return false
        if (component != other.component) return false
        return stackTraceElements.contentEquals(other.stackTraceElements)
    }

    override fun hashCode(): Int {
        var result = intent.hashCode()
        result = 31 * result + component.hashCode()
        result = 31 * result + stackTraceElements.contentHashCode()
        return result
    }
}
