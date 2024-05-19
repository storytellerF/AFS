package com.storyteller_f.file_system_local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.FileSystemPrefix
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.rawTree
import com.storyteller_f.file_system.tree
import com.storyteller_f.file_system_local.instance.DocumentLocalFileInstance

class LocalFileInstanceFactory : FileInstanceFactory {

    override suspend fun buildInstance(context: Context, uri: Uri): FileInstance? {
        return if (schemes.contains(uri.scheme!!)) {
            getLocalFileSystemInstance(context, uri)
        } else {
            null
        }
    }

    override fun getPrefix(context: Context, uri: Uri): FileSystemPrefix =
        getLocalFileSystemPrefix(context, uri.path!!)

    companion object {
        val schemes = listOf(ContentResolver.SCHEME_FILE)
    }
}

class DocumentFileInstanceFactory : FileInstanceFactory {

    override suspend fun buildInstance(context: Context, uri: Uri): FileInstance? {
        return if (schemes.contains(uri.scheme)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DocumentLocalFileInstance(
                    "/${uri.rawTree}",
                    uri.authority!!,
                    uri.tree,
                    context,
                    uri
                )
            } else {
                TODO("VERSION.SDK_INT < LOLLIPOP")
            }
        } else {
            null
        }
    }

    companion object {
        val schemes = listOf(ContentResolver.SCHEME_CONTENT)
    }
}
