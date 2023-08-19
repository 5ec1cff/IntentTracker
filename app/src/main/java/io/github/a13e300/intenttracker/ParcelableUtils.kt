package io.github.a13e300.intenttracker

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import io.github.a13e300.intenttracker.service.BundleValueInfo
import io.github.a13e300.intenttracker.service.IntentTrackerService
import java.io.Serializable

private val bootClassLoader = Bundle::class.java.classLoader

fun Parcelable.convertToBundleInfoOrSelf(): Parcelable =
    if (javaClass.classLoader == bootClassLoader) {
        this
    } else {
        BundleValueInfo(
            toString(),
            javaClass.name,
            javaClass.classLoader?.toString() ?: "null"
        )
    }

fun Bundle.convertToBundleInfo(): Bundle = Bundle().let { result ->
    for (k in keySet()) {
        val v = get(k)
        when (v) {
            is Int -> result.putInt(k, v)
            is Long -> result.putLong(k, v)
            is Boolean -> result.putBoolean(k, v)
            is Short -> result.putShort(k, v)
            is Char -> result.putChar(k, v)
            is Byte -> result.putByte(k, v)
            is IntArray -> result.putIntArray(k, v)
            is LongArray -> result.putLongArray(k, v)
            is BooleanArray -> result.putBooleanArray(k, v)
            is ShortArray -> result.putShortArray(k, v)
            is CharArray -> result.putCharArray(k, v)
            is ByteArray -> result.putByteArray(k, v)
            is Size -> result.putSize(k, v)
            is SizeF -> result.putSizeF(k, v)

            is CharSequence -> result.putCharSequence(k, v)

            is Array<*> -> {
                val a = arrayOfNulls<Any?>(v.size)
                for (i in a.indices) {
                    val item = v[i]
                    if (item is Parcelable) {
                        a[i] = item.convertToBundleInfoOrSelf()
                    } else {
                        a[i] = item
                    }
                }
                result.putParcelableArray(k, a as Array<out Parcelable>)
            }
            is ArrayList<*> -> {
                val list = ArrayList<Any>()
                for (item in v) {
                    if (item is Parcelable) {
                        list.add(item.convertToBundleInfoOrSelf())
                    } else {
                        list.add(item)
                    }
                }
                result.putParcelableArrayList(k, list as ArrayList<out Parcelable>)
            }
            is SparseArray<*> -> {
                val list = SparseArray<Any>()
                for (i in 0 until v.size()) {
                    val key = v.keyAt(i)
                    val value = v.valueAt(i)
                    if (value is Parcelable) {
                        list.put(key, value.convertToBundleInfoOrSelf())
                    } else {
                        list.put(key, value)
                    }
                }
                result.putSparseParcelableArray(k, list as SparseArray<out Parcelable>)
            }

            is Intent -> result.putParcelable(k, v.convert())
            is Bundle -> result.putBundle(k, v.convertToBundleInfo())
            is Parcelable -> {
                result.putParcelable(k, v.convertToBundleInfoOrSelf())
            }
            is Serializable -> {
                if (v.javaClass.classLoader == ClassLoader.getSystemClassLoader()) {
                    result.putSerializable(k, v)
                } else {
                    result.putParcelable(k, BundleValueInfo(
                        v.toString(),
                        v.javaClass.name,
                        v.javaClass.classLoader?.toString() ?: "null"
                    ))
                }
            }
        }
    }
    result
}

fun Intent.convert(): Intent =
    cloneFilter().also { r -> extras?.convertToBundleInfo()?.also { r.putExtras(it) } }

fun Intent.print(level: Int = 0) {
    val prefix = " ".repeat(level)
    println("${prefix}intent:$this")
    extras?.print(level + 1)
}

fun Bundle.print(level: Int = 0) {
    classLoader = IntentTrackerService::class.java.classLoader
    val prefix = " ".repeat(level)
    println("${prefix}bundle:")
    keySet().forEach { k ->
        val v = get(k)
        print("${prefix}$k -> ")
        when (v) {
            is Bundle -> v.print(level + 1)
            is Intent -> v.print(level + 1)
            else -> println(v)
        }
    }
}
