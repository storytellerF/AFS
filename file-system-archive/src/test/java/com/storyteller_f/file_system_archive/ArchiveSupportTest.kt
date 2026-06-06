package com.storyteller_f.file_system_archive

import android.net.Uri
import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.zip.ZipEntry

@RunWith(RobolectricTestRunner::class)
class ArchiveSupportTest {
    @Test
    fun factoryHandlesSupportedAndUnsupportedInputs() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val factory = ArchiveFileInstanceFactory()
        val archiveUri = Uri.Builder().scheme(ArchiveFileInstance.SCHEME).authority("root").path("/").build()
        val otherUri = Uri.Builder().scheme("other").path("/").build()

        assertTrue(factory.buildInstance(context, archiveUri) is ArchiveFileInstance)
        assertEquals(ArchiveFileSystemPrefix, factory.getPrefix(context, archiveUri))
        assertNull(factory.buildInstance(context, otherUri))
        assertNull(factory.getPrefix(context, otherUri))
    }

    @Test
    fun factoryBuildNestedFileRequiresZipExtension() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val factory = ArchiveFileInstanceFactory()
        val zipFile = context.buildZip("nested.zip", listOf(Node("hello.txt", content = "hello")))
        val textFile = context.buildZip("not_zip.txt", listOf(Node("hello.txt", content = "hello")))
        val zipInstance = getFileInstance(context, zipFile.toArchiveTestUri())!!
        val textInstance = getFileInstance(context, textFile.toArchiveTestUri())!!

        assertEquals(ArchiveFileInstance.SCHEME, factory.buildNestedFile(context, null, zipInstance)!!.scheme)
        assertNull(factory.buildNestedFile(context, null, textInstance))
    }

    @Test
    fun zipEntryExtensionsExposeKindAndTime() {
        val file = ZipEntry("folder/file.txt").apply {
            size = 123
            time = 1000
        }
        val directory = ZipEntry("folder/").apply {
            time = 2000
        }

        val fileKind = file.fileKind()
        val directoryKind = directory.fileKind()

        assertTrue(fileKind is FileKind.File)
        assertEquals(123, (fileKind as FileKind.File).size)
        assertEquals("txt", fileKind.extension)
        assertTrue(directoryKind.isDirectory)
        assertTrue(file.fileTime().lastModified != null)
        assertTrue(directory.fileTime().lastModified != null)
    }

    @Test
    fun providerReturnsArchiveScheme() {
        assertEquals(ArchiveFileInstance.SCHEME, ArchiveFileSystemProvider().scheme)
    }

    @Test
    fun unsupportedArchiveMutationsThrowNotImplemented() {
        val context = RuntimeEnvironment.getApplication()
        val archive = runBlocking {
            val zipFile = context.buildZip("readonly.zip", listOf(Node("hello.txt", content = "hello")))
            getFileInstance(
                context,
                ArchiveFileInstanceFactory.buildNestedFile(zipFile.toArchiveTestUri(), "/hello.txt")!!
            )!!
        }

        assertThrows(NotImplementedError::class.java) {
            runBlocking { archive.getFileInputStream() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { archive.getFileOutputStream() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { archive.createFile() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { archive.createDirectory() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { archive.deleteFileOrEmptyDirectory() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { archive.rename("new.txt") }
        }
    }
}
