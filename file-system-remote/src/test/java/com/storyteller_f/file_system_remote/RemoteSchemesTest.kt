package com.storyteller_f.file_system_remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class RemoteSchemesTest {

    @Test
    fun testExcludeHttpProtocol() {
        val excludeHttp = RemoteSchemes.EXCLUDE_HTTP_PROTOCOL
        assertTrue(excludeHttp.contains(RemoteSchemes.SMB))
        assertTrue(excludeHttp.contains(RemoteSchemes.SFTP))
        assertTrue(excludeHttp.contains(RemoteSchemes.FTP))
        assertTrue(excludeHttp.contains(RemoteSchemes.FTP_ES))
        assertTrue(excludeHttp.contains(RemoteSchemes.FTPS))
        assertTrue(excludeHttp.contains(RemoteSchemes.WEB_DAV))
        
        assertFalse(excludeHttp.contains(RemoteSchemes.HTTP))
        assertFalse(excludeHttp.contains(RemoteSchemes.HTTPS))
    }

    @Test
    fun testHttpProtocol() {
        val httpProtocol = RemoteSchemes.HTTP_PROTOCOL
        assertTrue(httpProtocol.contains(RemoteSchemes.HTTP))
        assertTrue(httpProtocol.contains(RemoteSchemes.HTTPS))
        
        assertFalse(httpProtocol.contains(RemoteSchemes.SMB))
    }

    @Test
    fun testAllProtocol() {
        val allOptions = RemoteSchemes.ALL_PROTOCOL
        assertEquals(8, allOptions.size)
        assertTrue(allOptions.containsAll(RemoteSchemes.EXCLUDE_HTTP_PROTOCOL))
        assertTrue(allOptions.containsAll(RemoteSchemes.HTTP_PROTOCOL))
    }

    @Test
    fun testDefaultPorts() {
        val defaultPorts = RemoteSchemes.DEFAULT_PORT
        assertEquals(6, defaultPorts.size)
        assertEquals(-1, defaultPorts[0])
        assertEquals(80, defaultPorts[5])
    }
}
