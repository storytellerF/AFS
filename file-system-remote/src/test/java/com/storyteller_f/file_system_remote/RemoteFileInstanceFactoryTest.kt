package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system_remote.instance.FtpFileInstance
import com.storyteller_f.file_system_remote.instance.FtpsFileInstance
import com.storyteller_f.file_system_remote.instance.HttpFileInstance
import com.storyteller_f.file_system_remote.instance.SFtpFileInstance
import com.storyteller_f.file_system_remote.instance.SmbFileInstance
import com.storyteller_f.file_system_remote.instance.WebDavFileInstance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteFileInstanceFactoryTest {
    @Test
    fun factoryBuildsExpectedInstanceTypes() = runBlocking {
        val factory = RemoteFileInstanceFactory()

        assertTrue(factory.buildInstance(Uri.parse("http://example.test/file.txt")) is HttpFileInstance)
        assertTrue(factory.buildInstance(Uri.parse("https://example.test/file.txt")) is HttpFileInstance)
        assertTrue(factory.buildInstance(remoteUri(RemoteSchemes.FTP)) is FtpFileInstance)
        assertTrue(factory.buildInstance(remoteUri(RemoteSchemes.SFTP)) is SFtpFileInstance)
        assertTrue(factory.buildInstance(shareUri(RemoteSchemes.SMB)) is SmbFileInstance)
        assertTrue(factory.buildInstance(remoteUri(RemoteSchemes.FTP_ES)) is FtpsFileInstance)
        assertTrue(factory.buildInstance(remoteUri(RemoteSchemes.FTPS)) is FtpsFileInstance)
        assertTrue(factory.buildInstance(remoteUri(RemoteSchemes.WEB_DAV)) is WebDavFileInstance)
        assertNull(factory.buildInstance(Uri.parse("file:///tmp/file.txt")))
    }

    @Test
    fun getRemoteInstanceRejectsUnknownScheme() {
        assertThrows(Exception::class.java) {
            getRemoteInstance(Uri.parse("unknown://host/"))
        }
    }

    private fun remoteUri(scheme: String): Uri {
        return RemoteSpec("example.test", 22, "user", "pass", scheme).toUri()
    }

    private fun shareUri(scheme: String): Uri {
        return ShareSpec("example.test", 445, "user", "pass", scheme, "share").toUri()
    }
}
