package com.storyteller_f.file_system.operate

import android.content.Context
import com.storyteller_f.file_system.instance.FileCreatePolicy.Create
import com.storyteller_f.file_system.instance.FileCreatePolicy.NotCreate
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.message.Message
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.size
import com.storyteller_f.file_system.toChildEfficiently
import com.storyteller_f.slim_ktx.exceptionMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

interface SuspendCallable<T> {
    suspend fun call(): T
}

/**
 * 返回应该是任务是否成功
 */
abstract class AbstractFileOperation(
    val fileInstance: FileInstance,
    val context: Context
) : SuspendCallable<Boolean>, FileOperationListener {
    var fileOperationListener: FileOperationListener? = null

    override fun onError(message: Message?) {
        fileOperationListener?.onError(message)
    }

    override fun onDirectoryDone(fileInstance: FileInstance?, message: Message?) {
        fileOperationListener?.onDirectoryDone(fileInstance, message)
    }

    override fun onFileDone(fileInstance: FileInstance?, message: Message?, size: Long) {
        fileOperationListener?.onFileDone(fileInstance, message, size)
    }

    fun bind(fileOperationListener: FileOperationListener): AbstractFileOperation {
        this.fileOperationListener = fileOperationListener
        return this
    }
}

/**
 * 有目标地址
 * @param target 必须是一个目录
 */
abstract class ScopeFileOperation(
    fileInstance: FileInstance,
    val target: FileInstance,
    context: Context
) : AbstractFileOperation(fileInstance, context)

/**
 * 没有目标地址
 */
abstract class BlindFileOperation(
    fileInstance: FileInstance,
    context: Context
) : AbstractFileOperation(fileInstance, context)

open class ScopeFileCopyOp(
    fileInstance: FileInstance,
    target: FileInstance,
    context: Context
) : ScopeFileOperation(fileInstance, target, context) {
    override suspend fun call(): Boolean {
        return if (fileInstance.fileKind().isFile) {
            // 新建一个文件
            copyFileFaster(fileInstance, target)
        } else {
            copyDirectoryFaster(
                fileInstance,
                target.toChildEfficiently(
                    context,
                    fileInstance.name,
                    Create(false)
                )
            )
        }
    }

    private suspend fun copyDirectoryFaster(f: FileInstance, t: FileInstance): Boolean {
        val listSafe = f.list()
        listSafe.files.forEach {
            yield()
            copyFileFaster(f.toChildEfficiently(context, it.name, Create(true)), t)
        }
        listSafe.directories.forEach {
            yield()
            copyDirectoryFaster(
                f.toChildEfficiently(context, it.name, Create(false)),
                t.toChildEfficiently(
                    context,
                    it.name,
                    Create(false)
                )
            )
        }
        notifyDirectoryDone(fileInstance, Message("${f.name} success"), 0)
        return true
    }

    open suspend fun notifyDirectoryDone(fileInstance: FileInstance, message: Message, i: Int) {
        onDirectoryDone(fileInstance, message)
    }

    open suspend fun notifyFileDone(f: FileInstance, message: Message, fileLength: Long, i: Int) {
        onFileDone(fileInstance, message, fileLength)
    }

    private suspend fun copyFileFaster(f: FileInstance, t: FileInstance): Boolean {
        try {
            val toChild = t.toChildEfficiently(context, f.name, Create(true))
            f.getFileInputStream().channel.use { int ->
                (toChild).getFileOutputStream().channel.use { out ->
                    copyFileInternal(int, out, f)
                    return true
                }
            }
        } catch (e: Exception) {
            onError(Message(e.message ?: "error"))
        }
        return false
    }

    private suspend fun copyFileInternal(
        int: FileChannel,
        out: FileChannel,
        f: FileInstance
    ) {
        withContext(Dispatchers.IO) {
            val byteBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
            while (int.read(byteBuffer) != -1) {
                yield()
                byteBuffer.flip()
                out.write(byteBuffer)
                byteBuffer.clear()
            }
            notifyFileDone(f, Message(""), f.size(), 0)
        }
    }
}

class ScopeFileMoveOp(
    fileInstance: FileInstance,
    target: FileInstance,
    context: Context
) : ScopeFileCopyOp(fileInstance, target, context), FileOperationListener {

    override suspend fun notifyFileDone(
        f: FileInstance,
        message: Message,
        fileLength: Long,
        i: Int
    ) {
        super.notifyFileDone(f, message, fileLength, i)
        try {
            fileInstance.deleteFileOrEmptyDirectory()
        } catch (e: Exception) {
            fileOperationListener?.onError(
                Message("delete ${fileInstance.name} failed ${e.localizedMessage}")
            )
        }
    }

    override suspend fun notifyDirectoryDone(
        fileInstance: FileInstance,
        message: Message,
        i: Int
    ) {
        super.notifyDirectoryDone(fileInstance, message, i)
        try {
            fileInstance.deleteFileOrEmptyDirectory()
        } catch (e: Exception) {
            fileOperationListener?.onError(
                Message("delete ${fileInstance.name} failed ${e.exceptionMessage}")
            )
        }
    }
}

class ScopeFileMoveOpInShell(
    fileInstance: FileInstance,
    target: FileInstance,
    context: Context
) : ScopeFileOperation(fileInstance, target, context) {
    override suspend fun call(): Boolean {
        val mvResult = withContext(Dispatchers.IO) {
            Runtime.getRuntime().exec("mv ${fileInstance.path} ${target.path}/").waitFor()
        }

        val cmdFailed = mvResult != 0
        when {
            cmdFailed -> onError(Message("process return $mvResult"))
            target.fileKind().isFile -> onFileDone(target, Message("success"), target.size())
            else -> onDirectoryDone(fileInstance, Message("success"))
        }
        return !cmdFailed
    }
}

class FileDeleteOp(
    fileInstance: FileInstance,
    context: Context
) : BlindFileOperation(fileInstance, context) {

    override suspend fun call(): Boolean {
        return deleteDirectory(fileInstance)
    }

    /**
     * @return 是否成功
     */
    private suspend fun deleteDirectory(fileInstance: FileInstance): Boolean {
        try {
            val listSafe = fileInstance.list()
            if (listSafe.files.any {
                    !deleteChildFile(fileInstance, it)
                }
            ) {
                // 如果失败提前结束
                return false
            }
            if (listSafe.directories.any {
                    !deleteChildDirectory(fileInstance, it)
                }
            ) {
                // 如果失败提前结束
                return false
            }
            // 删除当前空文件夹
            val deleteCurrentDirectory = fileInstance.deleteFileOrEmptyDirectory()
            if (deleteCurrentDirectory) {
                onDirectoryDone(
                    fileInstance,
                    Message("delete ${fileInstance.name} success")
                )
            } else {
                onError(Message("delete ${fileInstance.name} failed"))
            }
            return deleteCurrentDirectory
        } catch (_: Exception) {
            return false
        }
    }

    private suspend fun deleteChildDirectory(
        fileInstance: FileInstance,
        it: FileInfo
    ): Boolean {
        val childDirectory = fileInstance.toChildEfficiently(
            context,
            it.name,
            NotCreate
        )
        val deleteDirectory = deleteDirectory(childDirectory)
        if (deleteDirectory) {
            onDirectoryDone(
                childDirectory,
                Message("delete ${it.name} success")
            )
        } else {
            onError(Message("delete ${it.name} failed"))
        }
        return deleteDirectory
    }

    private suspend fun deleteChildFile(fileInstance: FileInstance, it: FileInfo): Boolean {
        val childFile = fileInstance.toChildEfficiently(context, it.name, NotCreate)
        val deleteFileOrEmptyDirectory = childFile.deleteFileOrEmptyDirectory()
        if (deleteFileOrEmptyDirectory) {
            onFileDone(
                childFile,
                Message("delete ${it.name} success"),
                fileInstance.size()
            )
        } else {
            onError(Message("delete ${it.name} failed"))
        }
        return deleteFileOrEmptyDirectory
    }
}
