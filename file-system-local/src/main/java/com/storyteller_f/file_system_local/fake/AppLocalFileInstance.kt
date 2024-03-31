package com.storyteller_f.file_system_local.fake

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.instance.BaseContextFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileSystemPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 标识一个apk 文件
 */
class AppLocalFileInstance(context: Context, uri: Uri) : BaseContextFileInstance(context, uri) {
    override suspend fun filePermissions() = FilePermissions.USER_READABLE
    override suspend fun fileTime(): FileTime {
        TODO("Not yet implemented")
    }

    override suspend fun fileKind() = FileKind.build(
        isFile = false,
        isSymbolicLink = false,
        isHidden = false,
        File(publicSourceDir).length(),
        "apk"
    )

    private val publicSourceDir: String =
        context.packageManager.getApplicationInfoCompat(name, 0).publicSourceDir

    override suspend fun getFileInputStream() =
        withContext(Dispatchers.IO) {
            FileInputStream(publicSourceDir)
        }

    override suspend fun getFileOutputStream() =
        withContext(Dispatchers.IO) {
            FileOutputStream(publicSourceDir)
        }

    override suspend fun listInternal(
        fileSystemPack: FileSystemPack
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun exists() = true

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }
}

fun PackageManager.getApplicationInfoCompat(packageName: String, flag: Long): ApplicationInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flag))
    } else {
        getApplicationInfo(packageName, flag.toInt())
    }
}

@SuppressLint("QueryPermissionsNeeded")
fun PackageManager.getInstalledApplicationsCompat(flag: Long): MutableList<ApplicationInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flag))
    } else {
        getInstalledApplications(flag.toInt())
    }
}
