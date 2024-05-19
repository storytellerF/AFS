package com.storyteller_f.file_system_local

import android.content.ContentResolver
import android.os.Build
import androidx.annotation.RequiresApi
import com.storyteller_f.file_system.AFSProvider

@RequiresApi(Build.VERSION_CODES.O)
class LocalFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return ContentResolver.SCHEME_FILE
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class ContentFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return ContentResolver.SCHEME_CONTENT
    }
}
