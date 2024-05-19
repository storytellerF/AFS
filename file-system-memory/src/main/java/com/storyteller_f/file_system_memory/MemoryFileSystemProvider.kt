package com.storyteller_f.file_system_memory

import com.storyteller_f.file_system.AFSProvider

class MemoryFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return "memory"
    }
}
