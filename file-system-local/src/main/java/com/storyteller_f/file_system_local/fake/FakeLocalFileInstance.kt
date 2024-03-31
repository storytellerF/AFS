package com.storyteller_f.file_system_local.fake

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import com.storyteller_f.file_system_local.LocalFileSystem
import com.storyteller_f.file_system_local.fileTime
import com.storyteller_f.file_system_local.getMyId
import com.storyteller_f.file_system_local.getStorageCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 预定义的用于无法访问的中间目录，不能标识一个文件类型
 */
class FakeLocalFileInstance(val context: Context, uri: Uri) :
    ForbidChangeLocalFileInstance(uri) {
    private val myId = context.getMyId()

    @SuppressLint("SdCardPath")
    private val presetDirectories: MutableMap<String, List<String>> = mutableMapOf(
        "/data/user/$myId" to listOf(context.packageName),
        "/data/data" to listOf(context.packageName),
        LocalFileSystem.EMULATED_ROOT_PATH to listOf(myId.toString()),
        "/data/user" to listOf(myId.toString()),
    )

    private val presetFiles: MutableMap<String, List<String>> = mutableMapOf(
        "/data/app" to context.packageManager.getInstalledApplicationsCompat(0).mapNotNull {
            it.packageName
        }
    )

    override suspend fun getFileInputStream(): FileInputStream = TODO("Not yet implemented")

    override suspend fun getFileOutputStream(): FileOutputStream = TODO("Not yet implemented")

    override suspend fun filePermissions() = FilePermissions.USER_READABLE
    override suspend fun fileTime(): FileTime {
        TODO("Not yet implemented")
    }

    override suspend fun fileKind() = FileKind.build(
        isFile = false,
        isSymbolicLink = false,
        isHidden = false,
        0,
        extension
    )

    @WorkerThread
    override suspend fun listInternal(
        fileSystemPack: FileSystemPack
    ) {
        presetFiles[path]?.map { packageName ->
            val (file, child) = child(packageName)
            val length = getAppSize(packageName)
            FileInfo(
                packageName,
                child,
                file.fileTime(),
                FileKind.build(isFile = true, isSymbolicLink = false, isHidden = false, length, ""),
                FilePermissions.USER_READABLE,
            )
        }?.let(fileSystemPack::addFiles)

        (presetSystemDirectories[path] ?: presetDirectories[path])?.map {
            val (file, child) = child(it)
            FileInfo(
                it,
                child,
                file.fileTime(),
                FileKind.build(
                    isFile = false,
                    isSymbolicLink = symLink.contains(it),
                    false,
                    0,
                    ""
                ),
                FilePermissions.USER_READABLE
            )
        }?.let(fileSystemPack::addDirectories)

        if (path == LocalFileSystem.STORAGE_PATH) {
            storageVolumes().let(fileSystemPack::addDirectories)
        }
    }

    private fun getAppSize(packageName: String): Long {
        return File(
            context.packageManager.getApplicationInfoCompat(
                packageName,
                0
            ).publicSourceDir
        ).length()
    }

    private suspend fun storageVolumes(): List<FileInfo> {
        return context.getStorageCompat().map {
            val (file, child) = child(it.name)
            FileInfo(
                it.name,
                child,
                file.fileTime(),
                FileKind.build(isFile = false, isSymbolicLink = false, isHidden = false, 0, ""),
                FilePermissions.USER_READABLE
            )
        }
    }

    override suspend fun exists() = true

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy) =
        FakeLocalFileInstance(context, childUri(name))

    companion object {
        val presetSystemDirectories = mapOf(
            "/" to listOf("sdcard", "storage", "data", "mnt", "system"),
            "/data" to listOf("user", "data", "app", "local"),
            "/storage" to listOf("self"),
            "/storage/self" to listOf("primary")
        )

        val symLink = listOf("bin", "sdcard", "etc")
    }
}
