package com.storyteller_f.file_system_root

import android.os.Build
import androidx.annotation.RequiresApi
import com.storyteller_f.file_system.AFSProvider

@RequiresApi(Build.VERSION_CODES.O)
class RootFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return "root"
    }
}
