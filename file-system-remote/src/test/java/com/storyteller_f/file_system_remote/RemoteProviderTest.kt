package com.storyteller_f.file_system_remote

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteProviderTest {
    @Test
    fun providersExposeTheirSchemes() {
        assertEquals(RemoteSchemes.FTP, FtpFileSystemProvider().scheme)
        assertEquals(RemoteSchemes.FTPS, FtpsFileSystemProvider().scheme)
        assertEquals(RemoteSchemes.HTTP, HttpFileSystemProvider().scheme)
        assertEquals(RemoteSchemes.HTTPS, HttpsFileSystemProvider().scheme)
        assertEquals(RemoteSchemes.SFTP, SFtpFileSystemProvider().scheme)
        assertEquals(RemoteSchemes.SMB, SmbFileSystemProvider().scheme)
        assertEquals(RemoteSchemes.WEB_DAV, WebDavFileSystemProvider().scheme)
    }
}
