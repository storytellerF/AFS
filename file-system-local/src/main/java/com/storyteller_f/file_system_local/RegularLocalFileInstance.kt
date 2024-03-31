package com.storyteller_f.file_system_local

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.WorkerThread
import com.storyteller_f.file_system.getExtension
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileCreatePolicy.*
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermission
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import com.storyteller_f.file_system.parentPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files

@Suppress("unused")
class RegularLocalFileInstance(context: Context, uri: Uri) : LocalFileInstance(context, uri) {
    private val innerFile = File(path)

    @Throws(IOException::class)
    override suspend fun createFile(): Boolean {
        return if (innerFile.exists()) {
            true
        } else {
            withContext(Dispatchers.IO) {
                innerFile.createNewFile()
            }
        }
    }

    override suspend fun createDirectory(): Boolean {
        return if (innerFile.exists()) true else innerFile.mkdirs()
    }

    @Throws(Exception::class)
    override suspend fun toChild(name: String, policy: FileCreatePolicy): LocalFileInstance {
        val subFile = File(innerFile, name)
        val uri = getUri(subFile)
        val internalFileInstance = RegularLocalFileInstance(context, uri)
        // 检查目标文件是否存在
        checkChildExistsOtherwiseCreate(subFile, policy)
        return internalFileInstance
    }

    @Suppress("ThrowsCount")
    @Throws(IOException::class)
    private suspend fun checkChildExistsOtherwiseCreate(file: File, policy: FileCreatePolicy) {
        when {
            !exists() -> "当前文件或者文件夹不存在。path:$path"
            fileKind().isFile -> "当前是一个文件，无法向下操作"
            !file.exists() -> when {
                policy !is Create -> "不存在，且不能创建"
                policy.isFile -> {
                    if (!withContext(Dispatchers.IO) {
                            file.createNewFile()
                        }) {
                        "新建文件失败"
                    } else {
                        null // 创建文件成功
                    }
                }

                !file.mkdirs() -> "新建文件失败"
                else -> null // 创建文件夹成功
            }

            else -> null // 文件存在
        }?.let {
            throw IOException(it)
        }
    }

    @Throws(FileNotFoundException::class)
    override suspend fun getFileInputStream(): FileInputStream = withContext(Dispatchers.IO) {
        FileInputStream(innerFile)
    }

    @Throws(FileNotFoundException::class)
    override suspend fun getFileOutputStream(): FileOutputStream = withContext(Dispatchers.IO) {
        FileOutputStream(innerFile)
    }

    override suspend fun filePermissions() =
        FilePermissions(
            FilePermission(
                innerFile.canRead(),
                innerFile.canWrite(),
                innerFile.canExecute()
            )
        )

    override suspend fun fileTime() = innerFile.fileTime()
    override suspend fun fileKind() =
        innerFile.fileKind1()

    private fun File.fileKind1() = FileKind.build(
        isFile,
        isSymbolicLink(),
        isHidden,
        length(),
        getExtension(name).orEmpty()
    )

    @WorkerThread
    public override suspend fun listInternal(
        fileSystemPack: FileSystemPack
    ) {
        val listFiles = innerFile.listFiles() // 获取子文件
        if (listFiles != null) {
            for (childFile in listFiles) {
                val childUri = childUri(childFile.name)
                val permissions = childFile.permissions()
                val fileTime = childFile.fileTime()
                val kind = childFile.fileKind1()
                val name = childFile.name

                val info = FileInfo(
                    name,
                    childUri,
                    fileTime,
                    kind,
                    permissions
                )
                if (childFile.isDirectory) {
                    fileSystemPack.addDirectory(info)
                } else {
                    fileSystemPack.addFile(info)
                }
            }
        }
    }

    override suspend fun exists(): Boolean {
        return innerFile.exists()
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        return innerFile.delete()
    }

    override suspend fun rename(newName: String): FileInstance? {
        val file = File(parentPath(path), newName)
        val renameResult = innerFile.renameTo(file)
        return if (renameResult) {
            RegularLocalFileInstance(context, getUri(file))
        } else {
            null
        }
    }

    override suspend fun toParent(): LocalFileInstance {
        return RegularLocalFileInstance(context, getUri(innerFile.parentFile!!))
    }

    companion object {
        private const val TAG = "ExternalFileInstance"
        private fun getUri(subFile: File): Uri {
            return Uri.Builder().scheme("file").path(subFile.path).build()
        }
    }
}

private fun File.isSymbolicLink() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    Files.isSymbolicLink(toPath())
} else {
    try {
        absolutePath == canonicalPath
    } catch (_: IOException) {
        false
    }
}
