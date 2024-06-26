package com.storyteller_f.file_system_archive

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.storyteller_f.file_system.decodeByBase64
import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system.instance.BaseContextFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * 比如uri 的路径是/test.zip/hello.txt，此时/test.zip 必须是一个文件，但是如果不是一个压缩文件，会出现错误
 * 压缩文件中可能还有一个压缩文件，所以需要指定archiveFileUri 指向最近的压缩文件，比如它的路径就是/test.zip
 */
class ArchiveFileInstance(context: Context, uri: Uri) :
    BaseContextFileInstance(context, uri) {

    private val archiveFileUri get() = uri.authority!!.decodeByBase64().toUri()

    private var rootFileInstance: FileInstance? = null

    private suspend fun reconnectIfNeed(): FileInstance {
        val root = rootFileInstance
        return if (root == null) {
            val new = getFileInstance(context, archiveFileUri)!!
            rootFileInstance = new
            new
        } else {
            root
        }
    }

    override suspend fun filePermissions(): FilePermissions {
        return FilePermissions.USER_READABLE
    }

    override suspend fun fileTime(): FileTime {
        return readCurrentFileData {
            it.fileTime()
        }!!
    }

    override suspend fun fileKind(): FileKind {
        return readCurrentFileData {
            it.fileKind()
        }!!
    }

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    class ZipWrapInputStream(private val zipInputStream: ZipInputStream) : InputStream() {
        override fun read(): Int {
            return zipInputStream.read()
        }
    }

    override suspend fun getInputStream(): InputStream {
        return readCurrentFileData(false) {
            ZipWrapInputStream(this)
        }!!
    }

    override suspend fun getOutputStream(): OutputStream {
        return super.getOutputStream()
    }

    /**
     * @param filter 第二个参数是entryName，以/ 开头，不会以/ 结束
     */
    private suspend fun readEntry(
        closeStream: Boolean = true,
        filter: ZipInputStream.(ZipEntry, entryName: String) -> Boolean
    ) {
        reconnectIfNeed().let {
            val zipInputStream = ZipInputStream(it.getInputStream())
            try {
                process(zipInputStream, filter)
            } finally {
                if (closeStream) {
                    zipInputStream.close()
                }
            }
        }
    }

    private fun process(
        zipInputStream: ZipInputStream,
        filter: ZipInputStream.(ZipEntry, String) -> Boolean
    ) {
        while (true) {
            zipInputStream.nextEntry?.let { zipEntry ->
                val name1 = zipEntry.name.removeSuffix("/")
                val p = "/$name1"
                if (p.startsWith(path) && zipInputStream.filter(zipEntry, p)) {
                    return
                }
                zipInputStream.closeEntry() // filter 中可能并没有读取，手动关闭
            } ?: break
        }
    }

    private suspend fun <R> readEntryData(
        closeStream: Boolean = true,
        block: ZipInputStream.(ZipEntry, String) -> R?
    ): R? {
        var result: R? = null
        readEntry(closeStream) { entry, entryName ->
            val block1 = block(entry, entryName)
            if (block1 != null) {
                result = block1
                true
            } else {
                false
            }
        }
        return result
    }

    private suspend fun <R> readCurrentFileData(
        closeStream: Boolean = true,
        block: ZipInputStream.(ZipEntry) -> R?
    ): R? {
        return readEntryData(closeStream) { entry, entryName ->
            if (entryName == path) {
                block(entry)
            } else {
                null
            }
        }
    }

    override suspend fun listInternal(
        fileSystemPack: FileSystemPack
    ) {
        readEntry { zipEntry, entryPath ->
            if (entryPath != path) {
                // entryPath 可能的结果/data /data/text.h /data/test/test.h
                val entryRelativePath =
                    if (path != "/") {
                        entryPath.substringAfter("$path/")
                    } else {
                        entryPath.substringAfter(path)
                    }
                if (entryRelativePath.isNotEmpty() && !entryRelativePath.contains("/")) {
                    val fileTime = zipEntry.fileTime()
                    val childUri = childUri(entryRelativePath)
                    val kind = zipEntry.fileKind()
                    val info = FileInfo(
                        entryRelativePath,
                        childUri,
                        fileTime,
                        kind,
                        FilePermissions.USER_READABLE,
                    )
                    if (zipEntry.isDirectory) {
                        fileSystemPack.addDirectory(info)
                    } else {
                        fileSystemPack.addFile(info)
                    }
                }
            }

            false
        }
    }

    override suspend fun exists(): Boolean {
        return readCurrentFileData {
            true
        } == true
    }

    override suspend fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        val child = childUri(name)
        return ArchiveFileInstance(context, child)
    }

    override suspend fun toParent(): FileInstance {
        return ArchiveFileInstance(context, parentUri())
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun rename(newName: String): FileInstance? {
        TODO("Not yet implemented")
    }

    companion object {
        const val SCHEME = "archive"
    }
}
