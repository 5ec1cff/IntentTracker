package io.github.a13e300.intenttracker.service

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BundleValueInfo(
    val string: String,
    val className: String,
    val classLoader: String
): Parcelable
