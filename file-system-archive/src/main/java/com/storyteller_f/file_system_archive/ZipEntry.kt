package com.storyteller_f.file_system_archive

import android.os.Build
import com.storyteller_f.file_system.getExtension
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FileTime
import java.util.zip.ZipEntry

fun ZipEntry.fileKind(): FileKind {
    val size = size
    return FileKind.build(
        !isDirectory,
        isSymbolicLink = false,
        isHidden = false,
        size = size,
        extension = getExtension(name).orEmpty()
    )
}

fun ZipEntry.fileTime() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        FileTime(
            lastModifiedTime?.toMillis(),
            lastAccessTime?.toMillis(),
            creationTime?.toMillis()
        )
    } else {
        FileTime(time)
    }
