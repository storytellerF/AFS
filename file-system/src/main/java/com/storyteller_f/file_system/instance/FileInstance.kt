package com.storyteller_f.file_system.instance

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.util.ObjectsCompat
import com.storyteller_f.file_system.getExtension
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import com.storyteller_f.file_system.parentPath
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

// todo getChannel
// todo file descriptor
abstract class FileInstance(val uri: Uri) {

    /**
     * 相对于根的path，主要用于展示。在不同的文件系统中有不同的实现
     */
    open val path: String = uri.path!!

    /**
     * 获取父路径
     *
     * @return 父路径
     */
    val parent: String?
        get() = parentPath(path)

    val name: String
        get() = File(path).name

    val extension: String
        get() = getExtension(name).orEmpty()

    @WorkerThread
    abstract suspend fun filePermissions(): FilePermissions

    @WorkerThread
    abstract suspend fun fileTime(): FileTime

    @WorkerThread
    abstract suspend fun fileKind(): FileKind

    @WorkerThread
    suspend fun getFileInfo() =
        FileInfo(
            name,
            uri,
            fileTime(),
            fileKind(),
            filePermissions(),
        )

    @WorkerThread
    abstract suspend fun getFileInputStream(): FileInputStream

    @WorkerThread
    abstract suspend fun getFileOutputStream(): FileOutputStream

    @WorkerThread
    open suspend fun getInputStream(): InputStream {
        return getFileInputStream()
    }

    @WorkerThread
    open suspend fun getOutputStream(): OutputStream {
        return getFileOutputStream()
    }

    /**
     * 应该仅用于目录。可能会抛出异常，内部不会处理。
     */
    @WorkerThread
    protected abstract suspend fun listInternal(
        fileSystemPack: FileSystemPack
    )

    @WorkerThread
    suspend fun list(): FileSystemPack {
        val fileSystemPack = FileSystemPack(buildFilesContainer(), buildDirectoryContainer())
        listInternal(fileSystemPack)
        return fileSystemPack
    }

    /**
     * 是否存在
     *
     * @return true代表存在
     */
    @WorkerThread
    abstract suspend fun exists(): Boolean

    @WorkerThread
    abstract suspend fun createFile(): Boolean

    @WorkerThread
    abstract suspend fun createDirectory(): Boolean

    /**
     * 调用者只能是一个路径
     * 如果目标文件或者文件夹不存在，将会自动创建，因为在这种状态下，新建文件速度快，特别是外部存储目录
     * 不应该考虑能否转换成功
     *
     * @param name                名称
     * @return 返回子对象
     */
    @WorkerThread
    abstract suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance?

    /**
     * 移动指针，指向他的父文件夹
     * 不应该考虑能否转换成功
     *
     * @return 返回他的文件夹
     */
    @WorkerThread
    abstract suspend fun toParent(): FileInstance

    /**
     * 删除当前文件
     *
     * @return 返回是否删除成功
     */
    @WorkerThread
    abstract suspend fun deleteFileOrEmptyDirectory(): Boolean

    /**
     * 重命名当前文件
     *
     * @param newName 新的文件名，不包含路径
     * @return 返回新的FileInstance
     */
    @WorkerThread
    abstract suspend fun rename(newName: String): FileInstance?

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as FileInstance
        return ObjectsCompat.equals(uri, that.uri)
    }

    override fun hashCode(): Int {
        return ObjectsCompat.hash(uri)
    }

    private fun buildFilesContainer(): MutableList<FileInfo> = mutableListOf()

    private fun buildDirectoryContainer(): MutableList<FileInfo> = mutableListOf()

    protected fun childUri(name: String): Uri {
        return uri.buildUpon().appendPath(name).build()
    }

    protected fun overridePath(newPath: String): Uri {
        return uri.buildUpon().path(newPath).build()
    }

    protected fun parentUri(): Uri {
        val parentPath = parentPath(path)!!
        return overridePath(parentPath)
    }
}
