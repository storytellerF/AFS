package com.storyteller_f.file_system_archive

import android.os.Build
import androidx.annotation.RequiresApi
import com.storyteller_f.file_system.AFSProvider

@RequiresApi(Build.VERSION_CODES.O)
class ArchiveFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return ArchiveFileInstance.SCHEME
    }
}
