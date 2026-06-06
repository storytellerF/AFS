package com.storyteller_f.file_system_memory

import android.net.Uri
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemoryFileInstanceUnitTest {

    @Before
    fun setup() {
        val fileSystem = Jimfs.newFileSystem(Configuration.unix())
        MemoryFileInstance.memoryFileSystems["test"] = fileSystem
    }

    @Test
    fun testCreateAndListFile() = runTest {
        val parentUri = Uri.Builder().scheme(MemoryFileInstance.SCHEME).authority("test").path("/home").build()
        val parentList = MemoryFileInstance(parentUri)
        parentList.createDirectory() // setup parent directory

        val uri = Uri.Builder().scheme(MemoryFileInstance.SCHEME).authority("test").path("/home/hello.txt").build()
        val fileInstance = MemoryFileInstance(uri)
        assertTrue(fileInstance.createFile())
        assertTrue(fileInstance.exists())

        fileInstance.getOutputStream().bufferedWriter().use { it.write("Hello World") }
        val readContent = fileInstance.getInputStream().bufferedReader().readText()
        assertEquals("Hello World", readContent)

        val kind = fileInstance.fileKind()
        assertTrue(kind.isFile)
        assertFalse(kind.isDirectory)

        val time = fileInstance.fileTime()
        assertNotNull(time.lastModified)

        val perms = fileInstance.filePermissions()
        assertTrue(perms.userPermission.readable)

        assertTrue(fileInstance.deleteFileOrEmptyDirectory())
        assertFalse(fileInstance.exists())
    }

    @Test
    fun testDirectoryAndRename() = runTest {
        val dirUri = Uri.Builder().scheme(MemoryFileInstance.SCHEME).authority("test").path("/books").build()
        val dirInstance = MemoryFileInstance(dirUri)
        assertTrue(dirInstance.createDirectory())
        assertTrue(dirInstance.exists())

        val kind = dirInstance.fileKind()
        assertTrue(kind.isDirectory)

        var child = dirInstance.toChild("novel.txt", FileCreatePolicy.NotCreate)
        assertNotNull(child)
        assertFalse(child!!.exists())

        child = dirInstance.toChild("novel.txt", FileCreatePolicy.Create(true))
        assertNotNull(child)
        assertTrue(child!!.exists())

        val renamed = child.rename("epic.txt")
        assertNotNull(renamed)
        assertTrue(renamed!!.exists())
        assertFalse(child.exists())

        val parent = renamed.toParent()
        assertTrue(parent.exists())
        assertEquals("/books", parent.uri.path)
    }

    @Test
    fun testCreateDirectoryChildAndListDirectoryContents() = runTest {
        val dirUri = Uri.Builder().scheme(MemoryFileInstance.SCHEME).authority("test").path("/library").build()
        val dirInstance = MemoryFileInstance(dirUri)
        assertTrue(dirInstance.createDirectory())

        val file = dirInstance.toChild("novel.txt", FileCreatePolicy.Create(true))!!
        val directory = dirInstance.toChild("authors", FileCreatePolicy.Create(false))!!

        val pack = dirInstance.list()
        assertEquals(2, pack.count)
        assertEquals("novel.txt", pack.files.single().name)
        assertEquals(file.uri, pack.files.single().uri)
        assertEquals("authors", pack.directories.single().name)
        assertEquals(directory.uri, pack.directories.single().uri)
    }

    @Test
    fun testFactoryBuildsOnlyMemoryInstances() = runTest {
        val factory = MemoryFileInstanceFactory()
        val memoryUri = Uri.Builder().scheme(MemoryFileInstance.SCHEME).authority("test").path("/home").build()
        val regularUri = Uri.Builder().scheme("file").path("/home").build()

        assertTrue(factory.buildInstance(memoryUri) is MemoryFileInstance)
        assertNull(factory.buildInstance(regularUri))
    }

    @Test
    fun testFileStreamMethodsAreNotImplemented() {
        val uri = Uri.Builder().scheme(MemoryFileInstance.SCHEME).authority("test").path("/home/hello.txt").build()
        val fileInstance: FileInstance = MemoryFileInstance(uri)

        assertThrows(NotImplementedError::class.java) {
            runBlocking {
                fileInstance.getFileInputStream()
            }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking {
                fileInstance.getFileOutputStream()
            }
        }
    }
}
