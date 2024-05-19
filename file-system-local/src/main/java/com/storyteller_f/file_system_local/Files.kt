package com.storyteller_f.file_system_local

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system_local.instance.DocumentLocalFileInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

fun File.permissions(): FilePermissions {
    val w = canWrite()
    val e = canExecute()
    val r = canRead()
    return FilePermissions.permissions(r, w, e)
}

fun DocumentFile.permissions(): FilePermissions {
    val w = canWrite()
    val r = canRead()
    return FilePermissions.permissions(r, w, false)
}

@RequiresApi(api = Build.VERSION_CODES.N)
fun Context.getStorageVolume(): List<StorageVolume> {
    val storageManager = getSystemService(StorageManager::class.java)
    return storageManager.storageVolumes
}

fun Context.getStorageCompat(): List<File> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        getStorageVolume().map { storageVolume: StorageVolume ->
            val uuid = storageVolume.uuid
            File(
                LocalFileSystemPaths.STORAGE_PATH,
                volumePathName(uuid)
            )
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val externalFilesDirs = externalCacheDirs
        externalFilesDirs.map {
            val absolutePath = it.absolutePath
            val endIndex = absolutePath.indexOf("Android")
            val path = absolutePath.substring(0, endIndex)
            File(path)
        }
    } else {
        val file = File("/storage/")
        file.listFiles()?.toList() ?: return listOf()
    }
}

fun volumePathName(uuid: String?): String = uuid ?: "emulated"

@Suppress("DEPRECATION")
fun getSpace(prefix: String?): Long {
    val stat = StatFs(prefix)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        stat.availableBytes
    } else {
        stat.blockSize.toLong() * stat.availableBlocks.toLong()
    }
}

@Suppress("DEPRECATION")
fun getFree(prefix: String?): Long {
    val stat = StatFs(prefix)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        stat.freeBytes
    } else {
        stat.blockSize.toLong() * stat.freeBlocks.toLong()
    }
}

@Suppress("DEPRECATION")
fun getTotal(prefix: String?): Long {
    val stat = StatFs(prefix)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        stat.totalBytes
    } else {
        stat.blockSize.toLong() * stat.blockCount.toLong()
    }
}

suspend fun File.fileTime(): FileTime {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val basicFileAttributes =
                withContext(Dispatchers.IO) {
                    Files.readAttributes(toPath(), BasicFileAttributes::class.java)
                }
            val createdTime = basicFileAttributes.creationTime().toMillis()
            val lastAccessTime = basicFileAttributes.lastAccessTime().toMillis()
            return FileTime(lastModified(), lastAccessTime, createdTime)
        } catch (_: IOException) {
        }
    }
    return FileTime(lastModified())
}

fun Activity.generateSAFRequestIntent(prefix: LocalFileSystemPrefix): Intent? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val sm = getSystemService(StorageManager::class.java)
        val volume = sm.getStorageVolume(File(prefix.key))
        if (volume != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return volume.createOpenDocumentTreeIntent()
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (prefix is LocalFileSystemPrefix.SelfEmulated) {
                val primary = DocumentsContract.buildRootUri(
                    DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS,
                    DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS_TREE
                )
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, primary)
            } else if (prefix is LocalFileSystemPrefix.Mounted) {
                val tree = DocumentLocalFileInstance.getMountedTree(prefix.key)
                val primary = DocumentsContract.buildRootUri(
                    DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS,
                    tree
                )
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, primary)
            }
        }
        return intent
    }
    return null
}
