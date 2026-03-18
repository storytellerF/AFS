package com.storyteller_f.file_system.instance

import org.junit.Assert.assertEquals
import org.junit.Test

class FilePermissionsTest {

    @Test
    fun testFilePermissionToString() {
        val rwx = FilePermission(readable = true, writable = true, executable = true)
        assertEquals("-w-", FilePermission(writable = true).toString())
        assertEquals("r--", FilePermission(readable = true).toString())
        assertEquals("--x", FilePermission(executable = true).toString())
        assertEquals("rw-", FilePermission(readable = true, writable = true).toString())
        assertEquals("r-x", FilePermission(readable = true, executable = true).toString())
        assertEquals("-wx", FilePermission(writable = true, executable = true).toString())
        assertEquals("rwx", rwx.toString())
        assertEquals("---", FilePermission().toString())
    }

    @Test
    fun testFilePermissionsToString() {
        val user = FilePermission(readable = true, writable = true)
        val group = FilePermission(readable = true)
        val others = FilePermission()
        val permissions = FilePermissions(user, group, others)
        
        assertEquals("rw--r------", permissions.toString())
        
        val permissionsNull = FilePermissions(userPermission = user)
        assertEquals("rw---------", permissionsNull.toString())
    }

    @Test
    fun testFilePermissionsPermissions() {
        val perms = FilePermissions.permissions(r = true, w = false, e = true)
        assertEquals("r-x", perms.userPermission.toString())
    }
}
