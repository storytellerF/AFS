package com.storyteller_f.file_system_remote.instance

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HttpFileInstanceTest {
    @Test
    fun unsupportedOperationsThrowOrReturnFalseWithoutNetwork() {
        val instance = HttpFileInstance(Uri.parse("http://example.test/file.txt"))

        assertFalse(runBlocking { instance.createFile() })
        assertFalse(runBlocking { instance.createDirectory() })
        assertFalse(runBlocking { instance.deleteFileOrEmptyDirectory() })
        assertThrows(NotImplementedError::class.java) {
            runBlocking { instance.fileTime() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { instance.getFileOutputStream() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { instance.list() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { instance.rename("renamed.txt") }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { instance.toParent() }
        }
        assertThrows(NotImplementedError::class.java) {
            runBlocking { instance.toChild("child", FileCreatePolicy.NotCreate) }
        }
    }
}
