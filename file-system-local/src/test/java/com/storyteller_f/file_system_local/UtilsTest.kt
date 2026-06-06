package com.storyteller_f.file_system_local

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class UtilsTest {

    @Test
    fun testExtractSdPath() {
        assertEquals("/storage/XX44-XX55", extractSdPath("/storage/XX44-XX55/test"))
        assertEquals("/storage/XX44-XX55", extractSdPath("/storage/XX44-XX55/"))
        assertEquals("/storage/XX44-XX55", extractSdPath("/storage/XX44-XX55"))
        assertEquals("/storage/0000-0000", extractSdPath("/storage/0000-0000/Android/data/com.test"))
    }

    @Test
    fun testCurrentUserPathsAndLocalPrefixes() {
        val context = RuntimeEnvironment.getApplication()
        val uid = context.getMyId()
        val packageName = context.packageName

        assertEquals("${LocalFileSystemPaths.EMULATED_ROOT_PATH}/$uid", context.getCurrentUserEmulatedPath())
        assertEquals("${LocalFileSystemPaths.USER_DATA}/$uid", context.getCurrentUserDataPath())
        assertEquals("${LocalFileSystemPaths.DATA_SUB_DATA}/$packageName", context.appDataDir())

        assertEquals(LocalFileSystemPrefix.Public, getLocalFileSystemPrefix(context, "/system/bin/sh"))
        assertEquals(LocalFileSystemPrefix.SdCard, getLocalFileSystemPrefix(context, "/sdcard/Download"))
        assertEquals(
            LocalFileSystemPrefix.AppData(context.appDataDir()),
            getLocalFileSystemPrefix(context, "${context.appDataDir()}/cache")
        )
        assertEquals(
            LocalFileSystemPrefix.SelfPackage(uid, packageName),
            getLocalFileSystemPrefix(context, "${LocalFileSystemPaths.USER_DATA_FRONT_PATH}$uid/$packageName/files")
        )
        assertEquals(
            LocalFileSystemPrefix.SelfEmulated(0),
            getLocalFileSystemPrefix(context, "${LocalFileSystemPaths.USER_EMULATED_FRONT_PATH}0/Download")
        )
        assertEquals(LocalFileSystemPrefix.Self, getLocalFileSystemPrefix(context, "/storage/self"))
        assertEquals(LocalFileSystemPrefix.SelfPrimary, getLocalFileSystemPrefix(context, "/storage/self/primary/DCIM"))
        assertEquals(LocalFileSystemPrefix.EmulatedRoot, getLocalFileSystemPrefix(context, "/storage/emulated"))
        assertEquals(LocalFileSystemPrefix.Storage, getLocalFileSystemPrefix(context, "/storage"))
        assertEquals(LocalFileSystemPrefix.Mounted("/storage/ABCD-1234"), getLocalFileSystemPrefix(context, "/storage/ABCD-1234/DCIM"))
        assertEquals(LocalFileSystemPrefix.Root, getLocalFileSystemPrefix(context, "/"))
        assertEquals(LocalFileSystemPrefix.Data, getLocalFileSystemPrefix(context, "/data"))
        assertEquals(LocalFileSystemPrefix.Data2, getLocalFileSystemPrefix(context, "/data/data"))
        assertEquals(LocalFileSystemPrefix.DataUser, getLocalFileSystemPrefix(context, "/data/user"))
        assertEquals(
            LocalFileSystemPrefix.SelfDataRoot(uid),
            getLocalFileSystemPrefix(context, "${LocalFileSystemPaths.USER_DATA_FRONT_PATH}$uid")
        )
        assertEquals(LocalFileSystemPrefix.InstalledApps, getLocalFileSystemPrefix(context, "/data/app/com.example"))

        assertThrows(Exception::class.java) {
            getLocalFileSystemPrefix(context, "/tmp/not-an-android-path")
        }
    }

    @Test
    fun testGetDirectorySize() = runBlocking {
        val nested = DirectoryInstance(
            "/root/nested",
            files = listOf(fileInfo("deep.bin", 7))
        )
        val root = DirectoryInstance(
            "/root",
            files = listOf(fileInfo("one.txt", 10), fileInfo("two.txt", 15)),
            directories = listOf(directoryInfo("nested")),
            children = mapOf("nested" to nested)
        )

        assertEquals(32, root.getDirectorySize())
    }

    private fun fileInfo(name: String, size: Long): FileInfo {
        return FileInfo(
            name = name,
            uri = Uri.Builder().path("/$name").build(),
            time = FileTime(),
            kind = FileKind.File(null, false, size, name.substringAfterLast('.')),
            permissions = FilePermissions.USER_READABLE
        )
    }

    private fun directoryInfo(name: String): FileInfo {
        return FileInfo(
            name = name,
            uri = Uri.Builder().path("/$name").build(),
            time = FileTime(),
            kind = FileKind.Directory(null, false),
            permissions = FilePermissions.USER_READABLE
        )
    }

    private class DirectoryInstance(
        path: String,
        private val files: List<FileInfo> = emptyList(),
        private val directories: List<FileInfo> = emptyList(),
        private val children: Map<String, FileInstance> = emptyMap(),
    ) : FileInstance(Uri.Builder().path(path).build()) {
        override suspend fun filePermissions() = FilePermissions.USER_READABLE

        override suspend fun fileTime() = FileTime()

        override suspend fun fileKind() = FileKind.Directory(null, false)

        override suspend fun getFileInputStream(): FileInputStream {
            error("Directory does not expose a file input stream")
        }

        override suspend fun getFileOutputStream(): FileOutputStream {
            error("Directory does not expose a file output stream")
        }

        override suspend fun listInternal(fileSystemPack: FileSystemPack) {
            fileSystemPack.addFiles(files)
            fileSystemPack.addDirectories(directories)
        }

        override suspend fun exists() = true

        override suspend fun createFile() = false

        override suspend fun createDirectory() = false

        override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance? {
            return children[name]
        }

        override suspend fun toParent() = this

        override suspend fun deleteFileOrEmptyDirectory() = false

        override suspend fun rename(newName: String): FileInstance? = null
    }
}
