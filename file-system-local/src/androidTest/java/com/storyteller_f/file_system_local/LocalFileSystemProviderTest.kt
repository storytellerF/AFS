package com.storyteller_f.file_system_local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system.AFSProvider
import com.storyteller_f.file_system.installedProviders
import com.storyteller_f.file_system.newFileSystem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.spi.FileSystemProvider
import java.util.ServiceLoader
import kotlin.io.path.readText

@RunWith(AndroidJUnit4::class)
class LocalFileSystemProviderTest {

    @Test
    fun test() {
        val installedProviders = installedProviders()
        assertEquals(2, installedProviders.size)
    }

    @Test
    fun testPath() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val load = ServiceLoader.load(FileSystemProvider::class.java)
        load.forEach {
            println(it.scheme)
        }

        runBlocking {
            val testFile = File(appContext.filesDir, "hello.txt")
            testFile.writeText("world")
            val fileSystem = newFileSystem(
                testFile.toURI(),
                mutableMapOf(AFSProvider.CONTEXT_KEY to appContext)
            )
            val path = fileSystem!!.getPath(testFile.absolutePath)
            val text = path.readText()
            assertEquals("world", text)
        }
    }
}