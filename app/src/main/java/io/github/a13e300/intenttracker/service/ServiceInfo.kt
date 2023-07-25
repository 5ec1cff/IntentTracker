package io.github.a13e300.intenttracker.service

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServiceInfo(
    val pid: Int,
    val processName: String
): Parcelable