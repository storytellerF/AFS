package com.storyteller_f.file_system_archive

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory2
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileSystemPack
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class TestArchiveRootFileInstanceFactory : FileInstanceFactory2 {
    override suspend fun buildInstance(uri: Uri): FileInstance? {
        return if (uri.scheme == SCHEME) {
            TestArchiveRootFileInstance(uri)
        } else {
            null
        }
    }

    companion object {
        const val SCHEME = "archive-test"
    }
}

class TestArchiveRootFileInstance(uri: Uri) : FileInstance(uri) {
    private val file: File
        get() = File(uri.path!!)

    override suspend fun filePermissions() = FilePermissions.USER_READABLE

    override suspend fun fileTime() = FileTime(lastModified = file.lastModified())

    override suspend fun fileKind() = FileKind.File(null, file.isHidden, file.length(), extension)

    override suspend fun getFileInputStream(): FileInputStream {
        return file.inputStream()
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        return file.outputStream()
    }

    override suspend fun listInternal(fileSystemPack: FileSystemPack) = Unit

    override suspend fun exists() = file.exists()

    override suspend fun createFile() = file.createNewFile()

    override suspend fun createDirectory() = file.mkdirs()

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance? = null

    override suspend fun toParent() = this

    override suspend fun deleteFileOrEmptyDirectory() = file.delete()

    override suspend fun rename(newName: String): FileInstance? = null
}
