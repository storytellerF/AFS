package com.storyteller_f.file_system_root

import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory2
import com.storyteller_f.file_system.instance.FileInstance

class RootFileInstanceFactory : FileInstanceFactory2 {

    override suspend fun buildInstance(uri: Uri): FileInstance? {
        return if (RootAccessFileInstance.ROOT_FILESYSTEM_SCHEME == uri.scheme) {
            RootAccessFileInstance.buildInstance(uri)!!
        } else {
            null
        }
    }
}
