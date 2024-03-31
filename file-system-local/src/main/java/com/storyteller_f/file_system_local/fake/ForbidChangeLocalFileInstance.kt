package com.storyteller_f.file_system_local.fake

import android.net.Uri
import com.storyteller_f.file_system.instance.FileInstance

/**
 * 禁止修改操作
 */
abstract class ForbidChangeLocalFileInstance(uri: Uri) :
    DirectoryLocalFileInstance(uri) {
    override suspend fun createDirectory() = false

    override suspend fun deleteFileOrEmptyDirectory() = false

    override suspend fun rename(newName: String): FileInstance? {
        TODO("Not yet implemented")
    }
}
