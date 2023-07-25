package io.github.a13e300.intenttracker.service

import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StartActivityInfo(
    val intent: Intent,
    val stackTraceElements: Array<StackTraceElement>
): Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StartActivityInfo

        if (intent != other.intent) return false
        if (!stackTraceElements.contentEquals(other.stackTraceElements)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intent.hashCode()
        result = 31 * result + stackTraceElements.contentHashCode()
        return result
    }
}
