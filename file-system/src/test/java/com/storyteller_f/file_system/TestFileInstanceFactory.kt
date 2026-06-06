package com.storyteller_f.file_system

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileSystemPack
import java.io.FileInputStream
import java.io.FileOutputStream

class TestFileInstanceFactory : FileInstanceFactory {
    override suspend fun buildInstance(context: Context, uri: Uri): FileInstance? {
        return if (uri.scheme == SCHEME || uri.scheme == NESTED_SCHEME) {
            instances.getOrPut(uri.toString()) { TestServiceFileInstance(uri) }
        } else {
            null
        }
    }

    override fun getPrefix(context: Context, uri: Uri): FileSystemPrefix? {
        return if (uri.scheme == SCHEME || uri.scheme == NESTED_SCHEME) {
            TestPrefix(uri.authority.orEmpty())
        } else {
            null
        }
    }

    override fun buildNestedFile(context: Context, name: String?, fileInstance: FileInstance): Uri? {
        return if (fileInstance.extension == "zip") {
            Uri.Builder()
                .scheme(NESTED_SCHEME)
                .authority(fileInstance.uri.toString().encodeByBase64())
                .path(name ?: "/")
                .build()
        } else {
            null
        }
    }

    data class TestPrefix(val key: String) : FileSystemPrefix

    companion object {
        const val SCHEME = "test"
        const val NESTED_SCHEME = "test-nested"
        val instances = mutableMapOf<String, TestServiceFileInstance>()

        fun reset() {
            instances.clear()
        }
    }
}

class TestServiceFileInstance(uri: Uri) : FileInstance(uri) {
    var exists = false
    var directory = true

    override suspend fun filePermissions() = FilePermissions.USER_READABLE

    override suspend fun fileTime() = FileTime()

    override suspend fun fileKind(): FileKind {
        return if (directory) {
            FileKind.Directory(null, false)
        } else {
            FileKind.File(null, false, 0, extension)
        }
    }

    override suspend fun getFileInputStream(): FileInputStream {
        error("Not needed for service-loader tests")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        error("Not needed for service-loader tests")
    }

    override suspend fun listInternal(fileSystemPack: FileSystemPack) = Unit

    override suspend fun exists() = exists

    override suspend fun createFile(): Boolean {
        exists = true
        directory = false
        return true
    }

    override suspend fun createDirectory(): Boolean {
        exists = true
        directory = true
        return true
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance? {
        val child = TestServiceFileInstance(childUri(name))
        when (policy) {
            is FileCreatePolicy.Create -> if (policy.isFile) child.createFile() else child.createDirectory()
            FileCreatePolicy.NotCreate -> Unit
        }
        TestFileInstanceFactory.instances[child.uri.toString()] = child
        return child
    }

    override suspend fun toParent(): FileInstance {
        return TestServiceFileInstance(parentUri()).apply {
            exists = true
            directory = true
        }
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        exists = false
        return true
    }

    override suspend fun rename(newName: String): FileInstance? {
        return TestServiceFileInstance(overridePath("${parent!!}/$newName")).apply {
            exists = this@TestServiceFileInstance.exists
            directory = this@TestServiceFileInstance.directory
        }
    }
}
