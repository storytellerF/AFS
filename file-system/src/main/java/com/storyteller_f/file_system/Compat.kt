package com.storyteller_f.file_system

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

fun Int.bit(mask: Int): Boolean = and(mask) != 0

val Throwable.exceptionMessage: String
    get() = localizedMessage ?: message ?: javaClass.simpleName

fun <T : Parcelable> Bundle.getParcelableCompat(key: String, clazz: Class<T>): T? {
    @Suppress("DEPRECATION")
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, clazz)
    } else {
        getParcelable(key) as? T
    }
}
