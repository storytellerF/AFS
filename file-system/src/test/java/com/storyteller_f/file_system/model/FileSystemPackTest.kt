package com.storyteller_f.file_system.model

import android.net.Uri
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileSystemPackTest {
    @Test
    fun addFilesAndDirectoriesUpdatesContainersAndCount() {
        val firstFile = info("one.txt", FileKind.File(null, false, 10, "txt"))
        val secondFile = info("two.bin", FileKind.File(null, false, 20, "bin"))
        val directory = info("docs", FileKind.Directory(null, false))
        val pack = FileSystemPack(mutableListOf(), mutableListOf())

        assertTrue(pack.addFile(firstFile))
        pack.addFiles(listOf(secondFile))
        assertTrue(pack.addDirectory(directory))
        pack.addDirectories(emptyList())

        assertEquals(listOf(firstFile, secondFile), pack.files)
        assertEquals(listOf(directory), pack.directories)
        assertEquals(3, pack.count)
        assertEquals(0, FileSystemPack.EMPTY.count)
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
}
