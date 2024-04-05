package com.storyteller_f.file_system_archive

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.FileSystemPrefix
import com.storyteller_f.file_system.encodeByBase64
import com.storyteller_f.file_system.instance.FileInstance

data object ArchiveFileSystemPrefix : FileSystemPrefix

class ArchiveFileInstanceFactory : FileInstanceFactory {
    override val schemes: List<String>
        get() = listOf(ArchiveFileInstance.SCHEME)

    override suspend fun buildInstance(context: Context, uri: Uri): FileInstance? {
        return if (schemes.contains(uri.scheme)) {
            ArchiveFileInstance(context, uri)
        } else {
            null
        }
    }

    override fun getPrefix(context: Context, uri: Uri): FileSystemPrefix? {
        return if (schemes.contains(uri.scheme)) {
            ArchiveFileSystemPrefix
        } else {
            null
        }
    }

    override fun buildNestedFile(context: Context, name: String?, fileInstance: FileInstance): Uri? {
        return if (fileInstance.extension == "zip") {
            val uri = fileInstance.uri
            Companion.buildNestedFile(uri, name)
        } else {
            null
        }
    }

    companion object {
        fun buildNestedFile(uri: Uri, name: String?): Uri? {
            val authority = uri.toString().encodeByBase64()
            return Uri.Builder()
                .scheme(ArchiveFileInstance.SCHEME)
                .authority(authority)
                .path(name ?: "/")
                .build()
        }
    }
}
