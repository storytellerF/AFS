package com.storyteller_f.file_system.instance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FileTimeTest {

    @Test
    fun testFileTimeFormatting() {
        val calendar = Calendar.getInstance()
        calendar.set(2023, Calendar.JANUARY, 15, 14, 30, 45) // 2023-01-15 14:30:45
        calendar.set(Calendar.MILLISECOND, 500)
        
        val timestamp = calendar.timeInMillis
        val fileTime = FileTime(lastModified = timestamp)
        
        val sdf = SimpleDateFormat("yyyy:MM:dd hh:mm:ss sss", Locale.CHINA)
        val expectedDateStr = sdf.format(Date(timestamp))
        
        assertEquals(expectedDateStr, fileTime.formattedLastModifiedTime)
        assertNull(fileTime.formattedLastAccessTime)
        assertNull(fileTime.formattedCreatedTime)
        
        val allTime = FileTime(lastModified = timestamp, lastAccessed = timestamp, created = timestamp)
        assertEquals(expectedDateStr, allTime.formattedLastAccessTime)
        assertEquals(expectedDateStr, allTime.formattedCreatedTime)
    }
}
