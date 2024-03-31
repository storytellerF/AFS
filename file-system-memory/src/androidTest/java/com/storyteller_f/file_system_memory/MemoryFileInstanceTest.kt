package com.storyteller_f.file_system_memory

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.io.path.createFile
import kotlin.io.path.writeText

@RunWith(AndroidJUnit4::class)
class MemoryFileInstanceTest {
    @Test
    fun test() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val fileSystem = Jimfs.newFileSystem(Configuration.unix())
        fileSystem.getPath("/hello.txt").apply {
            createFile()
            writeText("hello")
        }
        MemoryFileInstance.memoryFileSystems["test"] = fileSystem
        val uri =
            Uri.Builder().scheme(MemoryFileInstance.SCHEME).authority("test").path("/").build()
        runBlocking {
            val fileInstance = getFileInstance(appContext, uri)!!
            assertEquals(1, fileInstance.list().files.count())
            val child = fileInstance.toChild("hello.txt", FileCreatePolicy.NotCreate)!!
            assertEquals("hello", child.getInputStream().bufferedReader().readText())
        }
    }
}