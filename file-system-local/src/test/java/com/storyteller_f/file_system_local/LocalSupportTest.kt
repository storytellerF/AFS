package com.storyteller_f.file_system_local

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system_local.instance.fake.FakeLocalFileInstance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LocalSupportTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun fileSystemUriStorePersistsAndRestoresUris() {
        val context = RuntimeEnvironment.getApplication()
        val store = FileSystemUriStore()
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload")

        assertNull(store.savedUri(context, "authority", "primary:Download"))

        store.saveUri(context, "authority", uri, "primary:Download")

        assertEquals(uri, store.savedUri(context, "authority", "primary:Download"))
        assertEquals(mapOf("authority" to listOf("primary:Download")), store.savedUris(context))
    }

    @Test
    fun fileHelpersExposePermissionsTimeSpaceAndVolumeName() = runBlocking {
        val file = temporaryFolder.newFile("helper.txt").apply { writeText("hello") }
        val dir = temporaryFolder.root

        assertTrue(file.permissions().userPermission.readable)
        assertNotNull(file.fileTime().lastModified)
        assertEquals("emulated", volumePathName(null))
        assertEquals("ABCD-1234", volumePathName("ABCD-1234"))
        assertTrue(getSpace(dir.absolutePath) >= 0)
        assertTrue(getFree(dir.absolutePath) >= 0)
        assertTrue(getTotal(dir.absolutePath) >= 0)
    }

    @Test
    fun fakeLocalFileInstanceListsPresetDirectoriesAndForbidsMutation() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val root = FakeLocalFileInstance(context, Uri.Builder().scheme("file").path("/").build())

        val pack = root.list()

        assertTrue(root.exists())
        assertTrue(root.fileKind().isDirectory)
        assertEquals(FileKind.Directory(null, false), root.fileKind())
        assertTrue(pack.directories.map { it.name }.containsAll(listOf("sdcard", "storage", "data")))
        assertFalse(root.createFile())
        assertFalse(root.createDirectory())
        assertFalse(root.deleteFileOrEmptyDirectory())
        assertThrows(NotImplementedError::class.java) {
            runBlocking { root.rename("new") }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { root.getFileInputStream() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { root.getFileOutputStream() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { root.fileTime() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { root.toParent() }
        }

        val child = root.toChild("data", FileCreatePolicy.NotCreate)
        assertEquals("/data", child.path)
    }
}
