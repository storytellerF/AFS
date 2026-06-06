package com.storyteller_f.file_system_remote.instance

import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system_remote.RemoteSchemes
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.ShareSpec
import kotlinx.coroutines.runBlocking
import org.apache.commons.net.ftp.FTPFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class RemoteInstanceSupportTest {
    @Test
    fun ftpFileHelpersMapPermissionsSizeAndTime() {
        val ftpFile = FTPFile().apply {
            name = "readme.txt"
            type = FTPFile.FILE_TYPE
            size = 42
            timestamp = Calendar.getInstance().apply { timeInMillis = 1234 }
            setPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION, true)
            setPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION, true)
            setPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION, true)
        }

        assertEquals(42, ftpFile.fileLength())
        assertEquals("rw-", ftpFile.filePermission(FTPFile.USER_ACCESS).toString())
        assertEquals("r--", ftpFile.filePermission(FTPFile.GROUP_ACCESS).toString())
        assertEquals("rw--r------", ftpFile.permissions().toString())
        assertTrue(ftpFile.fileTime().lastModified != null)
    }

    @Test
    fun unsupportedFtpMutationsThrowWithoutConnecting() {
        val instance = FtpFileInstance(remoteSpec(RemoteSchemes.FTP).toUri())

        assertThrows(NotImplementedError::class.java) { runBlocking { instance.getFileInputStream() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { instance.getFileOutputStream() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { instance.createFile() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { instance.createDirectory() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { instance.deleteFileOrEmptyDirectory() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { instance.rename("new") } }
        assertThrows(NotImplementedError::class.java) { runBlocking { instance.toParent() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { instance.toChild("child", FileCreatePolicy.NotCreate) } }
    }

    @Test
    fun childBuildersPreserveSpecWithoutConnecting() = runBlocking {
        val ftps = FtpsFileInstance(remoteSpec(RemoteSchemes.FTPS).toUri())
        val sftp = SFtpFileInstance(remoteSpec(RemoteSchemes.SFTP).toUri())
        val smb = SmbFileInstance(shareSpec().toUri())
        val webdav = WebDavFileInstance(remoteSpec(RemoteSchemes.WEB_DAV).toUri())

        assertEquals("/child.txt", ftps.toChild("child.txt", FileCreatePolicy.NotCreate).path)
        assertEquals("/child.txt", sftp.toChild("child.txt", FileCreatePolicy.NotCreate).path)
        assertEquals("/child.txt", smb.toChild("child.txt", FileCreatePolicy.NotCreate).path)
        assertEquals("/child.txt", webdav.toChild("child.txt", FileCreatePolicy.NotCreate).path)
    }

    @Test
    fun unsupportedSftpAndSmbAndWebDavOperationsThrowWithoutConnecting() {
        val sftp = SFtpFileInstance(remoteSpec(RemoteSchemes.SFTP).toUri())
        val smb = SmbFileInstance(shareSpec().toUri())
        val webdav = WebDavFileInstance(remoteSpec(RemoteSchemes.WEB_DAV).toUri())

        assertThrows(NotImplementedError::class.java) { runBlocking { sftp.getFileInputStream() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { sftp.getFileOutputStream() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { sftp.createFile() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { sftp.createDirectory() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { sftp.deleteFileOrEmptyDirectory() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { sftp.rename("new") } }
        assertThrows(NotImplementedError::class.java) { runBlocking { sftp.toParent() } }

        assertThrows(NotImplementedError::class.java) { runBlocking { smb.getFileInputStream() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { smb.getFileOutputStream() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { smb.createFile() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { smb.createDirectory() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { smb.deleteFileOrEmptyDirectory() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { smb.rename("new") } }
        assertThrows(NotImplementedError::class.java) { runBlocking { smb.toParent() } }

        assertThrows(NotImplementedError::class.java) { runBlocking { webdav.getFileInputStream() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { webdav.getFileOutputStream() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { webdav.getOutputStream() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { webdav.createFile() } }
        assertThrows(NotImplementedError::class.java) { runBlocking { webdav.rename("new") } }
        assertThrows(NotImplementedError::class.java) { runBlocking { webdav.toParent() } }
    }

    private fun remoteSpec(scheme: String): RemoteSpec {
        return RemoteSpec("example.test", 22, "user", "pass", scheme)
    }

    private fun shareSpec(): ShareSpec {
        return ShareSpec("example.test", 445, "user", "pass", RemoteSchemes.SMB, "share")
    }
}
