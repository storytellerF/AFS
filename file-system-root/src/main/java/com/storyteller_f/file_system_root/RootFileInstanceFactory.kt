package com.storyteller_f.file_system_root

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance

class RootFileInstanceFactory : FileInstanceFactory {
    override val schemes: List<String>
        get() = listOf(RootAccessFileInstance.ROOT_FILESYSTEM_SCHEME)

    override suspend fun buildInstance(context: Context, uri: Uri): FileInstance? {
        return if (RootAccessFileInstance.ROOT_FILESYSTEM_SCHEME == uri.scheme) {
            RootAccessFileInstance.buildInstance(uri)!!
        } else {
            null
        }
    }
}
