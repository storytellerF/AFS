package com.storyteller_f.file_system_local

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system.getFileSystemPrefix
import com.storyteller_f.file_system_local.LocalFileSystemPaths.USER_EMULATED_FRONT_PATH
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileInstanceFactoryKtTest {
    @Test
    fun testPrefix() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            listOf(
                LocalFileSystemPaths.CURRENT_EMULATED_PATH to LocalFileSystemPaths.CURRENT_EMULATED_PATH,
                "/storage/self/primary" to "/storage/self/primary",
                LocalFileSystemPaths.ROOT_USER_EMULATED_PATH to LocalFileSystemPaths.ROOT_USER_EMULATED_PATH,
                "${LocalFileSystemPaths.ROOT_USER_EMULATED_PATH}/Downloads" to LocalFileSystemPaths.ROOT_USER_EMULATED_PATH,
                "/storage/XX44-XX55/Downloads" to "/storage/XX44-XX55",
                "/storage/XX44-XX55" to "/storage/XX44-XX55",
                LocalFileSystemPaths.STORAGE_PATH to "/storage"
            ).forEach { (path, expected) ->
                val prefix =
                    getFileSystemPrefix(appContext, File(path).toUri()) as LocalFileSystemPrefix
                assertEquals(expected, prefix.key)
            }
        }

    }

    @Test
    fun testId() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            val prefix = getLocalFileSystemPrefix(
                appContext,
                "${USER_EMULATED_FRONT_PATH}10"
            ) as LocalFileSystemPrefix.SelfEmulated
            assertEquals(10L, prefix.uid)
        }
    }

    @Test
    fun testList() {
        runBlocking {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            listOf(
                "/storage/self" to arrayOf("primary"),
                "/storage/self/primary" to
                        File("/storage/self/primary").listFiles().orEmpty(),
            ).forEach { (it, expected) ->
                val fileInstance = getFileInstance(
                    appContext,
                    File(it).toUri(),
                )!!
                assertEquals(expected.size, fileInstance.list().count)
            }
        }

    }

}