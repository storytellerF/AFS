package com.storyteller_f.file_system_memory

import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory2
import com.storyteller_f.file_system.getExtension
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.isHidden
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.isWritable
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes

abstract class PathsFileInstance(uri: Uri) : FileInstance(uri) {
    abstract val nioPath: Path
    override suspend fun filePermissions(): FilePermissions {
        return nioPath.filePermissions1()
    }

    private fun Path.filePermissions1(): FilePermissions {
        val readable = isReadable()
        val writable = isWritable()
        val executable = isExecutable()
        return FilePermissions.permissions(readable, writable, executable)
    }

    override suspend fun fileTime(): FileTime {
        return nioPath.fileTime1()
    }

    private fun Path.fileTime1(): FileTime {
        val fileAttributes = readAttributes<BasicFileAttributes>()
        val lastModifiedTime = fileAttributes.lastModifiedTime().toMillis()
        val lastAccessTime = fileAttributes.lastAccessTime().toMillis()
        val creationTime = fileAttributes.creationTime().toMillis()
        return FileTime(lastModifiedTime, lastAccessTime, creationTime)
    }

    override suspend fun fileKind(): FileKind {
        return nioPath.fileKind1()
    }

    private fun Path.fileKind1() = FileKind.build(
        isRegularFile(),
        isSymbolicLink(),
        isHidden(),
        fileSize(),
        getExtension(name).orEmpty()
    )

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getInputStream(): InputStream {
        return nioPath.inputStream()
    }

    override suspend fun getOutputStream(): OutputStream {
        return nioPath.outputStream()
    }

    override suspend fun listInternal(fileSystemPack: FileSystemPack) {
        withContext(Dispatchers.IO) {
            Files.list(nioPath)
        }.forEach {
            val name = it.name
            val childUri = childUri(name)
            val time = it.fileTime1()
            val kind = it.fileKind1()
            val permissions = it.filePermissions1()
            val info = FileInfo(name, childUri, time, kind, permissions)
            if (it.isRegularFile()) {
                fileSystemPack.addFile(info)
            } else if (it.isDirectory()) {
                fileSystemPack.addDirectory(info)
            }
        }
    }

    override suspend fun exists(): Boolean {
        return nioPath.exists()
    }

    override suspend fun createFile(): Boolean {
        return runCatching {
            nioPath.createFile()
        }.isSuccess
    }

    override suspend fun createDirectory(): Boolean {
        return runCatching {
            nioPath.createDirectory()
        }.isSuccess
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance? {
        return MemoryFileInstance(childUri(name))
    }

    override suspend fun toParent(): FileInstance {
        return MemoryFileInstance(uri.buildUpon().path(nioPath.parent.pathString).build())
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        return runCatching {
            nioPath.deleteExisting()
        }.isSuccess
    }

    override suspend fun rename(newName: String): FileInstance? {
        val newPath = nioPath.parent.resolve(newName)
        nioPath.moveTo(newPath)
        return MemoryFileInstance(overridePath(newPath.pathString))
    }
}

class MemoryFileInstance(uri: Uri) : PathsFileInstance(uri) {

    companion object {
        const val SCHEME = "memory"
        val memoryFileSystems = mutableMapOf<String, FileSystem>()
    }

    override val nioPath: Path
        get() = memoryFileSystems[uri.authority]!!.getPath(path)
}

class MemoryFileInstanceFactory : FileInstanceFactory2 {
    override suspend fun buildInstance(uri: Uri): FileInstance? {
        return if (uri.scheme == MemoryFileInstance.SCHEME) {
            MemoryFileInstance(uri)
        } else {
            null
        }
    }
}
