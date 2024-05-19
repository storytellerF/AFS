package com.storyteller_f.file_system_local

import android.content.Context
import android.os.Build
import android.os.Process
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.storyteller_f.file_system.buildPath
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind

fun Context.getMyId() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    ContextCompat.getSystemService(this, UserManager::class.java)!!
        .getSerialNumberForUser(Process.myUserHandle())
} else {
    0L
}

@Suppress("unused")
fun Context.getCurrentUserEmulatedPath() =
    buildPath(
        LocalFileSystemPaths.EMULATED_ROOT_PATH,
        getMyId().toString()
    )

fun Context.getCurrentUserDataPath() =
    buildPath(
        LocalFileSystemPaths.USER_DATA,
        getMyId().toString()
    )

/**
 * /storage/XX44-XX55 或者是/storage/XX44-XX55/test。最终结果应该是/storage/XX44-XX55
 */
fun extractSdPath(path: String): String {
    var endIndex = path.indexOf("/", LocalFileSystemPaths.STORAGE_PATH.length + 1)
    if (endIndex == -1) endIndex = path.length
    return path.substring(0, endIndex)
}

fun Context.appDataDir() = "${LocalFileSystemPaths.DATA_SUB_DATA}/$packageName"

suspend fun FileInstance.getDirectorySize(): Long {
    var size: Long = 0
    val pack = list()
    pack.files.forEach {
        size += (it.kind as FileKind.File).size
    }
    pack.directories.forEach {
        size += (toChild(it.name, FileCreatePolicy.NotCreate)?.getDirectorySize() ?: 0)
    }
    return size
}
