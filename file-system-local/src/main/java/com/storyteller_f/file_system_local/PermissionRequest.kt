package com.storyteller_f.file_system_local

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.storyteller_f.file_system.getFileSystemPrefix
import com.storyteller_f.file_system_local.MainActivity.Companion.putBundle
import kotlinx.coroutines.CompletableDeferred

/**
 * @return 返回是否含有权限。对于没有权限的，调用 requestPermissionForSpecialPath
 */
suspend fun Context.checkFilePermission(uri: Uri): Boolean {
    if (uri.scheme != ContentResolver.SCHEME_FILE) return true
    return when (val prefix = getFileSystemPrefix(this, uri)) {
        is LocalFileSystemPrefix.SelfEmulated -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> DocumentLocalFileInstance.getEmulated(
                this,
                uri,
                prefix.key
            ).exists()

            else -> ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        is LocalFileSystemPrefix.Mounted -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> DocumentLocalFileInstance.getMounted(
                this,
                uri,
                prefix.key
            ).exists()

            else -> ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        else -> true
    }
}

suspend fun Context.requestFilePermission(uri: Uri): Boolean {
    if (uri.scheme != ContentResolver.SCHEME_FILE) return true
    val path = uri.path!!
    val task = CompletableDeferred<Boolean>()
    when {
        path.startsWith(LocalFileSystem.USER_EMULATED_FRONT_PATH) -> {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    requestManageExternalPermission(task)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    requestSAFPermission(uri, MainActivity.REQUEST_SAF_EMULATED, task)
                else -> requestWriteExternalStorage(task)
            }
        }
        path.startsWith(LocalFileSystem.STORAGE_PATH) -> {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    requestManageExternalPermission(task)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ->
                    requestSAFPermission(uri, MainActivity.REQUEST_SAF_SDCARD, task)
                else -> requestWriteExternalStorage(task)
            }
        }
    }
    val result = task.await()
    MainActivity.unbindWaitResult()
    return result
}

private suspend fun Context.requestWriteExternalStorage(task: CompletableDeferred<Boolean>) {
    if (yesOrNo("权限不足", "查看文件夹系统必须授予权限", "授予", "取消")) {
        MainActivity.bindWaitResult(task)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putBundle(MainActivity.REQUEST_EMULATED, Uri.EMPTY)
            }
        )
    } else {
        task.complete(false)
    }
}

private suspend fun Context.requestSAFPermission(
    uri: Uri,
    requestCodeSAF: String,
    task: CompletableDeferred<Boolean>
) {
    if (yesOrNo("需要授予权限", "在Android 10 或者访问存储卡，需要获取SAF权限", "去授予", "取消")) {
        MainActivity.task = task
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putBundle(requestCodeSAF, uri)
            }
        )
    } else {
        task.complete(false)
    }
}

@RequiresApi(api = Build.VERSION_CODES.R)
private suspend fun Context.requestManageExternalPermission(task: CompletableDeferred<Boolean>) {
    if (yesOrNo("需要授予权限", "在Android 11上，需要获取Manage External Storage权限", "去授予", "取消")) {
        MainActivity.task = task
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putBundle(MainActivity.REQUEST_MANAGE, Uri.EMPTY)
            }
        )
    } else {
        task.complete(false)
    }
}

private suspend fun Context.yesOrNo(
    title: String,
    message: String,
    yesString: String,
    noString: String
): Boolean {
    val deferred = CompletableDeferred<Boolean>()
    AlertDialog.Builder(this).setTitle(title)
        .setMessage(message)
        .setPositiveButton(yesString) { _: DialogInterface?, _: Int ->
            deferred.complete(true)
        }
        .setNegativeButton(noString) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            deferred.complete(false)
        }.show()
    println("wait dialog")
    return deferred.await()
}
