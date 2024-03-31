package com.storyteller_f.file_system.model

import android.net.Uri
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime

open class FileInfo(
    val name: String,
    val uri: Uri,
    val time: FileTime,
    val kind: FileKind,
    val permissions: FilePermissions,
) {
    val fullPath: String = uri.path!!
    val extension = (kind as? FileKind.File)?.extension
}
