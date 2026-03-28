package com.storyteller_f.file_system_remote

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpecTest {

    @Test
    fun testShareSpecToUri() {
        val shareSpec = ShareSpec(
            server = "192.168.1.1",
            port = 445,
            user = "admin",
            password = "password123",
            type = "smb",
            share = "public"
        )
        val uri = shareSpec.toUri()
        assertEquals("smb://admin:password123@192.168.1.1:445/public", uri.toString())
    }

    @Test
    fun testShareSpecParse() {
        val uri = Uri.parse("smb://guest:nopass@10.0.0.1:445/media")
        val shareSpec = ShareSpec.parse(uri)

        assertEquals("10.0.0.1", shareSpec.server)
        assertEquals(445, shareSpec.port)
        assertEquals("guest", shareSpec.user)
        assertEquals("nopass", shareSpec.password)
        assertEquals("smb", shareSpec.type)
        assertEquals("media", shareSpec.share)
    }

    @Test
    fun testRemoteSpecToUri() {
        val remoteSpec = RemoteSpec(
            server = "ftp.example.com",
            port = 21,
            user = "user1",
            password = "pwd",
            type = "ftp"
        )
        val uri = remoteSpec.toUri()
        // server uses base64
        val expectedServer = "ftp.example.com".toByteArray().let { kotlin.io.encoding.Base64.encode(it) }
        assertEquals("ftp://user1:pwd@$expectedServer:21/", uri.toString())
    }

    @Test
    fun testRemoteSpecParse() {
        val base64Server = "server.net".toByteArray().let { kotlin.io.encoding.Base64.encode(it) }
        val uri = Uri.parse("sftp://root:secret@$base64Server:22/")
        val remoteSpec = RemoteSpec.parse(uri)

        assertEquals("server.net", remoteSpec.server)
        assertEquals(22, remoteSpec.port)
        assertEquals("root", remoteSpec.user)
        assertEquals("secret", remoteSpec.password)
        assertEquals("sftp", remoteSpec.type)
    }
}
