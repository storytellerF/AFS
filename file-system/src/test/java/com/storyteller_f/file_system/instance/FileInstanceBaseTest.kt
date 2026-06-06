package com.storyteller_f.file_system.instance

import android.net.Uri
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class FileInstanceBaseTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun exposesPathNameParentAndExtensionFromUri() {
        val instance = TestFileInstance(Uri.Builder().scheme("test").path("/books/novel.txt").build())

        assertEquals("/books/novel.txt", instance.path)
        assertEquals("/books", instance.parent)
        assertEquals("novel.txt", instance.name)
        assertEquals("txt", instance.extension)
    }

    @Test
    fun equalityAndHashCodeUseUriAndConcreteType() {
        val uri = Uri.Builder().scheme("test").path("/same.txt").build()
        val first = TestFileInstance(uri)
        val second = TestFileInstance(uri)
        val other = TestFileInstance(Uri.Builder().scheme("test").path("/other.txt").build())

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, other)
        assertNotEquals(first, Any())
    }

    @Test
    fun getFileInfoAndListUseSubclassData() = runBlocking {
        val child = info("child.txt", FileKind.File(null, false, 4, "txt"))
        val directory = info("docs", FileKind.Directory(null, false))
        val instance = TestFileInstance(
            Uri.Builder().scheme("test").path("/root").build(),
            files = listOf(child),
            directories = listOf(directory)
        )

        val fileInfo = instance.getFileInfo()
        val pack = instance.list()

        assertEquals("root", fileInfo.name)
        assertTrue(fileInfo.kind.isDirectory)
        assertEquals(listOf(child), pack.files)
        assertEquals(listOf(directory), pack.directories)
        assertEquals(2, pack.count)
    }

    @Test
    fun defaultStreamsDelegateToFileStreams() = runBlocking {
        val backingFile = temporaryFolder.newFile("stream.txt")
        val instance = TestFileInstance(
            Uri.Builder().scheme("test").path("/stream.txt").build(),
            backingFile = backingFile
        )

        instance.getOutputStream().bufferedWriter().use { it.write("hello") }

        assertEquals("hello", instance.getInputStream().bufferedReader().use { it.readText() })
    }

    @Test
    fun createDeleteRenameAndChildMethodsUseFakeState() = runBlocking {
        val instance = TestFileInstance(Uri.Builder().scheme("test").path("/root").build())

        assertFalse(instance.exists())
        assertTrue(instance.createDirectory())
        assertTrue(instance.exists())

        val child = instance.toChild("child.txt", FileCreatePolicy.Create(true))!!
        assertTrue(child.exists())
        assertEquals("/root/child.txt", child.path)

        val renamed = child.rename("renamed.txt")!!
        assertEquals("/root/renamed.txt", renamed.path)
        assertTrue(child.deleteFileOrEmptyDirectory())
        assertFalse(child.exists())
        assertEquals("/root", renamed.toParent().path)
    }

    private fun info(name: String, kind: FileKind): FileInfo {
        return FileInfo(
            name = name,
            uri = Uri.Builder().path("/$name").build(),
            time = FileTime(),
            kind = kind,
            permissions = FilePermissions.USER_READABLE
        )
    }

    private class TestFileInstance(
        uri: Uri,
        private val files: List<FileInfo> = emptyList(),
        private val directories: List<FileInfo> = emptyList(),
        private val backingFile: File? = null,
    ) : FileInstance(uri) {
        private var exists = false
        private var directory = true

        override suspend fun filePermissions() = FilePermissions.USER_READABLE

        override suspend fun fileTime() = FileTime()

        override suspend fun fileKind(): FileKind {
            return if (directory) {
                FileKind.Directory(null, false)
            } else {
                FileKind.File(null, false, backingFile?.length() ?: 0, extension)
            }
        }

        override suspend fun getFileInputStream(): FileInputStream {
            return FileInputStream(backingFile!!)
        }

        override suspend fun getFileOutputStream(): FileOutputStream {
            return FileOutputStream(backingFile!!)
        }

        override suspend fun listInternal(fileSystemPack: FileSystemPack) {
            fileSystemPack.addFiles(files)
            fileSystemPack.addDirectories(directories)
        }

        override suspend fun exists() = exists

        override suspend fun createFile(): Boolean {
            exists = true
            directory = false
            return true
        }

        override suspend fun createDirectory(): Boolean {
            exists = true
            directory = true
            return true
        }

        override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance? {
            return TestFileInstance(childUri(name)).apply {
                when (policy) {
                    is FileCreatePolicy.Create -> if (policy.isFile) createFile() else createDirectory()
                    FileCreatePolicy.NotCreate -> Unit
                }
            }
        }

        override suspend fun toParent(): FileInstance {
            return TestFileInstance(parentUri()).apply { createDirectory() }
        }

        override suspend fun deleteFileOrEmptyDirectory(): Boolean {
            exists = false
            return true
        }

        override suspend fun rename(newName: String): FileInstance? {
            return TestFileInstance(overridePath("${parent!!}/$newName")).apply {
                exists = this@TestFileInstance.exists
                directory = this@TestFileInstance.directory
            }
        }
    }
}
