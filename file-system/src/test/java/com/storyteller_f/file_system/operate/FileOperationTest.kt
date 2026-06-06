package com.storyteller_f.file_system.operate

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.message.Message
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class FileOperationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun copyFileCopiesBytesAndNotifiesListener() = runBlocking {
        val source = temporaryFolder.newFile("source.txt").apply { writeText("hello") }
        val targetDir = temporaryFolder.newFolder("target")
        val listener = RecordingListener()

        val result = ScopeFileCopyOp(
            LocalTestFileInstance(source),
            LocalTestFileInstance(targetDir),
            context
        ).bind(listener).call()

        assertTrue(result)
        assertEquals("hello", File(targetDir, "source.txt").readText())
        assertEquals(1, listener.filesDone)
        assertEquals(5, listener.lastSize)
        assertEquals(0, listener.errors)
    }

    @Test
    fun copyDirectoryRecursivelyCopiesChildrenAndNotifiesDirectoryDone() = runBlocking {
        val sourceDir = temporaryFolder.newFolder("sourceDir")
        File(sourceDir, "one.txt").writeText("one")
        File(sourceDir, "nested").mkdir()
        File(sourceDir, "nested/two.txt").writeText("two")
        val targetDir = temporaryFolder.newFolder("copyTarget")
        val listener = RecordingListener()

        val result = ScopeFileCopyOp(
            LocalTestFileInstance(sourceDir),
            LocalTestFileInstance(targetDir),
            context
        ).bind(listener).call()

        assertTrue(result)
        assertEquals("one", File(targetDir, "sourceDir/one.txt").readText())
        assertEquals("two", File(targetDir, "sourceDir/nested/two.txt").readText())
        assertTrue(listener.directoriesDone >= 2)
        assertEquals(0, listener.errors)
    }

    @Test
    fun moveFileCopiesThenDeletesSource() = runBlocking {
        val source = temporaryFolder.newFile("move.txt").apply { writeText("move me") }
        val targetDir = temporaryFolder.newFolder("moveTarget")

        val result = ScopeFileMoveOp(
            LocalTestFileInstance(source),
            LocalTestFileInstance(targetDir),
            context
        ).call()

        assertTrue(result)
        assertFalse(source.exists())
        assertEquals("move me", File(targetDir, "move.txt").readText())
    }

    @Test
    fun deleteDirectoryRemovesNestedFilesAndDirectories() = runBlocking {
        val root = temporaryFolder.newFolder("deleteRoot")
        File(root, "one.txt").writeText("one")
        File(root, "nested").mkdir()
        File(root, "nested/two.txt").writeText("two")
        val listener = RecordingListener()

        val result = FileDeleteOp(LocalTestFileInstance(root), context).bind(listener).call()

        assertTrue(result)
        assertFalse(root.exists())
        assertTrue(listener.filesDone >= 2)
        assertTrue(listener.directoriesDone >= 2)
        assertEquals(0, listener.errors)
    }

    @Test
    fun deleteDirectoryReportsFailureWhenDeleteFails() = runBlocking {
        val instance = LocalTestFileInstance(temporaryFolder.newFolder("undeletable"), deleteResult = false)
        val listener = RecordingListener()

        val result = FileDeleteOp(instance, context).bind(listener).call()

        assertFalse(result)
        assertEquals(1, listener.errors)
    }

    private class RecordingListener : FileOperationListener {
        var filesDone = 0
        var directoriesDone = 0
        var errors = 0
        var lastSize = -1L

        override fun onFileDone(fileInstance: FileInstance?, message: Message?, size: Long) {
            filesDone++
            lastSize = size
        }

        override fun onDirectoryDone(fileInstance: FileInstance?, message: Message?) {
            directoriesDone++
        }

        override fun onError(message: Message?) {
            errors++
        }
    }

    private class LocalTestFileInstance(
        private val file: File,
        private val deleteResult: Boolean = true,
    ) : FileInstance(Uri.Builder().scheme("test-file").path(file.absolutePath).build()) {
        override val path: String = file.absolutePath

        override suspend fun filePermissions() = FilePermissions.USER_READABLE

        override suspend fun fileTime() = FileTime(lastModified = file.lastModified())

        override suspend fun fileKind(): FileKind {
            return if (file.isFile) {
                FileKind.File(null, file.isHidden, file.length(), extension)
            } else {
                FileKind.Directory(null, file.isHidden)
            }
        }

        override suspend fun getFileInputStream(): FileInputStream {
            return file.inputStream()
        }

        override suspend fun getFileOutputStream(): FileOutputStream {
            return file.outputStream()
        }

        override suspend fun listInternal(fileSystemPack: FileSystemPack) {
            file.listFiles().orEmpty().forEach { child ->
                val info = FileInfo(
                    child.name,
                    Uri.Builder().scheme("test-file").path(child.absolutePath).build(),
                    FileTime(lastModified = child.lastModified()),
                    if (child.isFile) {
                        FileKind.File(null, child.isHidden, child.length(), child.extension)
                    } else {
                        FileKind.Directory(null, child.isHidden)
                    },
                    FilePermissions.USER_READABLE
                )
                if (child.isFile) fileSystemPack.addFile(info) else fileSystemPack.addDirectory(info)
            }
        }

        override suspend fun exists() = file.exists()

        override suspend fun createFile(): Boolean {
            file.parentFile?.mkdirs()
            return file.createNewFile()
        }

        override suspend fun createDirectory(): Boolean {
            return file.mkdirs()
        }

        override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance? {
            val child = LocalTestFileInstance(File(file, name))
            when (policy) {
                is FileCreatePolicy.Create -> if (policy.isFile) child.createFile() else child.createDirectory()
                FileCreatePolicy.NotCreate -> Unit
            }
            return child
        }

        override suspend fun toParent(): FileInstance {
            return LocalTestFileInstance(file.parentFile!!)
        }

        override suspend fun deleteFileOrEmptyDirectory(): Boolean {
            return deleteResult && file.delete()
        }

        override suspend fun rename(newName: String): FileInstance? {
            val target = File(file.parentFile, newName)
            return if (file.renameTo(target)) LocalTestFileInstance(target) else null
        }
    }
}
