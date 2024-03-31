package com.storyteller_f.file_system.instance

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileTime(val lastModified: Long? = null, val lastAccessed: Long? = null, val created: Long? = null) {
    private val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd hh:mm:ss sss", Locale.CHINA)
    private val Long.time get() = simpleDateFormat.format(Date(this))

    val formattedLastModifiedTime = lastModified?.time
    val formattedLastAccessTime = lastAccessed?.time
    val formattedCreatedTime = created?.time
}
